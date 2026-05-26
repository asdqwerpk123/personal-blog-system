package org.example.personalblogsystem.filter;

import org.example.personalblogsystem.auth.JwtTokenService;
import org.example.personalblogsystem.utils.JwtUtil;
import org.example.personalblogsystem.utils.RedisCache;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * JWT 认证过滤器的兼容类名，保留旧代码中对 JwtAuthenticationTokenFilter 的注入和引用。
 * 实际认证逻辑全部继承自 JwtAuthenticationFilter。
 */
public class JwtAuthenticationTokenFilter extends JwtAuthenticationFilter {

    /**
     * 创建兼容过滤器实例，并把核心依赖传递给父类认证过滤器。
     *
     * @param jwtTokenService 后台 JWT 签发和解析服务
     * @param redisCache Redis 登录态缓存组件
     * @param jwtUtil 课程登录令牌工具类
     * @param userDetailsService Spring Security 用户加载服务
     */
    public JwtAuthenticationTokenFilter(JwtTokenService jwtTokenService,
                                        RedisCache redisCache,
                                        JwtUtil jwtUtil,
                                        UserDetailsService userDetailsService) {
        super(jwtTokenService, redisCache, jwtUtil, userDetailsService);
    }
}
