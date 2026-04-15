package org.example.personalblogsystem.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.personalblogsystem.entity.BlogComment;

import java.util.List;

public interface IBlogCommentService extends IService<BlogComment> {

    Page<BlogComment> pageComments(long current, long size, String keyword, String status, Long articleId);

    List<BlogComment> listCommentsByArticleId(Long articleId);

    BlogComment updateCommentStatus(Long id, String status);

    boolean deleteComment(Long id, Long operatorUserId);
}
