package org.example.personalblogsystem.auth;

import org.example.personalblogcommon.exception.BlogException;
import org.example.personalblogcommon.result.ResultCodeEnum;

public final class AdminAuthContext {

    private static final ThreadLocal<AdminAuthPrincipal> CURRENT_PRINCIPAL = new ThreadLocal<>();

    private AdminAuthContext() {
    }

    public static void set(AdminAuthPrincipal principal) {
        if (principal == null) {
            CURRENT_PRINCIPAL.remove();
            return;
        }
        CURRENT_PRINCIPAL.set(principal);
    }

    public static AdminAuthPrincipal get() {
        return CURRENT_PRINCIPAL.get();
    }

    public static AdminAuthPrincipal requireCurrentUser() {
        AdminAuthPrincipal current = CURRENT_PRINCIPAL.get();
        if (current == null) {
            throw new BlogException(ResultCodeEnum.UNAUTHORIZED);
        }
        return current;
    }

    public static void clear() {
        CURRENT_PRINCIPAL.remove();
    }
}
