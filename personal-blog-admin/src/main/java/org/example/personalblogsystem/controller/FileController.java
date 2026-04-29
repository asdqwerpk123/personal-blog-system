package org.example.personalblogsystem.controller;

import lombok.RequiredArgsConstructor;
import org.example.personalblogsystem.service.IMinioService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/files")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "minio", name = "enabled", havingValue = "true")
@ConditionalOnBean(IMinioService.class)
public class FileController {

    private final IMinioService minioService;

    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) {
        String url = minioService.upload(file);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "上传成功");
        result.put("url", url);

        return result;
    }
}
