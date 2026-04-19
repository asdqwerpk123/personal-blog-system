package org.example.personalblogsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.personalblogcommon.result.Result;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.example.personalblogsystem.dto.BlogFriendLinkCreateRequest;
import org.example.personalblogsystem.entity.BlogFriendLink;
import org.example.personalblogsystem.service.IBlogFriendLinkService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/admin/friend-link")
public class BlogFriendLinkController {

    private static final long MAX_PAGE_SIZE = 100L;

    private final IBlogFriendLinkService blogFriendLinkService;

    public BlogFriendLinkController(IBlogFriendLinkService blogFriendLinkService) {
        this.blogFriendLinkService = blogFriendLinkService;
    }

    @GetMapping("/page")
    public Result<Page<BlogFriendLink>> page(@RequestParam long current,
                                             @RequestParam long size,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String status) {
        validatePageRequest(current, size);
        validateStatusIfPresent(status);
        return Result.ok(blogFriendLinkService.pageFriendLinks(current, size, keyword, status));
    }

    @PostMapping
    public Result<BlogFriendLink> create(@RequestBody BlogFriendLinkCreateRequest request) {
        validateFriendLinkForCreate(request);
        return Result.ok(blogFriendLinkService.createFriendLink(toFriendLink(request)));
    }

    @PutMapping("/{id}")
    public Result<BlogFriendLink> update(@PathVariable Long id, @RequestBody BlogFriendLink friendLink) {
        validateFriendLinkForUpdate(friendLink);
        BlogFriendLink updatedFriendLink = blogFriendLinkService.updateFriendLink(id, friendLink);
        return updatedFriendLink == null ? Result.fail(ResultCodeEnum.NOT_FOUND) : Result.ok(updatedFriendLink);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return blogFriendLinkService.deleteFriendLink(id) ? Result.ok(null) : Result.fail(ResultCodeEnum.NOT_FOUND);
    }

    private void validatePageRequest(long current, long size) {
        if (current <= 0) {
            throw new IllegalArgumentException("current must be greater than 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be greater than 0");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must not exceed " + MAX_PAGE_SIZE);
        }
    }

    private void validateFriendLinkForCreate(BlogFriendLinkCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.getSiteName())) {
            throw new IllegalArgumentException("siteName must not be blank");
        }
        if (request == null || !StringUtils.hasText(request.getSiteUrl())) {
            throw new IllegalArgumentException("siteUrl must not be blank");
        }
        validateStatusIfPresent(request.getLinkStatus());
    }

    private void validateFriendLinkForUpdate(BlogFriendLink friendLink) {
        validateSiteName(friendLink);
        validateSiteUrl(friendLink);
        validateStatusIfPresent(friendLink == null ? null : friendLink.getLinkStatus());
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

    private void validateStatusIfPresent(String status) {
        if (!StringUtils.hasText(status)) {
            return;
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!"PENDING".equals(normalized) && !"APPROVED".equals(normalized) && !"REJECTED".equals(normalized)) {
            throw new IllegalArgumentException("linkStatus must be one of PENDING, APPROVED, REJECTED");
        }
    }

    private BlogFriendLink toFriendLink(BlogFriendLinkCreateRequest request) {
        BlogFriendLink friendLink = new BlogFriendLink();
        friendLink.setSiteName(request.getSiteName());
        friendLink.setSiteUrl(request.getSiteUrl());
        friendLink.setSiteLogo(request.getSiteLogo());
        friendLink.setSiteDesc(request.getSiteDesc());
        friendLink.setOwnerName(request.getOwnerName());
        friendLink.setContactEmail(request.getContactEmail());
        friendLink.setLinkStatus(request.getLinkStatus());
        return friendLink;
    }
}
