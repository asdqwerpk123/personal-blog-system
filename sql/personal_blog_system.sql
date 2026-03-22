SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS personal_blog_system
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_general_ci;

USE personal_blog_system;

DROP TRIGGER IF EXISTS trg_block_physical_delete_sys_user;
DROP TRIGGER IF EXISTS trg_block_physical_delete_blog_article;
DROP TRIGGER IF EXISTS trg_block_physical_delete_blog_comment;
DROP TRIGGER IF EXISTS trg_set_article_published_at_insert;
DROP TRIGGER IF EXISTS trg_set_article_published_at_update;
DROP TRIGGER IF EXISTS trg_set_article_published_time_insert;
DROP TRIGGER IF EXISTS trg_set_article_published_time_update;

DROP PROCEDURE IF EXISTS sp_delete_user;
DROP PROCEDURE IF EXISTS sp_delete_article;
DROP PROCEDURE IF EXISTS sp_delete_comment;

DROP TABLE IF EXISTS sys_operation_log;
DROP TABLE IF EXISTS blog_friend_link;
DROP TABLE IF EXISTS blog_comment;
DROP TABLE IF EXISTS blog_article_tag;
DROP TABLE IF EXISTS blog_article;
DROP TABLE IF EXISTS blog_tag;
DROP TABLE IF EXISTS blog_category;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_role;

CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    role_code VARCHAR(30) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(30) NOT NULL COMMENT '角色名称',
    role_rank TINYINT NOT NULL COMMENT '角色等级，值越小权限越高',
    remark VARCHAR(255) NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除，0未删除，1已删除',
    UNIQUE KEY uk_sys_role_role_code (role_code),
    UNIQUE KEY uk_sys_role_role_rank (role_rank)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色表';

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_name VARCHAR(50) NOT NULL COMMENT '用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希值',
    nick_name VARCHAR(50) NOT NULL COMMENT '昵称',
    email VARCHAR(100) NOT NULL COMMENT '邮箱',
    phone VARCHAR(20) NULL COMMENT '手机号',
    avatar_url VARCHAR(255) NULL COMMENT '头像地址',
    introduction VARCHAR(255) NULL COMMENT '个人简介',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    user_status ENUM('ENABLED', 'DISABLED') NOT NULL DEFAULT 'ENABLED' COMMENT '用户状态',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除，0未删除，1已删除',
    UNIQUE KEY uk_sys_user_user_name (user_name),
    UNIQUE KEY uk_sys_user_email (email),
    UNIQUE KEY uk_sys_user_phone (phone),
    KEY idx_sys_user_role_id (role_id),
    CONSTRAINT fk_sys_user_role FOREIGN KEY (role_id) REFERENCES sys_role (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

CREATE TABLE blog_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    category_name VARCHAR(50) NOT NULL COMMENT '分类名称',
    description VARCHAR(255) NULL COMMENT '分类描述',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    created_by BIGINT NOT NULL COMMENT '创建人用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除，0未删除，1已删除',
    UNIQUE KEY uk_blog_category_category_name (category_name),
    KEY idx_blog_category_created_by (created_by),
    CONSTRAINT fk_blog_category_created_by FOREIGN KEY (created_by) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文章分类表';

CREATE TABLE blog_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    tag_name VARCHAR(50) NOT NULL COMMENT '标签名称',
    description VARCHAR(255) NULL COMMENT '标签描述',
    created_by BIGINT NOT NULL COMMENT '创建人用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除，0未删除，1已删除',
    UNIQUE KEY uk_blog_tag_tag_name (tag_name),
    KEY idx_blog_tag_created_by (created_by),
    CONSTRAINT fk_blog_tag_created_by FOREIGN KEY (created_by) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文章标签表';

CREATE TABLE blog_article (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    article_title VARCHAR(200) NOT NULL COMMENT '文章标题',
    article_slug VARCHAR(200) NOT NULL COMMENT '文章短链接标识',
    article_summary VARCHAR(500) NULL COMMENT '文章摘要',
    cover_url VARCHAR(255) NULL COMMENT '封面图地址',
    article_content LONGTEXT NOT NULL COMMENT '文章内容',
    author_id BIGINT NOT NULL COMMENT '作者用户ID',
    category_id BIGINT NULL COMMENT '分类ID',
    article_status ENUM('DRAFT', 'PUBLISHED', 'PRIVATE') NOT NULL DEFAULT 'DRAFT' COMMENT '文章状态',
    top_flag TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶，0否，1是',
    allow_comment TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否允许评论，0否，1是',
    view_count INT NOT NULL DEFAULT 0 COMMENT '浏览量',
    like_count INT NOT NULL DEFAULT 0 COMMENT '点赞量',
    published_time DATETIME NULL COMMENT '发布时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除，0未删除，1已删除',
    UNIQUE KEY uk_blog_article_article_slug (article_slug),
    KEY idx_blog_article_author_id (author_id),
    KEY idx_blog_article_category_id (category_id),
    KEY idx_blog_article_status_deleted (article_status, deleted),
    CONSTRAINT fk_blog_article_author_id FOREIGN KEY (author_id) REFERENCES sys_user (id),
    CONSTRAINT fk_blog_article_category_id FOREIGN KEY (category_id) REFERENCES blog_category (id)
        ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文章表';

CREATE TABLE blog_article_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    article_id BIGINT NOT NULL COMMENT '文章ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除，0未删除，1已删除',
    UNIQUE KEY uk_blog_article_tag_article_id_tag_id (article_id, tag_id),
    KEY idx_blog_article_tag_tag_id (tag_id),
    CONSTRAINT fk_blog_article_tag_article_id FOREIGN KEY (article_id) REFERENCES blog_article (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_blog_article_tag_tag_id FOREIGN KEY (tag_id) REFERENCES blog_tag (id)
        ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文章标签关联表';

CREATE TABLE blog_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    article_id BIGINT NOT NULL COMMENT '文章ID',
    parent_id BIGINT NULL COMMENT '父评论ID',
    user_id BIGINT NOT NULL COMMENT '评论用户ID',
    reply_to_user_id BIGINT NULL COMMENT '被回复用户ID',
    comment_content VARCHAR(500) NOT NULL COMMENT '评论内容',
    comment_status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'APPROVED' COMMENT '评论状态',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除，0未删除，1已删除',
    KEY idx_blog_comment_article_id (article_id),
    KEY idx_blog_comment_user_id (user_id),
    KEY idx_blog_comment_parent_id (parent_id),
    CONSTRAINT fk_blog_comment_article_id FOREIGN KEY (article_id) REFERENCES blog_article (id),
    CONSTRAINT fk_blog_comment_parent_id FOREIGN KEY (parent_id) REFERENCES blog_comment (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_blog_comment_user_id FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_blog_comment_reply_to_user_id FOREIGN KEY (reply_to_user_id) REFERENCES sys_user (id)
        ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '评论表';

CREATE TABLE blog_friend_link (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    site_name VARCHAR(100) NOT NULL COMMENT '站点名称',
    site_url VARCHAR(255) NOT NULL COMMENT '站点地址',
    site_logo VARCHAR(255) NULL COMMENT '站点Logo地址',
    site_desc VARCHAR(255) NULL COMMENT '站点描述',
    owner_name VARCHAR(50) NULL COMMENT '站长名称',
    contact_email VARCHAR(100) NULL COMMENT '联系邮箱',
    link_status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING' COMMENT '友链审核状态',
    created_by BIGINT NULL COMMENT '创建人用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除，0未删除，1已删除',
    UNIQUE KEY uk_blog_friend_link_site_url (site_url),
    KEY idx_blog_friend_link_created_by (created_by),
    CONSTRAINT fk_blog_friend_link_created_by FOREIGN KEY (created_by) REFERENCES sys_user (id)
        ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '友情链接表';

CREATE TABLE sys_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    operator_user_id BIGINT NOT NULL COMMENT '操作人用户ID',
    target_type VARCHAR(30) NOT NULL COMMENT '目标对象类型',
    target_id BIGINT NOT NULL COMMENT '目标对象ID',
    action_type VARCHAR(30) NOT NULL COMMENT '操作类型',
    action_result ENUM('SUCCESS', 'FAILED') NOT NULL DEFAULT 'SUCCESS' COMMENT '操作结果',
    action_detail VARCHAR(255) NOT NULL COMMENT '操作详情',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除，0未删除，1已删除',
    KEY idx_sys_operation_log_operator_time (operator_user_id, create_time),
    CONSTRAINT fk_sys_operation_log_operator_user_id FOREIGN KEY (operator_user_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统操作日志表';

INSERT INTO sys_role (id, role_code, role_name, role_rank, remark, deleted) VALUES
(1, 'SUPER_ADMIN', '超级管理员', 1, '系统最高权限角色', 0),
(2, 'ADMIN', '管理员', 2, '可管理内容和普通用户', 0),
(3, 'USER', '普通用户', 3, '注册博客用户', 0);

INSERT INTO sys_user (
    id, user_name, password_hash, nick_name, email, phone, avatar_url, introduction, role_id, user_status, deleted
) VALUES
(1, 'root', SHA2('123456', 256), 'Root', 'root@blog.local', '13800000001', NULL, '初始超级管理员账号', 1, 'ENABLED', 0),
(2, 'admin_zhang', SHA2('123456', 256), 'Admin Zhang', 'admin_zhang@blog.local', '13800000002', NULL, '管理员账号A', 2, 'ENABLED', 0),
(3, 'admin_li', SHA2('123456', 256), 'Admin Li', 'admin_li@blog.local', '13800000003', NULL, '管理员账号B', 2, 'ENABLED', 0),
(4, 'tom', SHA2('123456', 256), 'Tom', 'tom@blog.local', '13800000004', NULL, '普通博客用户Tom', 3, 'ENABLED', 0),
(5, 'jerry', SHA2('123456', 256), 'Jerry', 'jerry@blog.local', '13800000005', NULL, '普通博客用户Jerry', 3, 'ENABLED', 0);

INSERT INTO blog_category (id, category_name, description, sort_no, created_by, deleted) VALUES
(1, 'Backend', 'Spring Boot 和 Java 相关文章分类', 1, 2, 0),
(2, 'Frontend', 'Vue 和 Web 页面相关文章分类', 2, 2, 0),
(3, 'Life', '日常学习与反思分类', 3, 4, 0);

INSERT INTO blog_tag (id, tag_name, description, created_by, deleted) VALUES
(1, 'SpringBoot', 'Spring Boot 项目标签', 2, 0),
(2, 'Vue', 'Vue 项目标签', 2, 0),
(3, 'MySQL', '数据库设计标签', 2, 0),
(4, 'Study', '学习记录标签', 4, 0);

INSERT INTO blog_article (
    id, article_title, article_slug, article_summary, cover_url, article_content, author_id, category_id,
    article_status, top_flag, allow_comment, view_count, like_count, published_time, deleted
) VALUES
(1, 'Build a Personal Blog with Spring Boot', 'build-personal-blog-with-spring-boot',
 '一篇关于个人博客项目后端实践的示例文章。', NULL,
 '本文介绍如何为个人博客系统设计一个简洁的 Spring Boot 后端。', 2, 1,
 'PUBLISHED', 1, 1, 120, 15, '2026-03-20 10:00:00', 0),
(2, 'Vue Frontend Notes for Blog Project', 'vue-frontend-notes-for-blog-project',
 '一篇关于 Vue 页面规划和组件拆分的学习笔记。', NULL,
 '本文记录博客系统前端页面设计、路由与组件结构思路。', 4, 2,
 'PUBLISHED', 0, 1, 58, 8, '2026-03-20 20:30:00', 0),
(3, 'My First Draft', 'my-first-draft',
 '一篇等待进一步打磨的草稿文章。', NULL,
 '这是一篇由普通用户创建的草稿文章。', 5, 3,
 'DRAFT', 0, 1, 0, 0, NULL, 0);

INSERT INTO blog_article_tag (id, article_id, tag_id, deleted) VALUES
(1, 1, 1, 0),
(2, 1, 3, 0),
(3, 2, 2, 0),
(4, 2, 4, 0),
(5, 3, 4, 0);

INSERT INTO blog_comment (
    id, article_id, parent_id, user_id, reply_to_user_id, comment_content, comment_status, deleted
) VALUES
(1, 1, NULL, 4, NULL, '这个后端设计对我的课程项目很有帮助。', 'APPROVED', 0),
(2, 1, 1, 2, 4, '继续加油，后面还可以扩展登录和权限控制。', 'APPROVED', 0),
(3, 2, NULL, 5, NULL, 'Vue 页面拆分思路很清晰，容易跟着实现。', 'APPROVED', 0);

INSERT INTO blog_friend_link (
    id, site_name, site_url, site_logo, site_desc, owner_name, contact_email, link_status, created_by, deleted
) VALUES
(1, 'Open Source Study Notes', 'https://example.com/study-notes', NULL,
 '一个用于 SQL 作业展示的示例友链。', 'Sample Owner', 'contact@example.com', 'APPROVED', 2, 0);

DELIMITER $$

CREATE TRIGGER trg_set_article_published_time_insert
BEFORE INSERT ON blog_article
FOR EACH ROW
BEGIN
    IF NEW.article_status = 'PUBLISHED' AND NEW.published_time IS NULL THEN
        SET NEW.published_time = CURRENT_TIMESTAMP;
    END IF;
END $$

CREATE TRIGGER trg_set_article_published_time_update
BEFORE UPDATE ON blog_article
FOR EACH ROW
BEGIN
    IF NEW.article_status = 'PUBLISHED' AND NEW.published_time IS NULL THEN
        SET NEW.published_time = CURRENT_TIMESTAMP;
    END IF;
END $$

CREATE TRIGGER trg_block_physical_delete_sys_user
BEFORE DELETE ON sys_user
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'Physical delete on sys_user is forbidden. Use sp_delete_user instead.';
END $$

CREATE TRIGGER trg_block_physical_delete_blog_article
BEFORE DELETE ON blog_article
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'Physical delete on blog_article is forbidden. Use sp_delete_article instead.';
END $$

CREATE TRIGGER trg_block_physical_delete_blog_comment
BEFORE DELETE ON blog_comment
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'Physical delete on blog_comment is forbidden. Use sp_delete_comment instead.';
END $$

CREATE PROCEDURE sp_delete_user (
    IN p_operator_user_id BIGINT,
    IN p_target_user_id BIGINT
)
proc_label: BEGIN
    DECLARE v_operator_role_code VARCHAR(30);
    DECLARE v_target_role_code VARCHAR(30);

    SELECT r.role_code
    INTO v_operator_role_code
    FROM sys_user u
    INNER JOIN sys_role r ON u.role_id = r.id
    WHERE u.id = p_operator_user_id
      AND u.deleted = 0
      AND u.user_status = 'ENABLED'
    LIMIT 1;

    IF v_operator_role_code IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Operator does not exist, is disabled, or has been deleted.';
    END IF;

    SELECT r.role_code
    INTO v_target_role_code
    FROM sys_user u
    INNER JOIN sys_role r ON u.role_id = r.id
    WHERE u.id = p_target_user_id
      AND u.deleted = 0
    LIMIT 1;

    IF v_target_role_code IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Target user does not exist or has already been deleted.';
    END IF;

    IF v_target_role_code = 'SUPER_ADMIN' THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Super administrator cannot be deleted.';
    END IF;

    IF v_operator_role_code = 'SUPER_ADMIN' THEN
        UPDATE sys_user
        SET deleted = 1,
            update_time = CURRENT_TIMESTAMP
        WHERE id = p_target_user_id;

    ELSEIF v_operator_role_code = 'ADMIN' THEN
        IF v_target_role_code <> 'USER' THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Administrator can only delete normal users.';
        END IF;

        UPDATE sys_user
        SET deleted = 1,
            update_time = CURRENT_TIMESTAMP
        WHERE id = p_target_user_id;

    ELSEIF v_operator_role_code = 'USER' THEN
        IF p_operator_user_id <> p_target_user_id THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Normal user can only delete own account.';
        END IF;

        IF v_target_role_code <> 'USER' THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Normal user cannot delete administrator accounts.';
        END IF;

        UPDATE sys_user
        SET deleted = 1,
            update_time = CURRENT_TIMESTAMP
        WHERE id = p_target_user_id;

    ELSE
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Unknown role, delete operation rejected.';
    END IF;

    INSERT INTO sys_operation_log (
        operator_user_id, target_type, target_id, action_type, action_result, action_detail
    ) VALUES (
        p_operator_user_id, 'USER', p_target_user_id, 'LOGIC_DELETE', 'SUCCESS',
        CONCAT('Delete user success. operator_role=', v_operator_role_code, ', target_role=', v_target_role_code)
    );
END $$

CREATE PROCEDURE sp_delete_article (
    IN p_operator_user_id BIGINT,
    IN p_article_id BIGINT
)
proc_label: BEGIN
    DECLARE v_operator_role_code VARCHAR(30);
    DECLARE v_article_author_id BIGINT;

    SELECT r.role_code
    INTO v_operator_role_code
    FROM sys_user u
    INNER JOIN sys_role r ON u.role_id = r.id
    WHERE u.id = p_operator_user_id
      AND u.deleted = 0
      AND u.user_status = 'ENABLED'
    LIMIT 1;

    IF v_operator_role_code IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Operator does not exist, is disabled, or has been deleted.';
    END IF;

    SELECT author_id
    INTO v_article_author_id
    FROM blog_article
    WHERE id = p_article_id
      AND deleted = 0
    LIMIT 1;

    IF v_article_author_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Article does not exist or has already been deleted.';
    END IF;

    IF v_operator_role_code = 'USER' AND v_article_author_id <> p_operator_user_id THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Normal user can only delete own article.';
    END IF;

    UPDATE blog_article
    SET deleted = 1,
        update_time = CURRENT_TIMESTAMP
    WHERE id = p_article_id;

    INSERT INTO sys_operation_log (
        operator_user_id, target_type, target_id, action_type, action_result, action_detail
    ) VALUES (
        p_operator_user_id, 'ARTICLE', p_article_id, 'LOGIC_DELETE', 'SUCCESS',
        CONCAT('Delete article success. operator_role=', v_operator_role_code)
    );
END $$

CREATE PROCEDURE sp_delete_comment (
    IN p_operator_user_id BIGINT,
    IN p_comment_id BIGINT
)
proc_label: BEGIN
    DECLARE v_operator_role_code VARCHAR(30);
    DECLARE v_comment_user_id BIGINT;

    SELECT r.role_code
    INTO v_operator_role_code
    FROM sys_user u
    INNER JOIN sys_role r ON u.role_id = r.id
    WHERE u.id = p_operator_user_id
      AND u.deleted = 0
      AND u.user_status = 'ENABLED'
    LIMIT 1;

    IF v_operator_role_code IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Operator does not exist, is disabled, or has been deleted.';
    END IF;

    SELECT user_id
    INTO v_comment_user_id
    FROM blog_comment
    WHERE id = p_comment_id
      AND deleted = 0
    LIMIT 1;

    IF v_comment_user_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Comment does not exist or has already been deleted.';
    END IF;

    IF v_operator_role_code = 'USER' AND v_comment_user_id <> p_operator_user_id THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Normal user can only delete own comment.';
    END IF;

    UPDATE blog_comment
    SET deleted = 1,
        update_time = CURRENT_TIMESTAMP
    WHERE id = p_comment_id;

    INSERT INTO sys_operation_log (
        operator_user_id, target_type, target_id, action_type, action_result, action_detail
    ) VALUES (
        p_operator_user_id, 'COMMENT', p_comment_id, 'LOGIC_DELETE', 'SUCCESS',
        CONCAT('Delete comment success. operator_role=', v_operator_role_code)
    );
END $$

DELIMITER ;

SET FOREIGN_KEY_CHECKS = 1;
