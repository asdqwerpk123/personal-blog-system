package org.example.personalblogsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.personalblogsystem.entity.BlogFriendLink;
import org.example.personalblogsystem.mapper.BlogFriendLinkMapper;
import org.example.personalblogsystem.mapper.SysUserMapper;
import org.example.personalblogsystem.service.IBlogFriendLinkService;
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

    public BlogFriendLinkServiceImpl(JdbcTemplate jdbcTemplate,
                                     SysUserMapper sysUserMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.sysUserMapper = sysUserMapper;
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
            throw translateDuplicateSiteUrlException(exception);
        }
        return getBySiteUrl(normalizedSiteUrl);
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
            return updateById(existing) ? getById(id) : null;
        } catch (DataAccessException exception) {
            throw translateDuplicateSiteUrlException(exception);
        }
    }

    @Override
    public boolean deleteFriendLink(Long id) {
        BlogFriendLink existing = getById(id);
        if (existing == null) {
            return false;
        }
        return removeById(id);
    }

    private void validateFriendLinkForCreate(BlogFriendLink friendLink) {
        validateSiteName(friendLink);
        validateSiteUrl(friendLink);
        if (friendLink == null || friendLink.getCreatedBy() == null) {
            throw new IllegalArgumentException("createdBy must not be null");
        }
        validateCreatedByExists(friendLink.getCreatedBy());
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
            throw new IllegalArgumentException("createdBy does not exist");
        }
    }

    private IllegalArgumentException translateDuplicateSiteUrlException(DataAccessException exception) {
        String message = exception.getMostSpecificCause() == null ? null : exception.getMostSpecificCause().getMessage();
        if (message != null && message.toLowerCase().contains("duplicate")) {
            return new IllegalArgumentException("siteUrl already exists");
        }
        throw exception;
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
