package org.example.personalblogsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.personalblogcommon.exception.BlogException;
import org.example.personalblogsystem.auth.AdminAuthContext;
import org.example.personalblogsystem.dto.UserCommentCreateRequest;
import org.example.personalblogsystem.dto.UserCommentResponse;
import org.example.personalblogsystem.entity.BlogArticle;
import org.example.personalblogsystem.entity.BlogComment;
import org.example.personalblogsystem.entity.SysUser;
import org.example.personalblogsystem.mapper.BlogArticleMapper;
import org.example.personalblogsystem.mapper.BlogCommentMapper;
import org.example.personalblogsystem.mapper.SysUserMapper;
import org.example.personalblogsystem.service.IBlogCommentService;
import org.example.personalblogsystem.service.OperationLogRecordService;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class BlogCommentServiceImpl extends ServiceImpl<BlogCommentMapper, BlogComment> implements IBlogCommentService {

    private static final int FORBIDDEN_CODE = 403;
    private static final String FORBIDDEN_MESSAGE = "无权限操作";

    private final JdbcTemplate jdbcTemplate;
    private final BlogArticleMapper blogArticleMapper;
    private final SysUserMapper sysUserMapper;
    private final OperationLogRecordService operationLogRecordService;

    public BlogCommentServiceImpl(JdbcTemplate jdbcTemplate,
                                  BlogArticleMapper blogArticleMapper,
                                  SysUserMapper sysUserMapper,
                                  OperationLogRecordService operationLogRecordService) {
        this.jdbcTemplate = jdbcTemplate;
        this.blogArticleMapper = blogArticleMapper;
        this.sysUserMapper = sysUserMapper;
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
        operationLogRecordService.recordSuccess("COMMENT", id, "REVIEW_COMMENT", "评论审核：" + existing.getCommentStatus());
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
            boolean replacedProcedureLog = operationLogRecordService.replaceLatestSuccess(
                    operatorUserId,
                    "COMMENT",
                    id,
                    Set.of("LOGIC_DELETE", "DELETE_COMMENT"),
                    "DELETE_COMMENT",
                    "删除评论：" + id);
            if (!replacedProcedureLog) {
                operationLogRecordService.recordSuccess("COMMENT", id, "DELETE_COMMENT", "删除评论：" + id);
            }
            return true;
        } catch (DataAccessException exception) {
            String message = getRootMessage(exception);
            if (message != null && message.contains("does not exist or has already been deleted")) {
                return false;
            }
            throw new IllegalArgumentException(message == null ? "delete comment failed" : message);
        }
    }

    @Override
    public Page<UserCommentResponse> pageUserComments(long current, long size, Long userId, String keyword, String status) {
        LambdaQueryWrapper<BlogComment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BlogComment::getUserId, userId);
        if (StringUtils.hasText(keyword)) {
            queryWrapper.like(BlogComment::getCommentContent, keyword.trim());
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(BlogComment::getCommentStatus, normalizeStatus(status));
        }
        queryWrapper.orderByDesc(BlogComment::getCreateTime, BlogComment::getId);

        Page<BlogComment> commentPage = page(new Page<>(current, size), queryWrapper);
        Page<UserCommentResponse> responsePage = new Page<>(
                commentPage.getCurrent(),
                commentPage.getSize(),
                commentPage.getTotal());
        responsePage.setRecords(commentPage.getRecords().stream().map(this::toUserCommentResponse).toList());
        return responsePage;
    }

    @Override
    public List<UserCommentResponse> listUserArticleComments(Long userId, Long articleId) {
        validateOwnedArticle(userId, articleId);
        return listCommentsByArticleId(articleId).stream()
                .map(this::toUserCommentResponse)
                .toList();
    }

    @Override
    @Transactional
    public UserCommentResponse createUserComment(Long userId, UserCommentCreateRequest request) {
        if (request == null || request.getArticleId() == null) {
            throw new IllegalArgumentException("文章不存在");
        }
        if (!StringUtils.hasText(request.getCommentContent()) || request.getCommentContent().trim().length() < 2) {
            throw new IllegalArgumentException("评论内容不能少于 2 个字符");
        }
        validateCommentableArticle(request.getArticleId());

        LocalDateTime now = LocalDateTime.now();
        BlogComment comment = new BlogComment();
        comment.setArticleId(request.getArticleId());
        comment.setParentId(null);
        comment.setUserId(userId);
        comment.setReplyToUserId(null);
        comment.setCommentContent(request.getCommentContent().trim());
        comment.setCommentStatus("PENDING");
        comment.setCreateTime(now);
        comment.setUpdateTime(now);
        comment.setDeleted(false);
        save(comment);
        operationLogRecordService.recordSuccess(userId, "COMMENT", comment.getId(), "CREATE_COMMENT",
                "发表评论: " + comment.getId());
        return toUserCommentResponse(getById(comment.getId()));
    }

    @Override
    @Transactional
    public boolean deleteUserComment(Long userId, Long id) {
        BlogComment existing = getById(id);
        if (existing == null) {
            throw new BlogException(404, "评论不存在");
        }
        if (!userId.equals(existing.getUserId())) {
            throw forbidden();
        }

        try {
            jdbcTemplate.update("CALL sp_delete_comment(?, ?)", userId, id);
            operationLogRecordService.replaceLatestSuccess(
                    userId,
                    "COMMENT",
                    id,
                    Set.of("LOGIC_DELETE", "DELETE_COMMENT"),
                    "DELETE_COMMENT",
                    "删除评论: " + id);
            return true;
        } catch (DataAccessException exception) {
            String message = getRootMessage(exception);
            if (message != null && message.toLowerCase(Locale.ROOT).contains("own comment")) {
                throw forbidden();
            }
            if (message != null && message.toLowerCase(Locale.ROOT).contains("does not exist")) {
                throw new BlogException(404, "评论不存在");
            }
            throw new IllegalArgumentException(message == null ? "删除评论失败" : message);
        }
    }

    private String normalizeStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!"PENDING".equals(normalized) && !"APPROVED".equals(normalized) && !"REJECTED".equals(normalized)) {
            throw new IllegalArgumentException("status must be one of PENDING, APPROVED, REJECTED");
        }
        return normalized;
    }

    private void validateCommentableArticle(Long articleId) {
        BlogArticle article = blogArticleMapper.selectById(articleId);
        if (article == null) {
            throw new BlogException(404, "文章不存在");
        }
        if (!"PUBLISHED".equals(article.getArticleStatus()) || !Boolean.TRUE.equals(article.getAllowComment())) {
            throw new IllegalArgumentException("文章暂不允许评论");
        }
    }

    private void validateOwnedArticle(Long userId, Long articleId) {
        BlogArticle article = blogArticleMapper.selectById(articleId);
        if (article == null) {
            throw new BlogException(404, "\u6587\u7ae0\u4e0d\u5b58\u5728");
        }
        if (userId == null || !userId.equals(article.getAuthorId())) {
            throw forbidden();
        }
    }

    private BlogException forbidden() {
        return new BlogException(FORBIDDEN_CODE, FORBIDDEN_MESSAGE);
    }

    private UserCommentResponse toUserCommentResponse(BlogComment comment) {
        UserCommentResponse response = new UserCommentResponse();
        response.setId(comment.getId());
        response.setArticleId(comment.getArticleId());
        response.setArticleTitle(resolveArticleTitle(comment.getArticleId()));
        response.setUserId(comment.getUserId());
        response.setNickName(resolveUserDisplayName(comment.getUserId()));
        response.setCommentContent(comment.getCommentContent());
        response.setCommentStatus(comment.getCommentStatus());
        response.setCreateTime(comment.getCreateTime());
        response.setUpdateTime(comment.getUpdateTime());
        return response;
    }

    private String resolveArticleTitle(Long articleId) {
        if (articleId == null) {
            return null;
        }
        BlogArticle article = blogArticleMapper.selectById(articleId);
        return article == null ? null : article.getArticleTitle();
    }

    private String resolveUserDisplayName(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getNickName()) ? user.getNickName() : user.getUserName();
    }

    private String getRootMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage();
    }
}
