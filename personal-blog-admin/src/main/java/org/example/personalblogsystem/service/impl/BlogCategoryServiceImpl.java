package org.example.personalblogsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.personalblogsystem.auth.AdminAuthContext;
import org.example.personalblogsystem.entity.BlogArticle;
import org.example.personalblogsystem.entity.BlogCategory;
import org.example.personalblogsystem.mapper.BlogArticleMapper;
import org.example.personalblogsystem.mapper.BlogCategoryMapper;
import org.example.personalblogsystem.mapper.SysUserMapper;
import org.example.personalblogsystem.service.IBlogCategoryService;
import org.example.personalblogsystem.service.OperationLogRecordService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * <p>
 * 文章分类服务实现类
 * </p>
 *
 * @author student
 * @since 2026-03-31
 */
@Service
public class BlogCategoryServiceImpl extends ServiceImpl<BlogCategoryMapper, BlogCategory> implements IBlogCategoryService {

    private final SysUserMapper sysUserMapper;
    private final BlogArticleMapper blogArticleMapper;
    private final OperationLogRecordService operationLogRecordService;

    public BlogCategoryServiceImpl(SysUserMapper sysUserMapper,
                                   BlogArticleMapper blogArticleMapper,
                                   OperationLogRecordService operationLogRecordService) {
        this.sysUserMapper = sysUserMapper;
        this.blogArticleMapper = blogArticleMapper;
        this.operationLogRecordService = operationLogRecordService;
    }

    @Override
    public List<BlogCategory> listCategories() {
        LambdaQueryWrapper<BlogCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(BlogCategory::getSortNo, BlogCategory::getId);
        return list(queryWrapper);
    }

    @Override
    public Page<BlogCategory> pageCategories(long current, long size, String keyword) {
        LambdaQueryWrapper<BlogCategory> queryWrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            queryWrapper.and(wrapper -> wrapper.like(BlogCategory::getCategoryName, keyword)
                    .or()
                    .like(BlogCategory::getDescription, keyword));
        }
        queryWrapper.orderByAsc(BlogCategory::getSortNo, BlogCategory::getId);
        return page(new Page<>(current, size), queryWrapper);
    }

    @Override
    public BlogCategory createCategory(BlogCategory category) {
        category.setCreatedBy(AdminAuthContext.requireCurrentUser().getUserId());
        validateCreatedByExists(category.getCreatedBy());
        validateCategoryNameUnique(category.getCategoryName(), null);

        LocalDateTime now = LocalDateTime.now();
        category.setId(null);
        category.setSortNo(category.getSortNo() == null ? 0 : category.getSortNo());
        category.setCreateTime(now);
        category.setUpdateTime(now);
        category.setDeleted(false);
        try {
            save(category);
        } catch (DataAccessException exception) {
            throw translateWriteException(exception);
        }
        operationLogRecordService.recordSuccess("CATEGORY", category.getId(), "CREATE_CATEGORY", "新增分类：" + category.getCategoryName());
        return getById(category.getId());
    }

    @Override
    public BlogCategory updateCategory(Long id, BlogCategory category) {
        BlogCategory existing = getById(id);
        if (existing == null) {
            return null;
        }

        validateCategoryNameUnique(category.getCategoryName(), id);
        existing.setCategoryName(category.getCategoryName());
        existing.setDescription(category.getDescription());
        if (category.getSortNo() != null) {
            existing.setSortNo(category.getSortNo());
        }
        existing.setUpdateTime(LocalDateTime.now());
        try {
            if (!updateById(existing)) {
                return null;
            }
            operationLogRecordService.recordSuccess("CATEGORY", id, "UPDATE_CATEGORY", "编辑分类：" + existing.getCategoryName());
            return getById(id);
        } catch (DataAccessException exception) {
            throw translateWriteException(exception);
        }
    }

    @Override
    public boolean deleteCategory(Long id) {
        BlogCategory existing = getById(id);
        if (existing == null) {
            return false;
        }

        Long articleCount = blogArticleMapper.selectCount(new LambdaQueryWrapper<BlogArticle>()
                .eq(BlogArticle::getCategoryId, id));
        if (articleCount != null && articleCount > 0) {
            throw new IllegalArgumentException("category is referenced by articles");
        }

        boolean deleted = removeById(id);
        if (deleted) {
            operationLogRecordService.recordSuccess("CATEGORY", id, "DELETE_CATEGORY", "删除分类：" + existing.getCategoryName());
        }
        return deleted;
    }

    private void validateCreatedByExists(Long createdBy) {
        if (createdBy == null || sysUserMapper.selectById(createdBy) == null) {
            throw new IllegalArgumentException("createdBy is invalid");
        }
    }

    private void validateCategoryNameUnique(String categoryName, Long currentCategoryId) {
        LambdaQueryWrapper<BlogCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BlogCategory::getCategoryName, categoryName);
        if (currentCategoryId != null) {
            queryWrapper.ne(BlogCategory::getId, currentCategoryId);
        }

        Long duplicateCount = count(queryWrapper);
        if (duplicateCount != null && duplicateCount > 0) {
            throw new IllegalArgumentException("categoryName already exists");
        }
    }

    private RuntimeException translateWriteException(DataAccessException exception) {
        if (isDuplicateCategoryNameViolation(exception)) {
            return new IllegalArgumentException("categoryName already exists");
        }
        if (isCreatedByForeignKeyViolation(exception)) {
            return new IllegalArgumentException("createdBy is invalid");
        }
        throw exception;
    }

    private boolean isDuplicateCategoryNameViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != current) {
            if (current instanceof SQLException sqlException) {
                String normalizedSqlMessage = normalizeMessage(sqlException.getMessage());
                if (sqlException.getErrorCode() == 1062
                        && isCategoryNameUniqueKeyMessage(normalizedSqlMessage)) {
                    return true;
                }
            }
            String normalizedMessage = normalizeMessage(current.getMessage());
            if (isCategoryNameUniqueKeyMessage(normalizedMessage)
                    && normalizedMessage.contains("duplicate")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isCreatedByForeignKeyViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != current) {
            if (current instanceof SQLException sqlException) {
                String normalizedSqlMessage = normalizeMessage(sqlException.getMessage());
                if (sqlException.getErrorCode() == 1452
                        && isCategoryCreatedByForeignKeyMessage(normalizedSqlMessage)) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isCategoryNameUniqueKeyMessage(String normalizedMessage) {
        return normalizedMessage.contains("uk_blog_category_category_name")
                || (normalizedMessage.contains("blog_category")
                && normalizedMessage.contains("category_name")
                && normalizedMessage.contains("duplicate"));
    }

    private boolean isCategoryCreatedByForeignKeyMessage(String normalizedMessage) {
        return normalizedMessage.contains("fk_blog_category_created_by")
                || (normalizedMessage.contains("blog_category")
                && normalizedMessage.contains("created_by")
                && normalizedMessage.contains("foreign key"));
    }

    private String normalizeMessage(String message) {
        return message == null ? "" : message.toLowerCase(Locale.ROOT);
    }
}
