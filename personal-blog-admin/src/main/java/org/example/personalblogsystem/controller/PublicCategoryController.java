package org.example.personalblogsystem.controller;

import org.example.personalblogcommon.result.Result;
import org.example.personalblogsystem.dto.PublicCategoryResponse;
import org.example.personalblogsystem.service.IBlogCategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/public/categories")
public class PublicCategoryController {

    private final IBlogCategoryService blogCategoryService;

    public PublicCategoryController(IBlogCategoryService blogCategoryService) {
        this.blogCategoryService = blogCategoryService;
    }

    @GetMapping("/list")
    public Result<List<PublicCategoryResponse>> list() {
        return Result.ok(blogCategoryService.listPublicCategories());
    }
}
