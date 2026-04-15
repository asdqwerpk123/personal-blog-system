package org.example.personalblogsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.personalblogsystem.entity.BlogArticleTag;
import org.example.personalblogsystem.entity.BlogTag;

import java.util.List;

public interface IBlogArticleTagService extends IService<BlogArticleTag> {

    List<BlogTag> listTagsByArticleId(Long articleId);

    List<BlogTag> replaceArticleTags(Long articleId, List<Long> tagIds);
}
