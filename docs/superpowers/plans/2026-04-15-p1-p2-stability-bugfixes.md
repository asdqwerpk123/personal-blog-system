# P1/P2 Stability Bugfixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate the reviewed stability bugs without expanding scope: make article-tag replacement atomic, translate tag/article unique-key collisions back to the existing 400 contract, and make status normalization locale-safe.

**Architecture:** Keep the current controller/service layering and existing `Result` + `GlobalExceptionHandler` flow. Fix the bugs in place with small targeted changes, then add focused integration tests that prove the failure modes are now handled correctly.

**Tech Stack:** Java 17, Spring Boot 4.0.3, MyBatis-Plus, MySQL 8, JUnit 5, Spring Boot Test, Mockito `@SpyBean`, Maven Wrapper

---

## File Map

- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogArticleTagServiceImpl.java`
  - Add transaction protection to the full-replacement tag update path.
- Create: `personal-blog-admin/src/test/java/org/example/personalblogsystem/service/BlogArticleTagServiceTransactionTest.java`
  - Reproduce a mid-flight failure during tag replacement and verify rollback.
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogTagServiceImpl.java`
  - Catch duplicate-key write failures and translate them to `IllegalArgumentException("tagName already exists")`.
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogArticleServiceImpl.java`
  - Catch duplicate-key write failures and translate them to `IllegalArgumentException("articleSlug already exists")`.
- Modify: `personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/BlogTagControllerTest.java`
  - Add `@SpyBean BlogTagMapper` and duplicate-key regression tests for create/update.
- Modify: `personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/BlogArticleControllerTest.java`
  - Add `@SpyBean BlogArticleMapper` and duplicate-key regression tests for create/update.
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/controller/BlogArticleController.java`
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/controller/BlogCommentController.java`
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/controller/BlogFriendLinkController.java`
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogArticleServiceImpl.java`
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogCommentServiceImpl.java`
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogFriendLinkServiceImpl.java`
  - Replace locale-sensitive `toUpperCase()` calls with `toUpperCase(Locale.ROOT)`.
- Modify: `personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/BlogCommentControllerTest.java`
- Modify: `personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/BlogFriendLinkControllerTest.java`
  - Add Turkish-locale regression tests that send lowercase status values.

## Task 1: Make Article-Tag Replacement Atomic

**Files:**
- Create: `personal-blog-admin/src/test/java/org/example/personalblogsystem/service/BlogArticleTagServiceTransactionTest.java`
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogArticleTagServiceImpl.java`

- [ ] **Step 1: Write the failing rollback test**

Create `personal-blog-admin/src/test/java/org/example/personalblogsystem/service/BlogArticleTagServiceTransactionTest.java` with this exact test class:

```java
package org.example.personalblogsystem.service;

import org.example.personalblogsystem.PersonalBlogSystemApplication;
import org.example.personalblogsystem.mapper.BlogArticleTagMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(classes = PersonalBlogSystemApplication.class,
        properties = "spring.profiles.active=test")
class BlogArticleTagServiceTransactionTest {

    @Autowired
    private IBlogArticleTagService blogArticleTagService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @SpyBean
    private BlogArticleTagMapper blogArticleTagMapper;

    @BeforeEach
    void resetArticleOneTagState() {
        jdbcTemplate.update("delete from blog_article_tag where article_id = ? and tag_id in (?, ?)", 1L, 2L, 4L);
        jdbcTemplate.update("update blog_article_tag set deleted = 0 where article_id = ? and tag_id in (?, ?)", 1L, 1L, 3L);
        jdbcTemplate.update("update blog_article_tag set deleted = 1 where article_id = ? and tag_id in (?, ?)", 1L, 2L, 4L);
    }

    @AfterEach
    void cleanup() {
        Mockito.reset(blogArticleTagMapper);
        resetArticleOneTagState();
    }

    @Test
    void shouldRollbackWholeReplacementWhenDeletePhaseFails() {
        doThrow(new DuplicateKeyException("forced mid-flight failure"))
                .when(blogArticleTagMapper)
                .logicDeleteById(anyLong());

        assertThatThrownBy(() -> blogArticleTagService.replaceArticleTags(1L, List.of(2L, 4L)))
                .isInstanceOf(DuplicateKeyException.class);

        List<Long> activeTagIds = jdbcTemplate.queryForList(
                "select tag_id from blog_article_tag where article_id = ? and deleted = 0 order by tag_id",
                Long.class,
                1L);

        assertThat(activeTagIds).containsExactly(1L, 3L);
    }
}
```

- [ ] **Step 2: Run the new test to prove the current bug exists**

Run:

```powershell
./mvnw.cmd -pl personal-blog-admin -Dtest=BlogArticleTagServiceTransactionTest test
```

Expected before the fix: FAIL because tag ids `2` and `4` remain active after the forced exception, proving `replaceArticleTags` partially committed work.

- [ ] **Step 3: Add transaction protection to the replacement method**

Update `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogArticleTagServiceImpl.java`:

```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
```

```java
@Override
@Transactional
public List<BlogTag> replaceArticleTags(Long articleId, List<Long> tagIds) {
    validateArticleExists(articleId);
    List<Long> normalizedTagIds = normalizeTagIds(tagIds);
    validateTagIdsExist(normalizedTagIds);

    List<BlogArticleTag> existingRelations = baseMapper.selectAllByArticleId(articleId);
    Map<Long, BlogArticleTag> relationByTagId = existingRelations.stream()
            .collect(Collectors.toMap(BlogArticleTag::getTagId, Function.identity(), (left, right) -> left));
    Set<Long> targetTagIds = new LinkedHashSet<>(normalizedTagIds);

    for (Long tagId : targetTagIds) {
        BlogArticleTag relation = relationByTagId.get(tagId);
        if (relation == null) {
            BlogArticleTag newRelation = new BlogArticleTag();
            LocalDateTime now = LocalDateTime.now();
            newRelation.setArticleId(articleId);
            newRelation.setTagId(tagId);
            newRelation.setCreateTime(now);
            newRelation.setUpdateTime(now);
            newRelation.setDeleted(false);
            save(newRelation);
            continue;
        }

        if (Boolean.TRUE.equals(relation.getDeleted())) {
            baseMapper.restoreById(relation.getId());
        }
    }

    for (BlogArticleTag relation : existingRelations) {
        if (!targetTagIds.contains(relation.getTagId()) && !Boolean.TRUE.equals(relation.getDeleted())) {
            baseMapper.logicDeleteById(relation.getId());
        }
    }

    return listTagsByArticleId(articleId);
}
```

- [ ] **Step 4: Re-run the rollback test**

Run:

```powershell
./mvnw.cmd -pl personal-blog-admin -Dtest=BlogArticleTagServiceTransactionTest test
```

Expected after the fix: PASS, with the active tag ids for article `1` still exactly `[1, 3]`.

- [ ] **Step 5: Commit the atomicity fix**

```powershell
git add personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogArticleTagServiceImpl.java personal-blog-admin/src/test/java/org/example/personalblogsystem/service/BlogArticleTagServiceTransactionTest.java
git commit -m "fix: make article tag replacement atomic"
```

## Task 2: Translate Article/Tag Unique-Key Races Back to 400

**Files:**
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogTagServiceImpl.java`
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogArticleServiceImpl.java`
- Modify: `personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/BlogTagControllerTest.java`
- Modify: `personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/BlogArticleControllerTest.java`

- [ ] **Step 1: Add failing duplicate-key regression tests for tag writes**

In `personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/BlogTagControllerTest.java`, add these imports and fields:

```java
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DuplicateKeyException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
```

```java
@SpyBean
private BlogTagMapper blogTagMapperSpy;

@org.junit.jupiter.api.AfterEach
void tearDownSpy() {
    reset(blogTagMapperSpy);
}
```

Add these tests:

```java
@Test
void shouldTranslateDuplicateKeyRaceOnTagCreate() throws Exception {
    BlogTag tag = new BlogTag();
    tag.setTagName("Temp-" + UUID.randomUUID().toString().substring(0, 8));
    tag.setDescription("temp");
    tag.setCreatedBy(2L);

    doThrow(new DuplicateKeyException("Duplicate entry 'SpringBoot' for key 'uk_blog_tag_tag_name'"))
            .when(blogTagMapperSpy)
            .insert(any(BlogTag.class));

    mockMvc.perform(post("/admin/tag")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(tag)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("tagName already exists"));
}

@Test
void shouldTranslateDuplicateKeyRaceOnTagUpdate() throws Exception {
    BlogTag updateRequest = new BlogTag();
    updateRequest.setTagName("Race-" + UUID.randomUUID().toString().substring(0, 8));
    updateRequest.setDescription("updated");

    doThrow(new DuplicateKeyException("Duplicate entry 'Vue' for key 'uk_blog_tag_tag_name'"))
            .when(blogTagMapperSpy)
            .updateById(any(BlogTag.class));

    mockMvc.perform(put("/admin/tag/{id}", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("tagName already exists"));
}
```

- [ ] **Step 2: Add failing duplicate-key regression tests for article writes**

In `personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/BlogArticleControllerTest.java`, add these imports and fields:

```java
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DuplicateKeyException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
```

```java
@SpyBean
private BlogArticleMapper blogArticleMapperSpy;

@org.junit.jupiter.api.AfterEach
void tearDownSpy() {
    reset(blogArticleMapperSpy);
}
```

Add these tests:

```java
@Test
void shouldTranslateDuplicateKeyRaceOnArticleCreate() throws Exception {
    BlogArticle article = buildArticle("Race create", randomSlug(), "content", 5L);
    article.setCategoryId(1L);

    doThrow(new DuplicateKeyException("Duplicate entry 'build-personal-blog-with-spring-boot' for key 'uk_blog_article_article_slug'"))
            .when(blogArticleMapperSpy)
            .insert(any(BlogArticle.class));

    mockMvc.perform(post("/admin/article")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(article)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("articleSlug already exists"));
}

@Test
void shouldTranslateDuplicateKeyRaceOnArticleUpdate() throws Exception {
    BlogArticle updateRequest = buildArticle("Race update", randomSlug(), "updated content", 5L);
    updateRequest.setCategoryId(1L);

    doThrow(new DuplicateKeyException("Duplicate entry 'build-personal-blog-with-spring-boot' for key 'uk_blog_article_article_slug'"))
            .when(blogArticleMapperSpy)
            .updateById(any(BlogArticle.class));

    mockMvc.perform(put("/admin/article/{id}", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("articleSlug already exists"));
}
```

- [ ] **Step 3: Run the duplicate-key tests and capture the current failure mode**

Run:

```powershell
./mvnw.cmd -pl personal-blog-admin "-Dtest=BlogTagControllerTest,BlogArticleControllerTest" test
```

Expected before the fix: FAIL because the forced mapper exceptions bubble into the generic 500 handler instead of returning code `400`.

- [ ] **Step 4: Harden `BlogTagServiceImpl` against duplicate-key write failures**

Update `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogTagServiceImpl.java` imports:

```java
import org.springframework.dao.DataAccessException;
```

Wrap create/update writes and add a translator:

```java
@Override
public BlogTag createTag(BlogTag tag) {
    validateTagNameUnique(tag.getTagName(), null);
    validateCreatedByExists(tag.getCreatedBy());

    LocalDateTime now = LocalDateTime.now();
    tag.setId(null);
    tag.setCreateTime(now);
    tag.setUpdateTime(now);
    tag.setDeleted(false);
    try {
        save(tag);
    } catch (DataAccessException exception) {
        throw translateDuplicateTagNameException(exception);
    }
    return getById(tag.getId());
}

@Override
public BlogTag updateTag(Long id, BlogTag tag) {
    BlogTag existing = getById(id);
    if (existing == null) {
        return null;
    }

    validateTagNameUnique(tag.getTagName(), id);
    existing.setTagName(tag.getTagName());
    existing.setDescription(tag.getDescription());
    existing.setUpdateTime(LocalDateTime.now());
    try {
        return updateById(existing) ? getById(id) : null;
    } catch (DataAccessException exception) {
        throw translateDuplicateTagNameException(exception);
    }
}

private IllegalArgumentException translateDuplicateTagNameException(DataAccessException exception) {
    String message = exception.getMostSpecificCause() == null ? null : exception.getMostSpecificCause().getMessage();
    if (message != null && message.toLowerCase().contains("duplicate")) {
        return new IllegalArgumentException("tagName already exists");
    }
    throw exception;
}
```

- [ ] **Step 5: Harden `BlogArticleServiceImpl` against duplicate-key write failures**

Update `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogArticleServiceImpl.java` with the same pattern:

```java
@Override
public BlogArticle createArticle(BlogArticle article) {
    validateArticleReferences(article, null);
    LocalDateTime now = LocalDateTime.now();
    article.setId(null);
    article.setArticleStatus(normalizeStatus(article.getArticleStatus(), "DRAFT"));
    article.setTopFlag(article.getTopFlag() != null && article.getTopFlag());
    article.setAllowComment(article.getAllowComment() == null || article.getAllowComment());
    article.setViewCount(article.getViewCount() == null ? 0 : article.getViewCount());
    article.setLikeCount(article.getLikeCount() == null ? 0 : article.getLikeCount());
    article.setCreateTime(now);
    article.setUpdateTime(now);
    article.setDeleted(false);
    try {
        save(article);
    } catch (DataAccessException exception) {
        throw translateDuplicateArticleSlugException(exception);
    }
    return getById(article.getId());
}

@Override
public BlogArticle updateArticle(Long id, BlogArticle article) {
    BlogArticle existing = getById(id);
    if (existing == null) {
        return null;
    }

    validateArticleReferences(article, id);
    existing.setArticleTitle(article.getArticleTitle());
    existing.setArticleSlug(article.getArticleSlug());
    existing.setArticleSummary(article.getArticleSummary());
    existing.setCoverUrl(article.getCoverUrl());
    existing.setArticleContent(article.getArticleContent());
    existing.setAuthorId(article.getAuthorId());
    existing.setCategoryId(article.getCategoryId());
    existing.setTopFlag(article.getTopFlag() == null ? existing.getTopFlag() : article.getTopFlag());
    existing.setAllowComment(article.getAllowComment() == null ? existing.getAllowComment() : article.getAllowComment());
    existing.setUpdateTime(LocalDateTime.now());
    try {
        return updateById(existing) ? getById(id) : null;
    } catch (DataAccessException exception) {
        throw translateDuplicateArticleSlugException(exception);
    }
}

private IllegalArgumentException translateDuplicateArticleSlugException(DataAccessException exception) {
    String message = exception.getMostSpecificCause() == null ? null : exception.getMostSpecificCause().getMessage();
    if (message != null && message.toLowerCase().contains("duplicate")) {
        return new IllegalArgumentException("articleSlug already exists");
    }
    throw exception;
}
```

- [ ] **Step 6: Re-run the duplicate-key tests**

Run:

```powershell
./mvnw.cmd -pl personal-blog-admin "-Dtest=BlogTagControllerTest,BlogArticleControllerTest" test
```

Expected after the fix: PASS, with the forced duplicate-key path now returning `400` and the existing field-specific messages.

- [ ] **Step 7: Commit the duplicate-key hardening**

```powershell
git add personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogTagServiceImpl.java personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogArticleServiceImpl.java personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/BlogTagControllerTest.java personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/BlogArticleControllerTest.java
git commit -m "fix: map duplicate article and tag writes to param errors"
```

## Task 3: Make Status Normalization Locale-Safe

**Files:**
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/controller/BlogArticleController.java`
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/controller/BlogCommentController.java`
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/controller/BlogFriendLinkController.java`
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogArticleServiceImpl.java`
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogCommentServiceImpl.java`
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogFriendLinkServiceImpl.java`
- Modify: `personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/BlogArticleControllerTest.java`
- Modify: `personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/BlogCommentControllerTest.java`
- Modify: `personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/BlogFriendLinkControllerTest.java`

- [ ] **Step 1: Add failing Turkish-locale regression tests**

In `personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/BlogArticleControllerTest.java`, add:

```java
import java.util.Locale;
```

```java
@Test
void shouldAcceptLowercaseArticleStatusUnderTurkishLocale() throws Exception {
    Locale original = Locale.getDefault();
    Locale.setDefault(Locale.forLanguageTag("tr-TR"));
    try {
        mockMvc.perform(put("/admin/article/{id}/status", 1L)
                        .param("status", "private"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.articleStatus").value("PRIVATE"));
    } finally {
        Locale.setDefault(original);
    }
}
```

In `personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/BlogCommentControllerTest.java`, add:

```java
import java.util.Locale;
```

```java
@Test
void shouldAcceptLowercaseCommentStatusUnderTurkishLocale() throws Exception {
    Locale original = Locale.getDefault();
    Locale.setDefault(Locale.forLanguageTag("tr-TR"));
    try {
        mockMvc.perform(put("/admin/comment/{id}/status", 1L)
                        .param("status", "approved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.commentStatus").value("APPROVED"));
    } finally {
        Locale.setDefault(original);
    }
}
```

In `personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/BlogFriendLinkControllerTest.java`, add:

```java
import java.util.Locale;
```

```java
@Test
void shouldAcceptLowercaseFriendLinkStatusUnderTurkishLocale() throws Exception {
    Locale original = Locale.getDefault();
    Locale.setDefault(Locale.forLanguageTag("tr-TR"));
    try {
        mockMvc.perform(post("/admin/friend-link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(friendLinkRequest(
                                "Locale Link",
                                randomUrl(),
                                null,
                                null,
                                "approved",
                                2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.linkStatus").value("APPROVED"));
    } finally {
        Locale.setDefault(original);
    }
}
```

- [ ] **Step 2: Run the locale regression tests to prove the bug**

Run:

```powershell
./mvnw.cmd -pl personal-blog-admin "-Dtest=BlogArticleControllerTest,BlogCommentControllerTest,BlogFriendLinkControllerTest" test
```

Expected before the fix: FAIL under Turkish locale because `toUpperCase()` produces locale-dependent values and the validations reject the lowercase inputs.

- [ ] **Step 3: Replace default-locale uppercase calls with `Locale.ROOT`**

Add this import everywhere you change uppercase normalization:

```java
import java.util.Locale;
```

Make these exact replacements:

`personal-blog-admin/src/main/java/org/example/personalblogsystem/controller/BlogArticleController.java`

```java
String normalized = status.trim().toUpperCase(Locale.ROOT);
```

`personal-blog-admin/src/main/java/org/example/personalblogsystem/controller/BlogCommentController.java`

```java
String normalized = status.trim().toUpperCase(Locale.ROOT);
```

`personal-blog-admin/src/main/java/org/example/personalblogsystem/controller/BlogFriendLinkController.java`

```java
String normalized = status.trim().toUpperCase(Locale.ROOT);
```

`personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogArticleServiceImpl.java`

```java
String normalized = value.toUpperCase(Locale.ROOT);
```

`personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogCommentServiceImpl.java`

```java
String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
```

`personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogFriendLinkServiceImpl.java`

```java
String normalized = value.toUpperCase(Locale.ROOT);
```

- [ ] **Step 4: Re-run the locale regression tests**

Run:

```powershell
./mvnw.cmd -pl personal-blog-admin "-Dtest=BlogArticleControllerTest,BlogCommentControllerTest,BlogFriendLinkControllerTest" test
```

Expected after the fix: PASS, with lowercase statuses still accepted when the JVM default locale is Turkish.

- [ ] **Step 5: Commit the locale-safe normalization fix**

```powershell
git add personal-blog-admin/src/main/java/org/example/personalblogsystem/controller/BlogArticleController.java personal-blog-admin/src/main/java/org/example/personalblogsystem/controller/BlogCommentController.java personal-blog-admin/src/main/java/org/example/personalblogsystem/controller/BlogFriendLinkController.java personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogArticleServiceImpl.java personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogCommentServiceImpl.java personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogFriendLinkServiceImpl.java personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/BlogArticleControllerTest.java personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/BlogCommentControllerTest.java personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/BlogFriendLinkControllerTest.java
git commit -m "fix: normalize status values with locale root"
```

## Task 4: Run Full Regression and Sanity Check

**Files:**
- Modify: none expected
- Test: `personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/BlogArticleControllerTest.java`
- Test: `personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/BlogTagControllerTest.java`
- Test: `personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/BlogCommentControllerTest.java`
- Test: `personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/BlogFriendLinkControllerTest.java`
- Test: `personal-blog-admin/src/test/java/org/example/personalblogsystem/service/BlogArticleTagServiceTransactionTest.java`

- [ ] **Step 1: Run the focused bugfix suite**

Run:

```powershell
./mvnw.cmd -pl personal-blog-admin "-Dtest=BlogArticleTagServiceTransactionTest,BlogTagControllerTest,BlogArticleControllerTest,BlogCommentControllerTest,BlogFriendLinkControllerTest" test
```

Expected: PASS, and the new regression tests prove each reviewed bug is closed.

- [ ] **Step 2: Run the complete repository test suite**

Run:

```powershell
./mvnw.cmd test
```

Expected: PASS across all modules, with no regressions in P0/P1/P2 behavior.

- [ ] **Step 3: Check working tree scope before final handoff**

Run:

```powershell
git status --short
```

Expected: only the files listed in this plan (plus any generated test reports ignored by git).

- [ ] **Step 4: Commit the regression-only follow-up if needed**

If Task 4 produced no new file edits, skip this commit. If an execution worker had to adjust a flaky test or import during regression, use:

```powershell
git add <exact-files-changed-during-regression>
git commit -m "test: finalize stability bugfix regression coverage"
```

## Self-Review Checklist

- Spec coverage: atomic tag replacement, duplicate-key translation, and locale-safe status normalization all have explicit tasks and tests.
- Placeholder scan: no unfinished placeholder instructions remain.
- Type consistency: uses the current repo types and paths (`BlogArticleTagServiceImpl`, `BlogTagServiceImpl`, `BlogArticleServiceImpl`, existing controller test classes, and `Result` error flow).
