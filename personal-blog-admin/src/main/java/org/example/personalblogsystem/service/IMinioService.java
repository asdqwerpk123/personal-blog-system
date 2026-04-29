package org.example.personalblogsystem.service;

import org.springframework.web.multipart.MultipartFile;

public interface IMinioService {

    String upload(MultipartFile file);
}
