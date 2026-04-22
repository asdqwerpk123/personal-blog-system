package org.example.personalblogsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.personalblogsystem.auth.AdminAuthContext;
import org.example.personalblogsystem.entity.BlogComment;
import org.example.personalblogsystem.mapper.BlogCommentMapper;
import org.example.personalblogsystem.service.IBlogCommentService;
import org.example.personalblogsystem.service.OperationLogRecordService;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class BlogCommentServiceImpl extends ServiceImpl<BlogCommentMapper, BlogComment> implements IBlogCommentService {

    private final JdbcTemplate jdbcTemplate;
    private final OperationLogRecordService operationLogRecordService;

    public BlogCommentServiceImpl(JdbcTemplate jdbcTemplate,
                                  OperationLogRecordService operationLogRecordService) {
        this.jdbcTemplate = jdbcTemplate;
        this.operationLogRecordService = operationLogRecordService;
    }

    @Override
    public Page<BlogComment> pageComments(long current, long size, String keyword, String status, Long articleId) {
        LambdaQueryWrapper<BlogComment> queryWrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            queryWrapper.like(BlogComment::getCommentContent, keyword);
        }
        if (status != null && !status.isBlank()) {
            queryWrapper.eq(BlogComment::getCommentStatus, normalizeStatus(status));
        }
        if (articleId != null) {
            queryWrapper.eq(BlogComment::getArticleId, articleId);
        }
        queryWrapper.orderByDesc(BlogComment::getUpdateTime, BlogComment::getId);
        return page(new Page<>(current, size), queryWrapper);
    }

    @Override
    public List<BlogComment> listCommentsByArticleId(Long articleId) {
        LambdaQueryWrapper<BlogComment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BlogComment::getArticleId, articleId)
                .orderByAsc(BlogComment::getCreateTime, BlogComment::getId);
        return list(queryWrapper);
    }

    @Override
    public BlogComment updateCommentStatus(Long id, String status) {
        BlogComment existing = getById(id);
        if (existing == null) {
            return null;
        }

        existing.setCommentStatus(normalizeStatus(status));
        existing.setUpdateTime(LocalDateTime.now());
        if (!updateById(existing)) {
            return null;
        }
        operationLogRecordService.recordSuccess("COMMENT", id, "STATUS", "Update comment status success");
        return getById(id);
    }

    @Override
    public boolean deleteComment(Long id) {
        BlogComment existing = getById(id);
        if (existing == null) {
            return false;
        }

        try {
            Long operatorUserId = AdminAuthContext.requireCurrentUser().getUserId();
            jdbcTemplate.update("CALL sp_delete_comment(?, ?)", operatorUserId, id);
            return true;
        } catch (DataAccessException exception) {
            String message = getRootMessage(exception);
            if (message != null && message.contains("does not exist or has already been deleted")) {
                return false;
            }
            throw new IllegalArgumentException(message == null ? "delete comment failed" : message);
        }
    }

    private String normalizeStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!"PENDING".equals(normalized) && !"APPROVED".equals(normalized) && !"REJECTED".equals(normalized)) {
            throw new IllegalArgumentException("status must be one of PENDING, APPROVED, REJECTED");
        }
        return normalized;
    }

    private String getRootMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage();
    }
}
