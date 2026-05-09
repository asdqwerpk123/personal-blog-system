package org.example.personalblogsystem.controller;

import org.example.personalblogcommon.result.Result;
import org.example.personalblogsystem.dto.PublicTagResponse;
import org.example.personalblogsystem.service.IBlogTagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/public/tags")
public class PublicTagController {

    private final IBlogTagService blogTagService;

    public PublicTagController(IBlogTagService blogTagService) {
        this.blogTagService = blogTagService;
    }

    @GetMapping("/list")
    public Result<List<PublicTagResponse>> list() {
        return Result.ok(blogTagService.listPublicTags());
    }
}
