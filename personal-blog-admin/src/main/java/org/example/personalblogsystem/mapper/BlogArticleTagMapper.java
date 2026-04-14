package org.example.personalblogsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.personalblogsystem.entity.BlogArticleTag;

import java.util.List;

public interface BlogArticleTagMapper extends BaseMapper<BlogArticleTag> {

    @Select("""
            select id, article_id, tag_id, create_time, update_time, deleted
            from blog_article_tag
            where article_id = #{articleId}
            order by id asc
            """)
    List<BlogArticleTag> selectAllByArticleId(@Param("articleId") Long articleId);

    @Update("""
            update blog_article_tag
            set deleted = 0,
                update_time = current_timestamp
            where id = #{id}
            """)
    int restoreById(@Param("id") Long id);

    @Update("""
            update blog_article_tag
            set deleted = 1,
                update_time = current_timestamp
            where id = #{id}
            """)
    int logicDeleteById(@Param("id") Long id);
}
