package org.example.personalblogsystem.service;

import org.example.personalblogsystem.dto.LoginRequest;
import org.example.personalblogsystem.dto.LoginUserResponse;
import org.example.personalblogsystem.dto.SysUserResponse;
import org.example.personalblogsystem.dto.UserRegisterRequest;

/**
 * 认证服务接口，定义登录、退出和用户注册的核心业务契约。
 * 由实现类对接 Spring Security、JWT、Redis 登录态和操作日志记录。
 */
public interface IAuthService {

    /**
     * 执行后台登录流程。
     *
     * @param request 登录请求，包含用户名和密码
     * @return 登录用户资料、访问令牌和过期时间
     */
    LoginUserResponse login(LoginRequest request);

    /**
     * 执行用户端登录流程，仅允许 USER 角色登录。
     *
     * @param request 登录请求，包含用户名和密码
     * @return 登录用户资料、访问令牌和过期时间
     */
    LoginUserResponse loginUser(LoginRequest request);

    /**
     * 清理当前登录主体的服务端登录态。
     */
    void logout();

    /**
     * 注册普通用户账号。
     *
     * @param request 用户注册请求
     * @return 去除密码散列后的用户资料
     */
    SysUserResponse register(UserRegisterRequest request);
}
