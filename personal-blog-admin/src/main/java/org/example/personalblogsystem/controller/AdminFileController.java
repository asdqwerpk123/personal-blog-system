package org.example.personalblogsystem.controller;

import org.example.personalblogcommon.result.Result;
import org.example.personalblogsystem.dto.FileUploadResponse;
import org.example.personalblogsystem.service.AvatarStorageService;
import org.example.personalblogsystem.service.FriendLinkLogoStorageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/files")
public class AdminFileController {

    private final FriendLinkLogoStorageService friendLinkLogoStorageService;
    private final AvatarStorageService avatarStorageService;

    public AdminFileController(FriendLinkLogoStorageService friendLinkLogoStorageService,
                               AvatarStorageService avatarStorageService) {
        this.friendLinkLogoStorageService = friendLinkLogoStorageService;
        this.avatarStorageService = avatarStorageService;
    }

    @PostMapping("/friend-link-logo")
    public Result<FileUploadResponse> uploadFriendLinkLogo(@RequestParam("file") MultipartFile file) {
        return Result.ok(friendLinkLogoStorageService.storeFriendLinkLogo(file));
    }

    @PostMapping("/avatar")
    public Result<FileUploadResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.ok(avatarStorageService.storeAvatar(file));
    }
}
