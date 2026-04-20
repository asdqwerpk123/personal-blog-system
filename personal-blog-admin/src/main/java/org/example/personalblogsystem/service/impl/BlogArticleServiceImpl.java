package org.example.personalblogsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.personalblogsystem.auth.AdminAuthContext;
import org.example.personalblogsystem.auth.AdminAuthPrincipal;
import org.example.personalblogsystem.entity.BlogArticle;
import org.example.personalblogsystem.entity.BlogCategory;
import org.example.personalblogsystem.mapper.BlogArticleMapper;
import org.example.personalblogsystem.mapper.BlogCategoryMapper;
import org.example.personalblogsystem.mapper.SysUserMapper;
import org.example.personalblogsystem.service.IBlogArticleService;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * <p>
 * 鏂囩珷琛?鏈嶅姟瀹炵幇绫?
 * </p>
 *
 * @author student
 * @since 2026-03-31
 */
@Service
public class BlogArticleServiceImpl extends ServiceImpl<BlogArticleMapper, BlogArticle> implements IBlogArticleService {

    private final JdbcTemplate jdbcTemplate;
    private final SysUserMapper sysUserMapper;
    private final BlogCategoryMapper blogCategoryMapper;

    public BlogArticleServiceImpl(JdbcTemplate jdbcTemplate,
                                  SysUserMapper sysUserMapper,
                                  BlogCategoryMapper blogCategoryMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.sysUserMapper = sysUserMapper;
        this.blogCategoryMapper = blogCategoryMapper;
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
    public BlogArticle createArticle(BlogArticle article) {
        AdminAuthPrincipal currentUser = AdminAuthContext.requireCurrentUser();
        article.setAuthorId(currentUser.getUserId());
        validateArticleReferences(article, null);
        LocalDateTime now = LocalDateTime.now();
        article.setId(null);
        article.setArticleStatus(normalizeStatus(article.getArticleStatus(), "DRAFT"));
        article.setTopFlag(article.getTopFlag() != null && article.getTopFlag());
        article.setAllowComment(article.getAllowComment() == null || article.getAllowComment());
        article.setPublishedTime(null);
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
        article.setAuthorId(preservedAuthorId);
        validateArticleReferences(article, id);
        existing.setArticleTitle(article.getArticleTitle());
        existing.setArticleSlug(article.getArticleSlug());
        existing.setArticleSummary(article.getArticleSummary());
        existing.setCoverUrl(article.getCoverUrl());
        existing.setArticleContent(article.getArticleContent());
        existing.setCategoryId(article.getCategoryId());
        existing.setTopFlag(article.getTopFlag() == null ? existing.getTopFlag() : article.getTopFlag());
        existing.setAllowComment(article.getAllowComment() == null ? existing.getAllowComment() : article.getAllowComment());
        existing.setAuthorId(preservedAuthorId);
        existing.setPublishedTime(preservedPublishedTime);
        existing.setUpdateTime(LocalDateTime.now());
        try {
            return updateById(existing) ? getById(id) : null;
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
        return updateById(existing) ? getById(id) : null;
    }

    @Override
    public boolean deleteArticle(Long id) {
        BlogArticle existing = getById(id);
        if (existing == null) {
            return false;
        }

        try {
            AdminAuthPrincipal currentUser = AdminAuthContext.requireCurrentUser();
            jdbcTemplate.update("CALL sp_delete_article(?, ?)", currentUser.getUserId(), id);
            return true;
        } catch (DataAccessException exception) {
            String message = getRootMessage(exception);
            if (message != null && message.contains("does not exist or has already been deleted")) {
                return false;
            }
            throw new IllegalArgumentException(message == null ? "delete article failed" : message);
        }
    }

    private String normalizeStatus(String status, String defaultStatus) {
        String value = status == null ? null : status.trim();
        if (value == null || value.isEmpty()) {
            return defaultStatus;
        }

        String normalized = value.toUpperCase(Locale.ROOT);
        if (!"DRAFT".equals(normalized) && !"PUBLISHED".equals(normalized) && !"PRIVATE".equals(normalized)) {
            throw new IllegalArgumentException("status must be one of DRAFT, PUBLISHED, PRIVATE");
        }
        return normalized;
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
