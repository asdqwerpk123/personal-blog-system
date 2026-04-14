package org.example.personalblogsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.personalblogsystem.entity.BlogArticle;
import org.example.personalblogsystem.entity.BlogCategory;
import org.example.personalblogsystem.mapper.BlogArticleMapper;
import org.example.personalblogsystem.mapper.BlogCategoryMapper;
import org.example.personalblogsystem.service.IBlogCategoryService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 鏂囩珷鍒嗙被琛?鏈嶅姟瀹炵幇绫?
 * </p>
 *
 * @author student
 * @since 2026-03-31
 */
@Service
public class BlogCategoryServiceImpl extends ServiceImpl<BlogCategoryMapper, BlogCategory> implements IBlogCategoryService {

    private final BlogArticleMapper blogArticleMapper;

    public BlogCategoryServiceImpl(BlogArticleMapper blogArticleMapper) {
        this.blogArticleMapper = blogArticleMapper;
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
        LocalDateTime now = LocalDateTime.now();
        category.setId(null);
        category.setSortNo(category.getSortNo() == null ? 0 : category.getSortNo());
        category.setCreateTime(now);
        category.setUpdateTime(now);
        category.setDeleted(false);
        save(category);
        return category;
    }

    @Override
    public BlogCategory updateCategory(Long id, BlogCategory category) {
        BlogCategory existing = getById(id);
        if (existing == null) {
            return null;
        }

        existing.setCategoryName(category.getCategoryName());
        existing.setDescription(category.getDescription());
        if (category.getSortNo() != null) {
            existing.setSortNo(category.getSortNo());
        }
        existing.setUpdateTime(LocalDateTime.now());
        return updateById(existing) ? existing : null;
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

        return removeById(id);
    }
}
