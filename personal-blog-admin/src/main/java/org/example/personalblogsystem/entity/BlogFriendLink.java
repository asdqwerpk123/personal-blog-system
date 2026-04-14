package org.example.personalblogsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@TableName("blog_friend_link")
public class BlogFriendLink implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("site_name")
    private String siteName;

    @TableField("site_url")
    private String siteUrl;

    @TableField("site_logo")
    private String siteLogo;

    @TableField("site_desc")
    private String siteDesc;

    @TableField("owner_name")
    private String ownerName;

    @TableField("contact_email")
    private String contactEmail;

    @TableField("link_status")
    private String linkStatus;

    @TableField("created_by")
    private Long createdBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableLogic
    private Boolean deleted;
}
