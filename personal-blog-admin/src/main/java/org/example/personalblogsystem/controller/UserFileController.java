package org.example.personalblogsystem.controller;

import org.example.personalblogcommon.result.Result;
import org.example.personalblogsystem.auth.UserAuthContext;
import org.example.personalblogsystem.dto.FileUploadResponse;
import org.example.personalblogsystem.service.AvatarStorageService;
import org.example.personalblogsystem.service.ISysUserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user/files")
public class UserFileController {

    private final AvatarStorageService avatarStorageService;
    private final ISysUserService sysUserService;

    public UserFileController(AvatarStorageService avatarStorageService,
                              ISysUserService sysUserService) {
        this.avatarStorageService = avatarStorageService;
        this.sysUserService = sysUserService;
    }

    @PostMapping("/avatar")
    public Result<FileUploadResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = UserAuthContext.requireCurrentUser().getUserId();
        FileUploadResponse response = avatarStorageService.storeUserAvatar(file, userId);
        sysUserService.updateUserAvatar(userId, response.getUrl());
        return Result.ok(response);
    }
}
