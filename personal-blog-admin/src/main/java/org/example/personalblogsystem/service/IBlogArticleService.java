package org.example.personalblogsystem.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.personalblogsystem.entity.BlogArticle;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.personalblogsystem.dto.PublicArticleDetailResponse;
import org.example.personalblogsystem.dto.PublicArticleResponse;
import org.example.personalblogsystem.dto.UserArticleRequest;
import org.example.personalblogsystem.dto.UserArticleResponse;
import org.example.personalblogsystem.dto.UserDashboardSummaryResponse;

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

    Page<PublicArticleResponse> pagePublicArticles(long current,
                                                   long size,
                                                   String keyword,
                                                   Long categoryId,
                                                   Long tagId);

    PublicArticleDetailResponse getPublicArticle(Long id);

    BlogArticle createArticle(BlogArticle article);

    BlogArticle updateArticle(Long id, BlogArticle article);

    BlogArticle updateArticleStatus(Long id, String status);

    int publishScheduledArticles();

    int publishArticleById(Long id);

    boolean deleteArticle(Long id);

    Page<UserArticleResponse> pageUserArticles(long current,
                                               long size,
                                               Long authorId,
                                               String title,
                                               Long categoryId,
                                               String status);

    UserArticleResponse getUserArticle(Long authorId, Long id);

    UserArticleResponse createUserArticle(Long authorId, UserArticleRequest request);

    UserArticleResponse updateUserArticle(Long authorId, Long id, UserArticleRequest request);

    UserArticleResponse updateUserArticleStatus(Long authorId, Long id, String status);

    boolean deleteUserArticle(Long authorId, Long id);

    UserDashboardSummaryResponse getUserDashboardSummary(Long authorId);

}
