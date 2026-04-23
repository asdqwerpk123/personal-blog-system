package org.example.personalblogsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.personalblogsystem.auth.AdminAuthContext;
import org.example.personalblogsystem.entity.BlogFriendLink;
import org.example.personalblogsystem.mapper.BlogFriendLinkMapper;
import org.example.personalblogsystem.mapper.SysUserMapper;
import org.example.personalblogsystem.service.IBlogFriendLinkService;
import org.example.personalblogsystem.service.OperationLogRecordService;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class BlogFriendLinkServiceImpl extends ServiceImpl<BlogFriendLinkMapper, BlogFriendLink> implements IBlogFriendLinkService {

    private static final String DEFAULT_LINK_STATUS = "PENDING";

    private final JdbcTemplate jdbcTemplate;
    private final SysUserMapper sysUserMapper;
    private final OperationLogRecordService operationLogRecordService;

    public BlogFriendLinkServiceImpl(JdbcTemplate jdbcTemplate,
                                     SysUserMapper sysUserMapper,
                                     OperationLogRecordService operationLogRecordService) {
        this.jdbcTemplate = jdbcTemplate;
        this.sysUserMapper = sysUserMapper;
        this.operationLogRecordService = operationLogRecordService;
    }

    @Override
    public Page<BlogFriendLink> pageFriendLinks(long current, long size, String keyword, String status) {
        LambdaQueryWrapper<BlogFriendLink> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper.like(BlogFriendLink::getSiteName, keyword)
                    .or()
                    .like(BlogFriendLink::getSiteUrl, keyword)
                    .or()
                    .like(BlogFriendLink::getSiteDesc, keyword));
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(BlogFriendLink::getLinkStatus, normalizeStatus(status, null));
        }
        queryWrapper.orderByDesc(BlogFriendLink::getUpdateTime, BlogFriendLink::getId);
        return page(new Page<>(current, size), queryWrapper);
    }

    @Override
    public BlogFriendLink createFriendLink(BlogFriendLink friendLink) {
        Long currentUserId = AdminAuthContext.requireCurrentUser().getUserId();
        friendLink.setCreatedBy(currentUserId);
        validateCreatedByExists(currentUserId);
        validateFriendLinkForCreate(friendLink);

        String normalizedSiteName = friendLink.getSiteName().trim();
        String normalizedSiteUrl = friendLink.getSiteUrl().trim();
        String normalizedStatus = normalizeStatus(friendLink.getLinkStatus(), DEFAULT_LINK_STATUS);

        validateSiteUrlUnique(normalizedSiteUrl, null);

        LocalDateTime now = LocalDateTime.now();
        friendLink.setId(null);
        friendLink.setSiteName(normalizedSiteName);
        friendLink.setSiteUrl(normalizedSiteUrl);
        friendLink.setLinkStatus(normalizedStatus);
        friendLink.setCreateTime(now);
        friendLink.setUpdateTime(now);
        friendLink.setDeleted(false);
        try {
            save(friendLink);
        } catch (DataAccessException exception) {
            throw translateWriteException(exception);
        }
        BlogFriendLink created = getBySiteUrl(normalizedSiteUrl);
        operationLogRecordService.recordSuccess("FRIEND_LINK", created.getId(), "CREATE", "Create friend link success: " + created.getSiteName());
        return created;
    }

    @Override
    public BlogFriendLink updateFriendLink(Long id, BlogFriendLink friendLink) {
        BlogFriendLink existing = getById(id);
        if (existing == null) {
            return null;
        }

        validateFriendLinkForUpdate(friendLink);

        String normalizedSiteName = friendLink.getSiteName().trim();
        String normalizedSiteUrl = friendLink.getSiteUrl().trim();
        String normalizedStatus = friendLink.getLinkStatus() == null
                ? existing.getLinkStatus()
                : normalizeStatus(friendLink.getLinkStatus(), null);

        validateSiteUrlUnique(normalizedSiteUrl, id);

        existing.setSiteName(normalizedSiteName);
        existing.setSiteUrl(normalizedSiteUrl);
        existing.setSiteLogo(friendLink.getSiteLogo());
        existing.setSiteDesc(friendLink.getSiteDesc());
        existing.setOwnerName(friendLink.getOwnerName());
        existing.setContactEmail(friendLink.getContactEmail());
        existing.setLinkStatus(normalizedStatus);
        existing.setUpdateTime(LocalDateTime.now());
        try {
            if (!updateById(existing)) {
                return null;
            }
            operationLogRecordService.recordSuccess("FRIEND_LINK", id, "UPDATE", "Update friend link success: " + existing.getSiteName());
            return getById(id);
        } catch (DataAccessException exception) {
            throw translateWriteException(exception);
        }
    }

    @Override
    public boolean deleteFriendLink(Long id) {
        BlogFriendLink existing = getById(id);
        if (existing == null) {
            return false;
        }
        boolean deleted = removeById(id);
        if (deleted) {
            operationLogRecordService.recordSuccess("FRIEND_LINK", id, "DELETE", "Delete friend link success: " + existing.getSiteName());
        }
        return deleted;
    }

    private void validateFriendLinkForCreate(BlogFriendLink friendLink) {
        validateSiteName(friendLink);
        validateSiteUrl(friendLink);
        if (StringUtils.hasText(friendLink.getLinkStatus())) {
            normalizeStatus(friendLink.getLinkStatus(), null);
        }
    }

    private void validateFriendLinkForUpdate(BlogFriendLink friendLink) {
        validateSiteName(friendLink);
        validateSiteUrl(friendLink);
        if (friendLink != null && StringUtils.hasText(friendLink.getLinkStatus())) {
            normalizeStatus(friendLink.getLinkStatus(), null);
        }
    }

    private void validateSiteName(BlogFriendLink friendLink) {
        if (friendLink == null || !StringUtils.hasText(friendLink.getSiteName())) {
            throw new IllegalArgumentException("siteName must not be blank");
        }
    }

    private void validateSiteUrl(BlogFriendLink friendLink) {
        if (friendLink == null || !StringUtils.hasText(friendLink.getSiteUrl())) {
            throw new IllegalArgumentException("siteUrl must not be blank");
        }
    }

    private void validateSiteUrlUnique(String siteUrl, Long currentId) {
        StringBuilder sql = new StringBuilder("select count(1) from blog_friend_link where site_url = ?");
        if (currentId != null) {
            sql.append(" and id <> ?");
        }
        Long duplicateCount = currentId == null
                ? jdbcTemplate.queryForObject(sql.toString(), Long.class, siteUrl)
                : jdbcTemplate.queryForObject(sql.toString(), Long.class, siteUrl, currentId);
        if (duplicateCount != null && duplicateCount > 0) {
            throw new IllegalArgumentException("siteUrl already exists");
        }
    }

    private BlogFriendLink getBySiteUrl(String siteUrl) {
        return getOne(new LambdaQueryWrapper<BlogFriendLink>()
                .eq(BlogFriendLink::getSiteUrl, siteUrl)
                .eq(BlogFriendLink::getDeleted, false));
    }

    private void validateCreatedByExists(Long createdBy) {
        if (createdBy == null || sysUserMapper.selectById(createdBy) == null) {
            throw new IllegalArgumentException("createdBy is invalid");
        }
    }

    private RuntimeException translateWriteException(DataAccessException exception) {
        if (isDuplicateSiteUrlViolation(exception)) {
            return new IllegalArgumentException("siteUrl already exists");
        }
        if (isCreatedByForeignKeyViolation(exception)) {
            return new IllegalArgumentException("createdBy is invalid");
        }
        throw exception;
    }

    private boolean isDuplicateSiteUrlViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != current) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("duplicate")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isCreatedByForeignKeyViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != current) {
            if (current instanceof java.sql.SQLException sqlException && sqlException.getErrorCode() == 1452) {
                String message = sqlException.getMessage();
                if (message != null) {
                    String normalizedMessage = message.toLowerCase(Locale.ROOT);
                    if (normalizedMessage.contains("fk_blog_friend_link_created_by")
                            || (normalizedMessage.contains("blog_friend_link")
                            && normalizedMessage.contains("created_by")
                            && normalizedMessage.contains("foreign key"))) {
                        return true;
                    }
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private String normalizeStatus(String status, String defaultStatus) {
        String value = status == null ? null : status.trim();
        if (value == null || value.isEmpty()) {
            if (defaultStatus != null) {
                return defaultStatus;
            }
            throw new IllegalArgumentException("linkStatus must be one of PENDING, APPROVED, REJECTED");
        }

        String normalized = value.toUpperCase(Locale.ROOT);
        if (!"PENDING".equals(normalized) && !"APPROVED".equals(normalized) && !"REJECTED".equals(normalized)) {
            throw new IllegalArgumentException("linkStatus must be one of PENDING, APPROVED, REJECTED");
        }
        return normalized;
    }
}
