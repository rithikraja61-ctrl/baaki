package com.baaki.controller;

import com.baaki.dto.AuthResponse;
import com.baaki.dto.GoogleLoginReq;
import com.baaki.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/config")
    public Map<String, String> config() {
        return Map.of("googleClientId", authService.getGoogleClientId());
    }

    @PostMapping("/google")
    public AuthResponse google(@RequestBody GoogleLoginReq req) {
        return authService.loginWithGoogle(req.credential());
    }

    @PostMapping("/demo")
    public AuthResponse demo() {
        return authService.loginAsDemoUser();
    }

    @GetMapping("/me")
    public AuthResponse.UserView me(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7).trim();
            return authService.authenticateToken(token)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authorization token");
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7).trim();
            authService.logout(token);
        }
        return Map.of("ok", true);
    }
}
