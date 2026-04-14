package org.example.personalblogsystem.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.personalblogsystem.entity.BlogArticle;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 文章表 服务类
 * </p>
 *
 * @author student
 * @since 2026-03-31
 */
public interface IBlogArticleService extends IService<BlogArticle> {

    Page<BlogArticle> pageArticles(long current, long size, String keyword);

    BlogArticle createArticle(BlogArticle article);

    BlogArticle updateArticle(Long id, BlogArticle article);

    BlogArticle updateArticleStatus(Long id, String status);

    boolean deleteArticle(Long id, Long operatorUserId);

}
