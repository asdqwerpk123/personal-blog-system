package org.example.personalblogsystem.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.personalblogsystem.dto.PublicTagResponse;
import org.example.personalblogsystem.entity.BlogTag;

import java.util.List;

public interface IBlogTagService extends IService<BlogTag> {

    List<PublicTagResponse> listPublicTags();

    Page<BlogTag> pageTags(long current, long size, String keyword);

    BlogTag createTag(BlogTag tag);

    BlogTag updateTag(Long id, BlogTag tag);

    boolean deleteTag(Long id);
}
