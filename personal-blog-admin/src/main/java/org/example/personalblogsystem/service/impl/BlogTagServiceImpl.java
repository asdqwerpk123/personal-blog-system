package org.example.personalblogsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.personalblogsystem.auth.AdminAuthContext;
import org.example.personalblogsystem.entity.BlogArticleTag;
import org.example.personalblogsystem.entity.BlogTag;
import org.example.personalblogsystem.mapper.BlogArticleTagMapper;
import org.example.personalblogsystem.mapper.BlogTagMapper;
import org.example.personalblogsystem.service.IBlogTagService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class BlogTagServiceImpl extends ServiceImpl<BlogTagMapper, BlogTag> implements IBlogTagService {

    private final BlogArticleTagMapper blogArticleTagMapper;

    public BlogTagServiceImpl(BlogArticleTagMapper blogArticleTagMapper) {
        this.blogArticleTagMapper = blogArticleTagMapper;
    }

    @Override
    public Page<BlogTag> pageTags(long current, long size, String keyword) {
        LambdaQueryWrapper<BlogTag> queryWrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            queryWrapper.and(wrapper -> wrapper.like(BlogTag::getTagName, keyword)
                    .or()
                    .like(BlogTag::getDescription, keyword));
        }
        queryWrapper.orderByDesc(BlogTag::getUpdateTime, BlogTag::getId);
        return page(new Page<>(current, size), queryWrapper);
    }

    @Override
    public BlogTag createTag(BlogTag tag) {
        tag.setCreatedBy(AdminAuthContext.requireCurrentUser().getUserId());
        validateTagNameUnique(tag.getTagName(), null);

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

    @Override
    public boolean deleteTag(Long id) {
        BlogTag existing = getById(id);
        if (existing == null) {
            return false;
        }

        Long articleTagCount = blogArticleTagMapper.selectCount(new LambdaQueryWrapper<BlogArticleTag>()
                .eq(BlogArticleTag::getTagId, id)
                .eq(BlogArticleTag::getDeleted, false));
        if (articleTagCount != null && articleTagCount > 0) {
            throw new IllegalArgumentException("tag is referenced by articles");
        }

        return removeById(id);
    }

    private void validateTagNameUnique(String tagName, Long currentTagId) {
        LambdaQueryWrapper<BlogTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BlogTag::getTagName, tagName);
        if (currentTagId != null) {
            queryWrapper.ne(BlogTag::getId, currentTagId);
        }

        Long duplicateCount = count(queryWrapper);
        if (duplicateCount != null && duplicateCount > 0) {
            throw new IllegalArgumentException("tagName already exists");
        }
    }

    private IllegalArgumentException translateDuplicateTagNameException(DataAccessException exception) {
        if (isDuplicateConstraintViolation(exception)) {
            return new IllegalArgumentException("tagName already exists");
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
