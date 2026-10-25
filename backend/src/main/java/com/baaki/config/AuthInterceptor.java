package com.baaki.config;

import com.baaki.dto.AuthResponse;
import com.baaki.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/") ||
                path.equals("/api/config") ||
                path.equals("/api/invoices/scan/status") ||
                path.startsWith("/api/public/") ||
                path.startsWith("/api/gstin/") ||
                path.startsWith("/api/words") ||
                path.equals("/error")) {
            return true;
        }

        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7).trim();
            Optional<AuthResponse.UserView> user = authService.authenticateToken(token);
            if (user.isPresent()) {
                request.setAttribute("currentUser", user.get());
                return true;
            }
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Please sign in to continue\"}");
        return false;
    }
}
