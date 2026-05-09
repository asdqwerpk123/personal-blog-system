package org.example.personalblogsystem.controller;

import org.example.personalblogcommon.result.Result;
import org.example.personalblogsystem.dto.PublicFriendLinkResponse;
import org.example.personalblogsystem.service.IBlogFriendLinkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/public/friend-links")
public class PublicFriendLinkController {

    private final IBlogFriendLinkService blogFriendLinkService;

    public PublicFriendLinkController(IBlogFriendLinkService blogFriendLinkService) {
        this.blogFriendLinkService = blogFriendLinkService;
    }

    @GetMapping("/list")
    public Result<List<PublicFriendLinkResponse>> list() {
        return Result.ok(blogFriendLinkService.listPublicFriendLinks());
    }
}
