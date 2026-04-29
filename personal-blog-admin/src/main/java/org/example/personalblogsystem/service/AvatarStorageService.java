package org.example.personalblogsystem.service;

import org.example.personalblogsystem.auth.AdminAuthContext;
import org.example.personalblogsystem.auth.AdminAuthPrincipal;
import org.example.personalblogsystem.config.AdminUploadProperties;
import org.example.personalblogsystem.dto.FileUploadResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AvatarStorageService {

    private static final long MAX_AVATAR_SIZE = 2L * 1024L * 1024L;
    private static final Map<String, String> ALLOWED_IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final AdminUploadProperties uploadProperties;
    private final OperationLogRecordService operationLogRecordService;

    public AvatarStorageService(AdminUploadProperties uploadProperties,
                                OperationLogRecordService operationLogRecordService) {
        this.uploadProperties = uploadProperties;
        this.operationLogRecordService = operationLogRecordService;
    }

    public FileUploadResponse storeAvatar(MultipartFile file) {
        validateFile(file);

        String extension = ALLOWED_IMAGE_EXTENSIONS.get(normalizeContentType(file.getContentType()));
        String fileName = UUID.randomUUID() + extension;
        Path uploadDir = Path.of(uploadProperties.getAvatarDir()).toAbsolutePath().normalize();
        Path target = uploadDir.resolve(fileName).normalize();

        try {
            Files.createDirectories(uploadDir);
            file.transferTo(target);
        } catch (IOException exception) {
            throw new IllegalArgumentException("图片上传失败，请稍后重试");
        }

        String url = "/uploads/avatars/" + fileName;
        AdminAuthPrincipal currentUser = AdminAuthContext.requireCurrentUser();
        operationLogRecordService.recordSuccess(
                currentUser.getUserId(),
                "USER_PROFILE",
                currentUser.getUserId(),
                "UPLOAD_AVATAR",
                "上传头像：" + url);
        return new FileUploadResponse(url);
    }

    public FileUploadResponse storeUserAvatar(MultipartFile file, Long operatorUserId) {
        validateFile(file);

        String extension = ALLOWED_IMAGE_EXTENSIONS.get(normalizeContentType(file.getContentType()));
        String fileName = UUID.randomUUID() + extension;
        Path uploadDir = Path.of(uploadProperties.getAvatarDir()).toAbsolutePath().normalize();
        Path target = uploadDir.resolve(fileName).normalize();

        try {
            Files.createDirectories(uploadDir);
            file.transferTo(target);
        } catch (IOException exception) {
            throw new IllegalArgumentException("图片上传失败，请稍后重试");
        }

        String url = "/uploads/avatars/" + fileName;
        operationLogRecordService.recordSuccess(
                operatorUserId,
                "USER_PROFILE",
                operatorUserId,
                "UPLOAD_AVATAR",
                "上传头像: " + url);
        return new FileUploadResponse(url);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择图片文件");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException("图片大小不能超过 2MB");
        }
        if (!ALLOWED_IMAGE_EXTENSIONS.containsKey(normalizeContentType(file.getContentType()))) {
            throw new IllegalArgumentException("不支持的图片格式");
        }
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }
}
