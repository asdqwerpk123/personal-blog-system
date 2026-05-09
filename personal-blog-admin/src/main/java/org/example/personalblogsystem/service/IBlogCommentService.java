package org.example.personalblogsystem.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.personalblogsystem.dto.PublicCommentResponse;
import org.example.personalblogsystem.dto.UserCommentCreateRequest;
import org.example.personalblogsystem.dto.UserCommentResponse;
import org.example.personalblogsystem.entity.BlogComment;

import java.util.List;

public interface IBlogCommentService extends IService<BlogComment> {

    Page<BlogComment> pageComments(long current, long size, String keyword, String status, Long articleId);

    List<BlogComment> listCommentsByArticleId(Long articleId);

    Page<PublicCommentResponse> pagePublicCommentsByArticleId(Long articleId, long current, long size);

    BlogComment updateCommentStatus(Long id, String status);

    boolean deleteComment(Long id);

    Page<UserCommentResponse> pageUserComments(long current, long size, Long userId, String keyword, String status);

    List<UserCommentResponse> listUserArticleComments(Long userId, Long articleId);

    UserCommentResponse createUserComment(Long userId, UserCommentCreateRequest request);

    boolean deleteUserComment(Long userId, Long id);
}
