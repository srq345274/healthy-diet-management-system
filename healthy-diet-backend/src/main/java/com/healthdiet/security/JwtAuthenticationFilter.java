package com.healthdiet.security;

import com.healthdiet.entity.User;
import com.healthdiet.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.AntPathMatcher;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String ATTR_USER_ID = "currentUserId";

    @Autowired
    private UserRepository userRepository;

    private final AntPathMatcher matcher = new AntPathMatcher();

    // 白名单接口（和前端 URL 完全对应）
    private static final List<String> WHITELIST = List.of(
            "/api/email/send-code",
            "/api/email/check-email-exists",
            "/api/auth/email-register",
            "/api/auth/email-pwd-login",
            "/api/auth/email-code-login",
            "/api/auth/email-reset-pwd",
            "/api/user/info",
            "/api/user/update-profile"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // 放行 OPTIONS 请求
        if ("OPTIONS".equalsIgnoreCase(method)) {
            chain.doFilter(request, response);
            return;
        }

        // 白名单放行
        for (String pattern : WHITELIST) {
            if (matcher.match(pattern, path)) {
                chain.doFilter(request, response);
                return;
            }
        }

        // 从数据库 token 校验
        String token = request.getHeader("token"); // 使用前端存储的 token
        if (token == null || token.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或 Token 无效\"}");
            return;
        }

        User user = userRepository.findByToken(token).orElse(null);
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token 无效或已过期\"}");
            return;
        }

        // 设置 userId 给后续 Controller 使用
        request.setAttribute(ATTR_USER_ID, user.getId());

        chain.doFilter(request, response);
    }
}