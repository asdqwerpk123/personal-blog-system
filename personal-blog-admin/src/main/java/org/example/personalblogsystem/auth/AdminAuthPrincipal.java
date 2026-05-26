package org.example.personalblogsystem.auth;

import lombok.Value;

/**
 * JWT 解析后得到的登录主体值对象，保存鉴权和操作日志所需的用户、角色信息。
 * 使用 Lombok @Value 生成不可变对象，避免请求处理中主体信息被修改。
 */
@Value
public class AdminAuthPrincipal {

    Long userId;
    String userName;
    Long roleId;
    String roleCode;
}
