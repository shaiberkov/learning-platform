package org.example.learningprojectserver.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

import static org.example.learningprojectserver.constants.SecurityConstants.JwtAuthenticationFilterConstants.*;

/**
 * Reads the caller set by JwtAuthenticationFilter. During impersonation the user and role are the
 * impersonated user's, and impersonatorId() names the system admin behind the session.
 */
public final class CurrentUser {

    private CurrentUser() {}

    public static String userId() {
        return principalValue(PRINCIPAL_USER_ID_KEY);
    }

    public static String username() {
        return principalValue(PRINCIPAL_USERNAME_KEY);
    }

    public static String impersonatorId() {
        return principalValue(PRINCIPAL_IMPERSONATOR_ID_KEY);
    }

    public static String impersonatorUsername() {
        return principalValue(PRINCIPAL_IMPERSONATOR_USERNAME_KEY);
    }

    /** The role without the ROLE_ prefix, or null for an anonymous caller. */
    public static String role() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            String name = authority.getAuthority();
            if (name != null && name.startsWith(ROLE_PREFIX)) {
                return name.substring(ROLE_PREFIX.length());
            }
        }
        return null;
    }

    private static String principalValue(String key) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Map<?, ?> principal)) {
            return null;
        }
        Object value = principal.get(key);
        return value == null ? null : value.toString();
    }
}
