package org.example.personalblogsystem.service;

import org.example.personalblogsystem.entity.BlogCategory;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * <p>
 * 文章分类表 服务类
 * </p>
 *
 * @author student
 * @since 2026-03-31
 */
public interface IBlogCategoryService extends IService<BlogCategory> {

    List<BlogCategory> listCategories();

    Page<BlogCategory> pageCategories(long current, long size, String keyword);

    BlogCategory createCategory(BlogCategory category);

    BlogCategory updateCategory(Long id, BlogCategory category);

    boolean deleteCategory(Long id);

}
