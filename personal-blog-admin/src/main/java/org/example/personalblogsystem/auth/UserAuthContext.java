package org.example.personalblogsystem.auth;

import org.example.personalblogcommon.exception.BlogException;
import org.example.personalblogcommon.result.ResultCodeEnum;

/**
 * 用户端当前登录主体上下文，使用 ThreadLocal 保存一次请求内的普通用户身份。
 * 与 AdminAuthContext 分离，避免管理端和用户端权限语义混用。
 */
public final class UserAuthContext {

    /**
     * 当前线程绑定的用户端登录主体，请求结束必须清理。
     */
    private static final ThreadLocal<AdminAuthPrincipal> CURRENT_PRINCIPAL = new ThreadLocal<>();

    private UserAuthContext() {
    }

    /**
     * 设置当前线程的用户端登录主体。
     *
     * @param principal 解析 JWT 后得到的主体；为 null 时清理上下文
     */
    public static void set(AdminAuthPrincipal principal) {
        if (principal == null) {
            CURRENT_PRINCIPAL.remove();
            return;
        }
        CURRENT_PRINCIPAL.set(principal);
    }

    /**
     * 获取当前线程的用户端登录主体。
     *
     * @return 当前主体；未认证时返回 null
     */
    public static AdminAuthPrincipal get() {
        return CURRENT_PRINCIPAL.get();
    }

    /**
     * 获取当前线程的用户端登录主体，未登录时直接抛出业务异常。
     *
     * @return 当前已认证主体
     * @throws BlogException 当前线程没有登录主体时抛出未认证异常
     */
    public static AdminAuthPrincipal requireCurrentUser() {
        AdminAuthPrincipal current = CURRENT_PRINCIPAL.get();
        if (current == null) {
            throw new BlogException(ResultCodeEnum.UNAUTHORIZED);
        }
        return current;
    }

    /**
     * 清理当前线程绑定的用户端身份。
     */
    public static void clear() {
        CURRENT_PRINCIPAL.remove();
    }
}
