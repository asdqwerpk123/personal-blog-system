package org.example.personalblogsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.personalblogsystem.entity.BlogArticle;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * <p>
 * 文章表 Mapper 接口
 * </p>
 *
 * @author student
 * @since 2026-03-31
 */
public interface BlogArticleMapper extends BaseMapper<BlogArticle> {

    int publishScheduledArticles(@Param("now") LocalDateTime now,
                                 @Param("draftStatus") String draftStatus,
                                 @Param("publishedStatus") String publishedStatus);

}
