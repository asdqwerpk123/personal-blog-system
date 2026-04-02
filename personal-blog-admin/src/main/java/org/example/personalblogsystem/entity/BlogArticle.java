package org.example.personalblogsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 文章表
 * </p>
 *
 * @author student
 * @since 2026-03-31
 */
@Getter
@Setter
@ToString
@TableName("blog_article")
public class BlogArticle implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文章标题
     */
    @TableField("article_title")
    private String articleTitle;

    /**
     * 文章短链接标识
     */
    @TableField("article_slug")
    private String articleSlug;

    /**
     * 文章摘要
     */
    @TableField("article_summary")
    private String articleSummary;

    /**
     * 封面图地址
     */
    @TableField("cover_url")
    private String coverUrl;

    /**
     * 文章内容
     */
    @TableField("article_content")
    private String articleContent;

    /**
     * 作者用户ID
     */
    @TableField("author_id")
    private Long authorId;

    /**
     * 分类ID
     */
    @TableField("category_id")
    private Long categoryId;

    /**
     * 文章状态
     */
    @TableField("article_status")
    private String articleStatus;

    /**
     * 是否置顶，0否，1是
     */
    @TableField("top_flag")
    private Boolean topFlag;

    /**
     * 是否允许评论，0否，1是
     */
    @TableField("allow_comment")
    private Boolean allowComment;

    /**
     * 浏览量
     */
    @TableField("view_count")
    private Integer viewCount;

    /**
     * 点赞量
     */
    @TableField("like_count")
    private Integer likeCount;

    /**
     * 发布时间
     */
    @TableField("published_time")
    private LocalDateTime publishedTime;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除，0未删除，1已删除
     */
    @TableField("deleted")
    private Boolean deleted;
}
