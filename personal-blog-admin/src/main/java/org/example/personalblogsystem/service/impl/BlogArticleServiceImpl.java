package org.example.personalblogsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.personalblogsystem.entity.BlogArticle;
import org.example.personalblogsystem.entity.BlogCategory;
import org.example.personalblogsystem.mapper.BlogArticleMapper;
import org.example.personalblogsystem.mapper.BlogCategoryMapper;
import org.example.personalblogsystem.mapper.SysUserMapper;
import org.example.personalblogsystem.service.IBlogArticleService;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
        save(article);
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
        return updateById(existing) ? getById(id) : null;
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
    public boolean deleteArticle(Long id, Long operatorUserId) {
        BlogArticle existing = getById(id);
        if (existing == null) {
            return false;
        }

        try {
            jdbcTemplate.update("CALL sp_delete_article(?, ?)", operatorUserId, id);
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

        String normalized = value.toUpperCase();
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
}
