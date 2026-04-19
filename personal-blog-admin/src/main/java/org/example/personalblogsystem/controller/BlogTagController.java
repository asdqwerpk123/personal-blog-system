package org.example.personalblogsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.personalblogcommon.result.Result;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.example.personalblogsystem.dto.BlogTagCreateRequest;
import org.example.personalblogsystem.entity.BlogTag;
import org.example.personalblogsystem.service.IBlogTagService;
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

@RestController
@RequestMapping("/admin/tag")
public class BlogTagController {

    private static final long MAX_PAGE_SIZE = 100L;

    private final IBlogTagService blogTagService;

    public BlogTagController(IBlogTagService blogTagService) {
        this.blogTagService = blogTagService;
    }

    @GetMapping("/page")
    public Result<Page<BlogTag>> page(@RequestParam long current,
                                      @RequestParam long size,
                                      @RequestParam(required = false) String keyword) {
        validatePageRequest(current, size);
        return Result.ok(blogTagService.pageTags(current, size, keyword));
    }

    @PostMapping
    public Result<BlogTag> create(@RequestBody BlogTagCreateRequest request) {
        validateTagForCreate(request);
        return Result.ok(blogTagService.createTag(toTag(request)));
    }

    @PutMapping("/{id}")
    public Result<BlogTag> update(@PathVariable Long id, @RequestBody BlogTag tag) {
        validateTagForUpdate(tag);
        BlogTag updatedTag = blogTagService.updateTag(id, tag);
        return updatedTag == null ? Result.fail(ResultCodeEnum.NOT_FOUND) : Result.ok(updatedTag);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return blogTagService.deleteTag(id) ? Result.ok(null) : Result.fail(ResultCodeEnum.NOT_FOUND);
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

    private void validateTagForCreate(BlogTagCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.getTagName())) {
            throw new IllegalArgumentException("tagName must not be blank");
        }
    }

    private void validateTagForUpdate(BlogTag tag) {
        validateTagName(tag);
    }

    private void validateTagName(BlogTag tag) {
        if (tag == null || !StringUtils.hasText(tag.getTagName())) {
            throw new IllegalArgumentException("tagName must not be blank");
        }
    }

    private BlogTag toTag(BlogTagCreateRequest request) {
        BlogTag tag = new BlogTag();
        tag.setTagName(request.getTagName());
        tag.setDescription(request.getDescription());
        return tag;
    }
}
