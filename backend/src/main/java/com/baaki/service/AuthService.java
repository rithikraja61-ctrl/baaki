package com.baaki.service;

import com.baaki.dto.AuthResponse;
import java.util.Optional;

public interface AuthService {
    AuthResponse loginWithGoogle(String credential);
    AuthResponse loginAsDemoUser();
    Optional<AuthResponse.UserView> authenticateToken(String token);
    void logout(String token);
    String getGoogleClientId();
}
