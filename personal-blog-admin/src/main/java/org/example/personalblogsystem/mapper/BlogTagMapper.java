package org.example.personalblogsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.personalblogsystem.entity.BlogTag;

import java.util.List;

public interface BlogTagMapper extends BaseMapper<BlogTag> {

    @Select("""
            select t.id, t.tag_name, t.description, t.created_by, t.create_time, t.update_time, t.deleted
            from blog_tag t
            inner join blog_article_tag at on t.id = at.tag_id
            where at.article_id = #{articleId}
              and at.deleted = 0
              and t.deleted = 0
            order by at.id asc
            """)
    List<BlogTag> selectByArticleId(@Param("articleId") Long articleId);
}
