package com.healthdiet.util;

import com.healthdiet.security.JwtAuthenticationFilter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

public final class RequestUser {

    private RequestUser() {
    }

    public static Long currentUserId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest req = attrs.getRequest();
        Object v = req.getAttribute(JwtAuthenticationFilter.ATTR_USER_ID);
        if (v instanceof Long) {
            return (Long) v;
        }
        return null;
    }
}
