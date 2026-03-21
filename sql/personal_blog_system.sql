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
    role_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(30) NOT NULL,
    role_name VARCHAR(30) NOT NULL,
    role_rank TINYINT NOT NULL,
    remark VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_role_code (role_code),
    UNIQUE KEY uk_sys_role_rank (role_rank)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE sys_user (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NULL,
    avatar_url VARCHAR(255) NULL,
    introduction VARCHAR(255) NULL,
    role_id BIGINT NOT NULL,
    status ENUM('ENABLED', 'DISABLED') NOT NULL DEFAULT 'ENABLED',
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_user_username (username),
    UNIQUE KEY uk_sys_user_email (email),
    UNIQUE KEY uk_sys_user_phone (phone),
    KEY idx_sys_user_role_id (role_id),
    CONSTRAINT fk_sys_user_role FOREIGN KEY (role_id) REFERENCES sys_role (role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE blog_category (
    category_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_name VARCHAR(50) NOT NULL,
    description VARCHAR(255) NULL,
    sort_no INT NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_blog_category_name (category_name),
    KEY idx_blog_category_created_by (created_by),
    CONSTRAINT fk_blog_category_created_by FOREIGN KEY (created_by) REFERENCES sys_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE blog_tag (
    tag_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tag_name VARCHAR(50) NOT NULL,
    description VARCHAR(255) NULL,
    created_by BIGINT NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_blog_tag_name (tag_name),
    KEY idx_blog_tag_created_by (created_by),
    CONSTRAINT fk_blog_tag_created_by FOREIGN KEY (created_by) REFERENCES sys_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE blog_article (
    article_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(200) NOT NULL,
    summary VARCHAR(500) NULL,
    cover_url VARCHAR(255) NULL,
    content LONGTEXT NOT NULL,
    author_id BIGINT NOT NULL,
    category_id BIGINT NULL,
    status ENUM('DRAFT', 'PUBLISHED', 'PRIVATE') NOT NULL DEFAULT 'DRAFT',
    is_top TINYINT(1) NOT NULL DEFAULT 0,
    allow_comment TINYINT(1) NOT NULL DEFAULT 1,
    view_count INT NOT NULL DEFAULT 0,
    like_count INT NOT NULL DEFAULT 0,
    published_at DATETIME NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_blog_article_slug (slug),
    KEY idx_blog_article_author_id (author_id),
    KEY idx_blog_article_category_id (category_id),
    KEY idx_blog_article_status_deleted (status, deleted),
    CONSTRAINT fk_blog_article_author_id FOREIGN KEY (author_id) REFERENCES sys_user (user_id),
    CONSTRAINT fk_blog_article_category_id FOREIGN KEY (category_id) REFERENCES blog_category (category_id)
        ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE blog_article_tag (
    article_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (article_id, tag_id),
    KEY idx_blog_article_tag_tag_id (tag_id),
    CONSTRAINT fk_blog_article_tag_article_id FOREIGN KEY (article_id) REFERENCES blog_article (article_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_blog_article_tag_tag_id FOREIGN KEY (tag_id) REFERENCES blog_tag (tag_id)
        ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE blog_comment (
    comment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    article_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    user_id BIGINT NOT NULL,
    reply_to_user_id BIGINT NULL,
    content VARCHAR(500) NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'APPROVED',
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_blog_comment_article_id (article_id),
    KEY idx_blog_comment_user_id (user_id),
    KEY idx_blog_comment_parent_id (parent_id),
    CONSTRAINT fk_blog_comment_article_id FOREIGN KEY (article_id) REFERENCES blog_article (article_id),
    CONSTRAINT fk_blog_comment_parent_id FOREIGN KEY (parent_id) REFERENCES blog_comment (comment_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_blog_comment_user_id FOREIGN KEY (user_id) REFERENCES sys_user (user_id),
    CONSTRAINT fk_blog_comment_reply_to_user_id FOREIGN KEY (reply_to_user_id) REFERENCES sys_user (user_id)
        ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE blog_friend_link (
    link_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    site_name VARCHAR(100) NOT NULL,
    site_url VARCHAR(255) NOT NULL,
    site_logo VARCHAR(255) NULL,
    site_desc VARCHAR(255) NULL,
    owner_name VARCHAR(50) NULL,
    contact_email VARCHAR(100) NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    created_by BIGINT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_blog_friend_link_site_url (site_url),
    KEY idx_blog_friend_link_created_by (created_by),
    CONSTRAINT fk_blog_friend_link_created_by FOREIGN KEY (created_by) REFERENCES sys_user (user_id)
        ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE sys_operation_log (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator_user_id BIGINT NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,
    action_type VARCHAR(30) NOT NULL,
    action_result ENUM('SUCCESS', 'FAILED') NOT NULL DEFAULT 'SUCCESS',
    detail VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_sys_operation_log_operator_time (operator_user_id, created_at),
    CONSTRAINT fk_sys_operation_log_operator_user_id FOREIGN KEY (operator_user_id) REFERENCES sys_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO sys_role (role_id, role_code, role_name, role_rank, remark) VALUES
(1, 'SUPER_ADMIN', 'Super Administrator', 1, 'Highest permission in system'),
(2, 'ADMIN', 'Administrator', 2, 'Manage content and normal users'),
(3, 'USER', 'Normal User', 3, 'Registered blog user');

INSERT INTO sys_user (
    user_id, username, password_hash, nickname, email, phone, avatar_url, introduction, role_id, status, deleted
) VALUES
(1, 'root', SHA2('123456', 256), 'Root', 'root@blog.local', '13800000001', NULL, 'Initial super administrator', 1, 'ENABLED', 0),
(2, 'admin_zhang', SHA2('123456', 256), 'Admin Zhang', 'admin_zhang@blog.local', '13800000002', NULL, 'Administrator account A', 2, 'ENABLED', 0),
(3, 'admin_li', SHA2('123456', 256), 'Admin Li', 'admin_li@blog.local', '13800000003', NULL, 'Administrator account B', 2, 'ENABLED', 0),
(4, 'tom', SHA2('123456', 256), 'Tom', 'tom@blog.local', '13800000004', NULL, 'Normal blog user Tom', 3, 'ENABLED', 0),
(5, 'jerry', SHA2('123456', 256), 'Jerry', 'jerry@blog.local', '13800000005', NULL, 'Normal blog user Jerry', 3, 'ENABLED', 0);

INSERT INTO blog_category (category_id, category_name, description, sort_no, created_by, deleted) VALUES
(1, 'Backend', 'Spring Boot and Java related notes', 1, 2, 0),
(2, 'Frontend', 'Vue and web interface notes', 2, 2, 0),
(3, 'Life', 'Daily study and reflection', 3, 4, 0);

INSERT INTO blog_tag (tag_id, tag_name, description, created_by, deleted) VALUES
(1, 'SpringBoot', 'Spring Boot project tag', 2, 0),
(2, 'Vue', 'Vue project tag', 2, 0),
(3, 'MySQL', 'Database design tag', 2, 0),
(4, 'Study', 'Learning diary tag', 4, 0);

INSERT INTO blog_article (
    article_id, title, slug, summary, cover_url, content, author_id, category_id,
    status, is_top, allow_comment, view_count, like_count, published_at, deleted
) VALUES
(1, 'Build a Personal Blog with Spring Boot', 'build-personal-blog-with-spring-boot',
 'A simple backend practice article for the personal blog project.', NULL,
 'This article introduces how to design a simple Spring Boot backend for a personal blog system.',
 2, 1, 'PUBLISHED', 1, 1, 120, 15, '2026-03-20 10:00:00', 0),
(2, 'Vue Frontend Notes for Blog Project', 'vue-frontend-notes-for-blog-project',
 'A study note about Vue page planning and component splitting.', NULL,
 'This article records frontend page design ideas, routing, and component structure for the blog system.',
 4, 2, 'PUBLISHED', 0, 1, 58, 8, '2026-03-20 20:30:00', 0),
(3, 'My First Draft', 'my-first-draft',
 'A draft article waiting to be polished.', NULL,
 'This is a draft article created by a normal user.', 5, 3, 'DRAFT', 0, 1, 0, 0, NULL, 0);

INSERT INTO blog_article_tag (article_id, tag_id) VALUES
(1, 1),
(1, 3),
(2, 2),
(2, 4),
(3, 4);

INSERT INTO blog_comment (
    comment_id, article_id, parent_id, user_id, reply_to_user_id, content, status, deleted
) VALUES
(1, 1, NULL, 4, NULL, 'This backend design is very helpful for my class project.', 'APPROVED', 0),
(2, 1, 1, 2, 4, 'Keep going, you can extend it with login and permission control later.', 'APPROVED', 0),
(3, 2, NULL, 5, NULL, 'The Vue page split idea is clear and easy to follow.', 'APPROVED', 0);

INSERT INTO blog_friend_link (
    link_id, site_name, site_url, site_logo, site_desc, owner_name, contact_email, status, created_by, deleted
) VALUES
(1, 'Open Source Study Notes', 'https://example.com/study-notes', NULL,
 'A sample friend link for SQL homework demonstration.', 'Sample Owner', 'contact@example.com', 'APPROVED', 2, 0);

DELIMITER $$

CREATE TRIGGER trg_set_article_published_at_insert
BEFORE INSERT ON blog_article
FOR EACH ROW
BEGIN
    IF NEW.status = 'PUBLISHED' AND NEW.published_at IS NULL THEN
        SET NEW.published_at = CURRENT_TIMESTAMP;
    END IF;
END $$

CREATE TRIGGER trg_set_article_published_at_update
BEFORE UPDATE ON blog_article
FOR EACH ROW
BEGIN
    IF NEW.status = 'PUBLISHED' AND NEW.published_at IS NULL THEN
        SET NEW.published_at = CURRENT_TIMESTAMP;
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
    INNER JOIN sys_role r ON u.role_id = r.role_id
    WHERE u.user_id = p_operator_user_id
      AND u.deleted = 0
      AND u.status = 'ENABLED'
    LIMIT 1;

    IF v_operator_role_code IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Operator does not exist, is disabled, or has been deleted.';
    END IF;

    SELECT r.role_code
    INTO v_target_role_code
    FROM sys_user u
    INNER JOIN sys_role r ON u.role_id = r.role_id
    WHERE u.user_id = p_target_user_id
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
        SET deleted = 1
        WHERE user_id = p_target_user_id;

    ELSEIF v_operator_role_code = 'ADMIN' THEN
        IF v_target_role_code <> 'USER' THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Administrator can only delete normal users.';
        END IF;

        UPDATE sys_user
        SET deleted = 1
        WHERE user_id = p_target_user_id;

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
        SET deleted = 1
        WHERE user_id = p_target_user_id;

    ELSE
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Unknown role, delete operation rejected.';
    END IF;

    INSERT INTO sys_operation_log (
        operator_user_id, target_type, target_id, action_type, action_result, detail
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
    INNER JOIN sys_role r ON u.role_id = r.role_id
    WHERE u.user_id = p_operator_user_id
      AND u.deleted = 0
      AND u.status = 'ENABLED'
    LIMIT 1;

    IF v_operator_role_code IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Operator does not exist, is disabled, or has been deleted.';
    END IF;

    SELECT author_id
    INTO v_article_author_id
    FROM blog_article
    WHERE article_id = p_article_id
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
    SET deleted = 1
    WHERE article_id = p_article_id;

    INSERT INTO sys_operation_log (
        operator_user_id, target_type, target_id, action_type, action_result, detail
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
    INNER JOIN sys_role r ON u.role_id = r.role_id
    WHERE u.user_id = p_operator_user_id
      AND u.deleted = 0
      AND u.status = 'ENABLED'
    LIMIT 1;

    IF v_operator_role_code IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Operator does not exist, is disabled, or has been deleted.';
    END IF;

    SELECT user_id
    INTO v_comment_user_id
    FROM blog_comment
    WHERE comment_id = p_comment_id
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
    SET deleted = 1
    WHERE comment_id = p_comment_id;

    INSERT INTO sys_operation_log (
        operator_user_id, target_type, target_id, action_type, action_result, detail
    ) VALUES (
        p_operator_user_id, 'COMMENT', p_comment_id, 'LOGIC_DELETE', 'SUCCESS',
        CONCAT('Delete comment success. operator_role=', v_operator_role_code)
    );
END $$

DELIMITER ;

SET FOREIGN_KEY_CHECKS = 1;