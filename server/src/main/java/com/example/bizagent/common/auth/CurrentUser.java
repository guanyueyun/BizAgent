package com.example.bizagent.common.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id() {
        return user().id();
    }

    public static AuthUser user() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUser user) {
            return user;
        }
        throw new SecurityException("用户未登录");
    }

    public static boolean hasPermission(String permissionCode) {
        AuthUser current = user();
        return Long.valueOf(1L).equals(current.id()) || current.permissions().contains(permissionCode);
    }

    public static void requirePermission(String permissionCode) {
        if (!hasPermission(permissionCode)) {
            throw new SecurityException("缺少权限: " + permissionCode);
        }
    }
}
