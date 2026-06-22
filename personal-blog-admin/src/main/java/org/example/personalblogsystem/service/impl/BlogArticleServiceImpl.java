package org.example.personalblogsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.personalblogcommon.exception.BlogException;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.example.personalblogsystem.auth.AdminAuthContext;
import org.example.personalblogsystem.auth.AdminAuthPrincipal;
import org.example.personalblogsystem.dto.PublicArticleDetailResponse;
import org.example.personalblogsystem.dto.PublicArticleResponse;
import org.example.personalblogsystem.dto.PublicTagResponse;
import org.example.personalblogsystem.dto.UserArticleRequest;
import org.example.personalblogsystem.dto.UserArticleResponse;
import org.example.personalblogsystem.dto.UserDashboardSummaryResponse;
import org.example.personalblogsystem.entity.BlogArticle;
import org.example.personalblogsystem.entity.BlogArticleTag;
import org.example.personalblogsystem.entity.BlogCategory;
import org.example.personalblogsystem.entity.BlogTag;
import org.example.personalblogsystem.entity.SysUser;
import org.example.personalblogsystem.mapper.BlogArticleMapper;
import org.example.personalblogsystem.mapper.BlogCategoryMapper;
import org.example.personalblogsystem.mapper.SysUserMapper;
import org.example.personalblogsystem.service.IBlogArticleService;
import org.example.personalblogsystem.service.IBlogArticleTagService;
import org.example.personalblogsystem.service.OperationLogRecordService;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * <p>
 * 文章服务实现类
 * </p>
 *
 * @author student
 * @since 2026-03-31
 */
@Service
public class BlogArticleServiceImpl extends ServiceImpl<BlogArticleMapper, BlogArticle> implements IBlogArticleService {

    private static final int FORBIDDEN_CODE = 403;
    private static final String FORBIDDEN_MESSAGE = "无权限操作";
    private static final String ARTICLE_STATUS_DRAFT = "DRAFT";
    private static final String ARTICLE_STATUS_PUBLISHED = "PUBLISHED";
    private static final String ARTICLE_STATUS_PRIVATE = "PRIVATE";
    private static final Set<String> USER_ARTICLE_STATUSES = Set.of(
            ARTICLE_STATUS_DRAFT,
            ARTICLE_STATUS_PUBLISHED,
            ARTICLE_STATUS_PRIVATE);

    private final JdbcTemplate jdbcTemplate;
    private final SysUserMapper sysUserMapper;
    private final BlogCategoryMapper blogCategoryMapper;
    private final IBlogArticleTagService blogArticleTagService;
    private final OperationLogRecordService operationLogRecordService;

    public BlogArticleServiceImpl(JdbcTemplate jdbcTemplate,
                                  SysUserMapper sysUserMapper,
                                  BlogCategoryMapper blogCategoryMapper,
                                  IBlogArticleTagService blogArticleTagService,
                                  OperationLogRecordService operationLogRecordService) {
        this.jdbcTemplate = jdbcTemplate;
        this.sysUserMapper = sysUserMapper;
        this.blogCategoryMapper = blogCategoryMapper;
        this.blogArticleTagService = blogArticleTagService;
        this.operationLogRecordService = operationLogRecordService;
    }

    @Override
    public Page<BlogArticle> pageArticles(long current, long size, String keyword) {
        LambdaQueryWrapper<BlogArticle> queryWrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            queryWrapper.and(wrapper -> wrapper.like(BlogArticle::getArticleTitle, keyword)
                    .or()
                    .like(BlogArticle::getArticleSummary, keyword));
        }
        queryWrapper.orderByDesc(BlogArticle::getPublishedTime, BlogArticle::getUpdateTime, BlogArticle::getId);
        return page(new Page<>(current, size), queryWrapper);
    }

    @Override
    public Page<PublicArticleResponse> pagePublicArticles(long current,
                                                          long size,
                                                          String keyword,
                                                          Long categoryId,
                                                          Long tagId) {
        LambdaQueryWrapper<BlogArticle> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BlogArticle::getArticleStatus, ARTICLE_STATUS_PUBLISHED);
        if (StringUtils.hasText(keyword)) {
            String trimmedKeyword = keyword.trim();
            queryWrapper.and(wrapper -> wrapper.like(BlogArticle::getArticleTitle, trimmedKeyword)
                    .or()
                    .like(BlogArticle::getArticleSummary, trimmedKeyword));
        }
        if (categoryId != null) {
            queryWrapper.eq(BlogArticle::getCategoryId, categoryId);
        }
        if (tagId != null) {
            List<Long> articleIds = listPublicArticleIdsByTagId(tagId);
            if (articleIds.isEmpty()) {
                queryWrapper.eq(BlogArticle::getId, -1L);
            } else {
                queryWrapper.in(BlogArticle::getId, articleIds);
            }
        }
        queryWrapper.orderByDesc(BlogArticle::getTopFlag, BlogArticle::getPublishedTime, BlogArticle::getId);

        Page<BlogArticle> articlePage = page(new Page<>(current, size), queryWrapper);
        Page<PublicArticleResponse> responsePage = new Page<>(
                articlePage.getCurrent(),
                articlePage.getSize(),
                articlePage.getTotal());
        responsePage.setRecords(articlePage.getRecords().stream()
                .map(this::toPublicArticleResponse)
                .toList());
        return responsePage;
    }

    @Override
    public PublicArticleDetailResponse getPublicArticle(Long id) {
        BlogArticle article = lambdaQuery()
                .eq(BlogArticle::getId, id)
                .eq(BlogArticle::getArticleStatus, ARTICLE_STATUS_PUBLISHED)
                .one();
        if (article == null) {
            throw new BlogException(ResultCodeEnum.NOT_FOUND);
        }
        return toPublicArticleDetailResponse(article);
    }

    @Override
    public BlogArticle createArticle(BlogArticle article) {
        AdminAuthPrincipal currentUser = AdminAuthContext.requireCurrentUser();
        article.setAuthorId(currentUser.getUserId());
        validateArticleReferences(article, null);
        LocalDateTime now = LocalDateTime.now();
        article.setId(null);
        article.setArticleStatus(normalizeStatus(article.getArticleStatus(), ARTICLE_STATUS_DRAFT));
        article.setTopFlag(article.getTopFlag() != null && article.getTopFlag());
        article.setAllowComment(article.getAllowComment() == null || article.getAllowComment());
        article.setPublishedTime(null);
        article.setPublishTime(article.getPublishTime());
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCreateTime(now);
        article.setUpdateTime(now);
        article.setDeleted(false);
        try {
            save(article);
        } catch (DataAccessException exception) {
            throw translateDuplicateArticleSlugException(exception);
        }
        operationLogRecordService.recordSuccess("ARTICLE", article.getId(), "CREATE_ARTICLE", "新增文章：" + article.getArticleTitle());
        return getById(article.getId());
    }

    @Override
    public BlogArticle updateArticle(Long id, BlogArticle article) {
        BlogArticle existing = getById(id);
        if (existing == null) {
            return null;
        }

        Long preservedAuthorId = existing.getAuthorId();
        LocalDateTime preservedPublishedTime = existing.getPublishedTime();
        LocalDateTime preservedPublishTime = existing.getPublishTime();
        article.setAuthorId(preservedAuthorId);
        validateArticleReferences(article, id);
        existing.setArticleTitle(article.getArticleTitle());
        existing.setArticleSlug(article.getArticleSlug());
        existing.setArticleSummary(article.getArticleSummary());
        existing.setCoverUrl(article.getCoverUrl());
        existing.setArticleContent(article.getArticleContent());
        existing.setCategoryId(article.getCategoryId());
        if (article.getArticleStatus() != null) {
            existing.setArticleStatus(normalizeStatus(article.getArticleStatus(), null));
        }
        existing.setTopFlag(article.getTopFlag() == null ? existing.getTopFlag() : article.getTopFlag());
        existing.setAllowComment(article.getAllowComment() == null ? existing.getAllowComment() : article.getAllowComment());
        existing.setAuthorId(preservedAuthorId);
        existing.setPublishedTime(preservedPublishedTime);
        existing.setPublishTime(article.getPublishTime() == null ? preservedPublishTime : article.getPublishTime());
        existing.setUpdateTime(LocalDateTime.now());
        try {
            if (!updateById(existing)) {
                return null;
            }
            operationLogRecordService.recordSuccess("ARTICLE", id, "UPDATE_ARTICLE", "编辑文章：" + existing.getArticleTitle());
            return getById(id);
        } catch (DataAccessException exception) {
            throw translateDuplicateArticleSlugException(exception);
        }
    }

    @Override
    public BlogArticle updateArticleStatus(Long id, String status) {
        BlogArticle existing = getById(id);
        if (existing == null) {
            return null;
        }

        existing.setArticleStatus(normalizeStatus(status, null));
        existing.setUpdateTime(LocalDateTime.now());
        if (!updateById(existing)) {
            return null;
        }
        operationLogRecordService.recordSuccess("ARTICLE", id, "CHANGE_ARTICLE_STATUS", "修改文章状态：" + existing.getArticleTitle() + " -> " + existing.getArticleStatus());
        return getById(id);
    }

    @Override
    @Transactional
    public int publishScheduledArticles() {
        return baseMapper.publishScheduledArticles(
                LocalDateTime.now(),
                ARTICLE_STATUS_DRAFT,
                ARTICLE_STATUS_PUBLISHED);
    }

    @Override
    @Transactional
    public int publishArticleById(Long id) {
        BlogArticle existing = getById(id);
        if (existing == null || !ARTICLE_STATUS_DRAFT.equals(existing.getArticleStatus())) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        existing.setArticleStatus(ARTICLE_STATUS_PUBLISHED);
        if (existing.getPublishedTime() == null) {
            existing.setPublishedTime(now);
        }
        existing.setUpdateTime(now);
        return updateById(existing) ? 1 : 0;
    }

    @Override
    public boolean deleteArticle(Long id) {
        BlogArticle existing = getById(id);
        if (existing == null) {
            return false;
        }

        try {
            AdminAuthPrincipal currentUser = AdminAuthContext.requireCurrentUser();
            Long operatorUserId = currentUser.getUserId();
            jdbcTemplate.update("CALL sp_delete_article(?, ?)", operatorUserId, id);
            boolean replacedProcedureLog = operationLogRecordService.replaceLatestSuccess(
                    operatorUserId,
                    "ARTICLE",
                    id,
                    Set.of("LOGIC_DELETE", "DELETE_ARTICLE"),
                    "DELETE_ARTICLE",
                    "删除文章：" + existing.getArticleTitle());
            if (!replacedProcedureLog) {
                operationLogRecordService.recordSuccess("ARTICLE", id, "DELETE_ARTICLE", "删除文章：" + existing.getArticleTitle());
            }
            return true;
        } catch (DataAccessException exception) {
            String message = getRootMessage(exception);
            if (message != null && message.contains("does not exist or has already been deleted")) {
                return false;
            }
            throw new IllegalArgumentException(message == null ? "delete article failed" : message);
        }
    }

    @Override
    public Page<UserArticleResponse> pageUserArticles(long current,
                                                      long size,
                                                      Long authorId,
                                                      String title,
                                                      Long categoryId,
                                                      String status) {
        LambdaQueryWrapper<BlogArticle> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BlogArticle::getAuthorId, authorId);
        if (StringUtils.hasText(title)) {
            queryWrapper.like(BlogArticle::getArticleTitle, title.trim());
        }
        if (categoryId != null) {
            queryWrapper.eq(BlogArticle::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(BlogArticle::getArticleStatus, normalizeUserStatus(status, null));
        }
        queryWrapper.orderByDesc(BlogArticle::getUpdateTime, BlogArticle::getId);

        Page<BlogArticle> articlePage = page(new Page<>(current, size), queryWrapper);
        Page<UserArticleResponse> responsePage = new Page<>(
                articlePage.getCurrent(),
                articlePage.getSize(),
                articlePage.getTotal());
        responsePage.setRecords(articlePage.getRecords().stream().map(this::toUserArticleResponse).toList());
        return responsePage;
    }

    @Override
    public UserArticleResponse getUserArticle(Long authorId, Long id) {
        return toUserArticleResponse(requireOwnedArticle(authorId, id));
    }

    @Override
    @Transactional
    public UserArticleResponse createUserArticle(Long authorId, UserArticleRequest request) {
        validateUserArticleRequest(request);

        BlogArticle article = new BlogArticle();
        LocalDateTime now = LocalDateTime.now();
        article.setArticleTitle(request.getArticleTitle().trim());
        article.setArticleSlug(resolveCreateSlug(request));
        article.setArticleSummary(trimToNull(request.getArticleSummary()));
        article.setCoverUrl(null);
        article.setArticleContent(request.getArticleContent().trim());
        article.setAuthorId(authorId);
        article.setCategoryId(request.getCategoryId());
        article.setArticleStatus(normalizeUserStatus(request.getArticleStatus(), ARTICLE_STATUS_DRAFT));
        article.setTopFlag(false);
        article.setAllowComment(true);
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setPublishedTime(null);
        article.setCreateTime(now);
        article.setUpdateTime(now);
        article.setDeleted(false);

        validateArticleReferences(article, null);
        try {
            save(article);
        } catch (DataAccessException exception) {
            throw translateDuplicateArticleSlugException(exception);
        }
        replaceTagsIfPresent(article.getId(), request.getTagIds());
        operationLogRecordService.recordSuccess(authorId, "ARTICLE", article.getId(), "CREATE_ARTICLE",
                "新建文章: " + article.getArticleTitle());
        return toUserArticleResponse(getById(article.getId()));
    }

    @Override
    @Transactional
    public UserArticleResponse updateUserArticle(Long authorId, Long id, UserArticleRequest request) {
        validateUserArticleRequest(request);
        BlogArticle existing = requireOwnedArticle(authorId, id);

        existing.setArticleTitle(request.getArticleTitle().trim());
        existing.setArticleSlug(resolveUpdateSlug(request, existing));
        existing.setArticleSummary(trimToNull(request.getArticleSummary()));
        existing.setCoverUrl(null);
        existing.setArticleContent(request.getArticleContent().trim());
        existing.setCategoryId(request.getCategoryId());
        existing.setArticleStatus(normalizeUserStatus(request.getArticleStatus(), existing.getArticleStatus()));
        existing.setUpdateTime(LocalDateTime.now());

        validateArticleReferences(existing, id);
        try {
            updateById(existing);
        } catch (DataAccessException exception) {
            throw translateDuplicateArticleSlugException(exception);
        }
        replaceTagsIfPresent(existing.getId(), request.getTagIds());
        operationLogRecordService.recordSuccess(authorId, "ARTICLE", id, "UPDATE_ARTICLE",
                "编辑文章: " + existing.getArticleTitle());
        return toUserArticleResponse(getById(id));
    }

    @Override
    @Transactional
    public UserArticleResponse updateUserArticleStatus(Long authorId, Long id, String status) {
        BlogArticle existing = requireOwnedArticle(authorId, id);
        existing.setArticleStatus(normalizeUserStatus(status, null));
        existing.setUpdateTime(LocalDateTime.now());
        updateById(existing);
        operationLogRecordService.recordSuccess(authorId, "ARTICLE", id, "CHANGE_ARTICLE_STATUS",
                "修改文章状态: " + existing.getArticleTitle() + " -> " + existing.getArticleStatus());
        return toUserArticleResponse(getById(id));
    }

    @Override
    @Transactional
    public boolean deleteUserArticle(Long authorId, Long id) {
        BlogArticle existing = requireOwnedArticle(authorId, id);
        try {
            jdbcTemplate.update("CALL sp_delete_article(?, ?)", authorId, id);
            operationLogRecordService.replaceLatestSuccess(
                    authorId,
                    "ARTICLE",
                    id,
                    Set.of("LOGIC_DELETE", "DELETE_ARTICLE"),
                    "DELETE_ARTICLE",
                    "删除文章: " + existing.getArticleTitle());
            return true;
        } catch (DataAccessException exception) {
            String message = getRootMessage(exception);
            if (message != null && message.toLowerCase(Locale.ROOT).contains("own article")) {
                throw forbidden();
            }
            if (message != null && message.toLowerCase(Locale.ROOT).contains("does not exist")) {
                throw new BlogException(404, "文章不存在");
            }
            throw new IllegalArgumentException(message == null ? "删除文章失败" : message);
        }
    }

    @Override
    public UserDashboardSummaryResponse getUserDashboardSummary(Long authorId) {
        UserDashboardSummaryResponse response = new UserDashboardSummaryResponse();
        response.setArticleCount(countByAuthorAndStatus(authorId, null));
        response.setPublishedCount(countByAuthorAndStatus(authorId, ARTICLE_STATUS_PUBLISHED));
        response.setDraftCount(countByAuthorAndStatus(authorId, ARTICLE_STATUS_DRAFT));
        response.setPrivateCount(countByAuthorAndStatus(authorId, ARTICLE_STATUS_PRIVATE));
        response.setTotalViewCount(lambdaQuery()
                .eq(BlogArticle::getAuthorId, authorId)
                .list()
                .stream()
                .mapToLong(article -> article.getViewCount() == null ? 0 : article.getViewCount())
                .sum());

        LocalDateTime monthStart = YearMonth.now().atDay(1).atStartOfDay();
        response.setMonthUpdateCount(lambdaQuery()
                .eq(BlogArticle::getAuthorId, authorId)
                .ge(BlogArticle::getUpdateTime, monthStart)
                .count());

        List<Long> articleIds = lambdaQuery()
                .eq(BlogArticle::getAuthorId, authorId)
                .list()
                .stream()
                .map(BlogArticle::getId)
                .toList();
        if (articleIds.isEmpty()) {
            response.setPendingCommentCount(0);
        } else {
            Long pendingCount = jdbcTemplate.queryForObject(
                    "select count(*) from blog_comment where deleted = 0 and comment_status = 'PENDING' and article_id in ("
                            + articleIds.stream().map(id -> "?").reduce((left, right) -> left + "," + right).orElse("?")
                            + ")",
                    Long.class,
                    articleIds.toArray());
            response.setPendingCommentCount(pendingCount == null ? 0 : pendingCount);
        }

        response.setRecentArticles(lambdaQuery()
                .eq(BlogArticle::getAuthorId, authorId)
                .orderByDesc(BlogArticle::getUpdateTime, BlogArticle::getId)
                .last("limit 5")
                .list()
                .stream()
                .map(this::toUserArticleResponse)
                .toList());
        return response;
    }

    private String normalizeStatus(String status, String defaultStatus) {
        String value = status == null ? null : status.trim();
        if (value == null || value.isEmpty()) {
            return defaultStatus;
        }

        String normalized = value.toUpperCase(Locale.ROOT);
        if (!ARTICLE_STATUS_DRAFT.equals(normalized)
                && !ARTICLE_STATUS_PUBLISHED.equals(normalized)
                && !ARTICLE_STATUS_PRIVATE.equals(normalized)) {
            throw new IllegalArgumentException("status must be one of DRAFT, PUBLISHED, PRIVATE");
        }
        return normalized;
    }

    private String normalizeUserStatus(String status, String defaultStatus) {
        String value = status == null ? null : status.trim();
        if (value == null || value.isEmpty()) {
            if (defaultStatus == null) {
                throw new IllegalArgumentException("文章状态不能为空");
            }
            return defaultStatus;
        }

        String normalized = value.toUpperCase(Locale.ROOT);
        if ("OFFLINE".equals(normalized)) {
            return ARTICLE_STATUS_PRIVATE;
        }
        if (!USER_ARTICLE_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("文章状态不正确");
        }
        return normalized;
    }

    private BlogArticle requireOwnedArticle(Long authorId, Long id) {
        BlogArticle article = getById(id);
        if (article == null) {
            throw new BlogException(404, "文章不存在");
        }
        if (!authorId.equals(article.getAuthorId())) {
            throw forbidden();
        }
        return article;
    }

    private BlogException forbidden() {
        return new BlogException(FORBIDDEN_CODE, FORBIDDEN_MESSAGE);
    }

    private void validateUserArticleRequest(UserArticleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("文章内容不能为空");
        }
        if (!StringUtils.hasText(request.getArticleTitle())) {
            throw new IllegalArgumentException("文章标题不能为空");
        }
        if (!StringUtils.hasText(request.getArticleContent())) {
            throw new IllegalArgumentException("文章内容不能为空");
        }
    }

    private String resolveCreateSlug(UserArticleRequest request) {
        String slug = trimToNull(request.getArticleSlug());
        return slug == null ? "article-" + UUID.randomUUID().toString().substring(0, 12) : slug;
    }

    private String resolveUpdateSlug(UserArticleRequest request, BlogArticle existing) {
        String slug = trimToNull(request.getArticleSlug());
        return slug == null ? existing.getArticleSlug() : slug;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void replaceTagsIfPresent(Long articleId, List<Long> tagIds) {
        if (tagIds != null) {
            blogArticleTagService.replaceArticleTags(articleId, tagIds);
        }
    }

    private long countByAuthorAndStatus(Long authorId, String status) {
        return lambdaQuery()
                .eq(BlogArticle::getAuthorId, authorId)
                .eq(StringUtils.hasText(status), BlogArticle::getArticleStatus, status)
                .count();
    }

    private UserArticleResponse toUserArticleResponse(BlogArticle article) {
        UserArticleResponse response = new UserArticleResponse();
        response.setId(article.getId());
        response.setArticleTitle(article.getArticleTitle());
        response.setArticleSlug(article.getArticleSlug());
        response.setArticleSummary(article.getArticleSummary());
        response.setArticleContent(article.getArticleContent());
        response.setAuthorId(article.getAuthorId());
        response.setAuthorName(resolveUserDisplayName(article.getAuthorId()));
        response.setCategoryId(article.getCategoryId());
        response.setCategoryName(resolveCategoryName(article.getCategoryId()));
        response.setArticleStatus(article.getArticleStatus());
        response.setTopFlag(article.getTopFlag());
        response.setAllowComment(article.getAllowComment());
        response.setViewCount(article.getViewCount());
        response.setPublishedTime(article.getPublishedTime());
        response.setCreateTime(article.getCreateTime());
        response.setUpdateTime(article.getUpdateTime());
        List<BlogTag> tags = blogArticleTagService.listTagsByArticleId(article.getId());
        response.setTags(tags.stream().map(BlogTag::getTagName).toList());
        response.setTagIds(tags.stream().map(BlogTag::getId).toList());
        return response;
    }

    private PublicArticleResponse toPublicArticleResponse(BlogArticle article) {
        PublicArticleResponse response = new PublicArticleResponse();
        populatePublicArticleResponse(response, article);
        return response;
    }

    private PublicArticleDetailResponse toPublicArticleDetailResponse(BlogArticle article) {
        PublicArticleDetailResponse response = new PublicArticleDetailResponse();
        populatePublicArticleResponse(response, article);
        response.setArticleSlug(article.getArticleSlug());
        response.setArticleContent(article.getArticleContent());
        response.setAllowComment(article.getAllowComment());
        response.setCreateTime(article.getCreateTime());
        response.setUpdateTime(article.getUpdateTime());
        return response;
    }

    private void populatePublicArticleResponse(PublicArticleResponse response, BlogArticle article) {
        response.setId(article.getId());
        response.setArticleTitle(article.getArticleTitle());
        response.setArticleSummary(article.getArticleSummary());
        response.setCoverUrl(article.getCoverUrl());
        response.setAuthorId(article.getAuthorId());
        response.setAuthorName(resolveUserDisplayName(article.getAuthorId()));
        response.setAuthorAvatarUrl(resolveUserAvatarUrl(article.getAuthorId()));
        response.setCategoryId(article.getCategoryId());
        response.setCategoryName(resolveCategoryName(article.getCategoryId()));
        response.setTags(blogArticleTagService.listTagsByArticleId(article.getId()).stream()
                .map(this::toPublicTagResponse)
                .toList());
        response.setTopFlag(article.getTopFlag());
        response.setViewCount(article.getViewCount());
        response.setPublishedTime(article.getPublishedTime());
    }

    private PublicTagResponse toPublicTagResponse(BlogTag tag) {
        PublicTagResponse response = new PublicTagResponse();
        response.setId(tag.getId());
        response.setTagName(tag.getTagName());
        response.setDescription(tag.getDescription());
        return response;
    }

    private List<Long> listPublicArticleIdsByTagId(Long tagId) {
        return blogArticleTagService.list(new LambdaQueryWrapper<BlogArticleTag>()
                        .eq(BlogArticleTag::getTagId, tagId)
                        .eq(BlogArticleTag::getDeleted, false))
                .stream()
                .map(BlogArticleTag::getArticleId)
                .distinct()
                .toList();
    }

    private String resolveCategoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        BlogCategory category = blogCategoryMapper.selectById(categoryId);
        return category == null ? null : category.getCategoryName();
    }

    private String resolveUserDisplayName(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getNickName()) ? user.getNickName() : user.getUserName();
    }

    private String resolveUserAvatarUrl(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = sysUserMapper.selectById(userId);
        return user == null ? null : user.getAvatarUrl();
    }

    private void validateArticleReferences(BlogArticle article, Long currentArticleId) {
        validateArticleSlugUnique(article.getArticleSlug(), currentArticleId);
        validateAuthorExists(article.getAuthorId());
        validateCategoryExists(article.getCategoryId());
    }

    private void validateArticleSlugUnique(String articleSlug, Long currentArticleId) {
        LambdaQueryWrapper<BlogArticle> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BlogArticle::getArticleSlug, articleSlug);
        if (currentArticleId != null) {
            queryWrapper.ne(BlogArticle::getId, currentArticleId);
        }

        Long duplicateCount = count(queryWrapper);
        if (duplicateCount != null && duplicateCount > 0) {
            throw new IllegalArgumentException("articleSlug already exists");
        }
    }

    private void validateAuthorExists(Long authorId) {
        if (authorId == null || sysUserMapper.selectById(authorId) == null) {
            throw new IllegalArgumentException("authorId does not exist");
        }
    }

    private void validateCategoryExists(Long categoryId) {
        if (categoryId == null) {
            return;
        }

        BlogCategory category = blogCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw new IllegalArgumentException("categoryId does not exist");
        }
    }

    private String getRootMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage();
    }

    private IllegalArgumentException translateDuplicateArticleSlugException(DataAccessException exception) {
        if (isDuplicateConstraintViolation(exception)) {
            return new IllegalArgumentException("articleSlug already exists");
        }
        throw exception;
    }

    private boolean isDuplicateConstraintViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != current) {
            if (current instanceof SQLException sqlException) {
                if (sqlException.getErrorCode() == 1062) {
                    return true;
                }
                String sqlState = sqlException.getSQLState();
                if ("23505".equals(sqlState)) {
                    return true;
                }
            }
            String message = current.getMessage();
            if (message != null) {
                String normalizedMessage = message.toLowerCase(Locale.ROOT);
                if (normalizedMessage.contains("duplicate entry")
                        || normalizedMessage.contains("duplicate key")
                        || normalizedMessage.contains("unique constraint")
                        || normalizedMessage.contains("unique index")
                        || normalizedMessage.contains("unique key")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
