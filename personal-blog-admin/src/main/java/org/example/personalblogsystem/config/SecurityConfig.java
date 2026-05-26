package org.example.personalblogsystem.config;

import org.example.personalblogsystem.filter.JwtAuthenticationFilter;
import org.example.personalblogsystem.handler.AccessDeniedHandlerImpl;
import org.example.personalblogsystem.handler.AuthenticationEntryPointImpl;
import org.example.personalblogsystem.service.PasswordHashService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 核心配置类，负责组装无状态 JWT 认证链、接口访问规则和统一异常处理。
 * 依赖自定义 JWT 过滤器、认证入口和权限拒绝处理器完成前后端分离场景下的安全控制。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * 超级管理员权限标识，对应 Spring Security 授权表达式中的 authority。
     */
    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    /**
     * 管理员权限标识，用于访问后台管理接口。
     */
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    /**
     * 普通用户权限标识，用于访问用户端接口。
     */
    private static final String ROLE_USER = "ROLE_USER";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationEntryPointImpl authenticationEntryPoint;
    private final AccessDeniedHandlerImpl accessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          AuthenticationEntryPointImpl authenticationEntryPoint,
                          AccessDeniedHandlerImpl accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    /**
     * 构建系统主安全过滤链。
     *
     * @param http Spring Security 提供的 HTTP 安全配置入口
     * @return 完成路径授权、异常处理和 JWT 过滤器注册后的安全过滤链
     * @throws Exception 安全链构建失败时由 Spring Security 抛出
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/login",
                                "/admin/auth/login",
                                "/user/auth/login",
                                "/user/auth/register").permitAll()
                        .requestMatchers(
                                "/public/**",
                                "/uploads/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/druid/**",
                                "/error").permitAll()
                        .requestMatchers("/user/info").authenticated()
                        .requestMatchers("/admin/list").hasAnyAuthority(ROLE_SUPER_ADMIN, ROLE_ADMIN)
                        .requestMatchers("/admin/**").hasAnyAuthority(ROLE_SUPER_ADMIN, ROLE_ADMIN)
                        .requestMatchers("/user/**").hasAuthority(ROLE_USER)
                        .anyRequest().authenticated());

        /**
         * 将 JWT 过滤器放在用户名密码认证过滤器之前，保证每次请求先尝试从 Bearer Token 恢复登录态。
         */
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 注册基于数据库用户信息的认证管理器。
     *
     * @param userDetailsService Spring Security 加载用户和角色信息的服务
     * @param passwordEncoder 兼容 BCrypt 与历史密码格式的密码编码器
     * @return 供登录流程调用的 AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(authenticationProvider);
    }

    /**
     * 提供 BCrypt 原生编码器，供密码服务生成新密码散列。
     *
     * @return BCryptPasswordEncoder 实例
     */
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 暴露 Spring Security 使用的密码编码器。
     *
     * @param passwordHashService 统一封装新旧密码散列兼容逻辑的服务
     * @return Spring Security 登录认证时使用的 PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder(PasswordHashService passwordHashService) {
        return new PasswordEncoder() {
            /**
             * 对新密码执行系统当前的安全散列策略。
             *
             * @param rawPassword 明文密码
             * @return 可存入 password_hash 字段的散列值
             */
            @Override
            public String encode(CharSequence rawPassword) {
                return passwordHashService.hash(rawPassword == null ? null : rawPassword.toString());
            }

            /**
             * 校验登录密码是否匹配数据库中的散列值。
             *
             * @param rawPassword 用户提交的明文密码
             * @param encodedPassword 数据库保存的密码散列
             * @return 匹配成功返回 true，否则返回 false
             */
            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return rawPassword != null && passwordHashService.matches(rawPassword.toString(), encodedPassword);
            }
        };
    }
}
