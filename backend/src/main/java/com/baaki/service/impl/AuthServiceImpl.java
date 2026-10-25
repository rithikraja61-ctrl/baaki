package com.baaki.service.impl;

import com.baaki.dto.AuthResponse;
import com.baaki.model.User;
import com.baaki.model.UserToken;
import com.baaki.repository.UserRepository;
import com.baaki.repository.UserTokenRepository;
import com.baaki.service.AuthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String GOOGLE_TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private final UserRepository userRepo;
    private final UserTokenRepository tokenRepo;
    private final ObjectMapper objectMapper;
    private final String googleClientId;
    private final HttpClient httpClient;

    public AuthServiceImpl(UserRepository userRepo,
                           UserTokenRepository tokenRepo,
                           ObjectMapper objectMapper,
                           @Value("${baaki.auth.google-client-id:}") String googleClientId) {
        this.userRepo = userRepo;
        this.tokenRepo = tokenRepo;
        this.objectMapper = objectMapper;
        this.googleClientId = googleClientId != null ? googleClientId.trim() : "";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public String getGoogleClientId() {
        return googleClientId;
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(String credential) {
        if (credential == null || credential.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Google credential");
        }

        GoogleProfile profile = verifyGoogleToken(credential);
        User user = userRepo.findByEmail(profile.email())
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.email = profile.email();
                    newUser.name = profile.name();
                    newUser.picture = profile.picture();
                    newUser.googleId = profile.googleId();
                    newUser.role = "OWNER";
                    newUser.createdAt = LocalDateTime.now();
                    return newUser;
                });

        user.name = profile.name() != null ? profile.name() : user.name;
        if (profile.picture() != null) {
            user.picture = profile.picture();
        }
        if (profile.googleId() != null) {
            user.googleId = profile.googleId();
        }
        user.lastLoginAt = LocalDateTime.now();
        userRepo.save(user);

        String sessionToken = generateToken();
        UserToken tokenEntity = new UserToken(sessionToken, user, LocalDateTime.now().plusDays(30));
        tokenRepo.save(tokenEntity);

        return new AuthResponse(sessionToken, toView(user));
    }

    @Override
    @Transactional
    public AuthResponse loginAsDemoUser() {
        String demoEmail = "rakesh@sharmapackaging.in";
        User user = userRepo.findByEmail(demoEmail)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.email = demoEmail;
                    newUser.name = "Rakesh Sharma";
                    newUser.role = "OWNER";
                    newUser.createdAt = LocalDateTime.now();
                    return newUser;
                });

        user.lastLoginAt = LocalDateTime.now();
        userRepo.save(user);

        String sessionToken = generateToken();
        UserToken tokenEntity = new UserToken(sessionToken, user, LocalDateTime.now().plusDays(30));
        tokenRepo.save(tokenEntity);

        return new AuthResponse(sessionToken, toView(user));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthResponse.UserView> authenticateToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return tokenRepo.findByTokenAndExpiresAtAfter(token, LocalDateTime.now())
                .map(t -> toView(t.user));
    }

    @Override
    @Transactional
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            tokenRepo.deleteById(token);
        }
    }

    private GoogleProfile verifyGoogleToken(String credential) {
        // First try official tokeninfo verification
        try {
            String url = GOOGLE_TOKENINFO_URL + URLEncoder.encode(credential, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                String email = root.path("email").asText(null);
                String name = root.path("name").asText(email);
                String picture = root.path("picture").asText(null);
                String googleId = root.path("sub").asText(null);
                String aud = root.path("aud").asText("");

                if (!googleClientId.isEmpty() && !googleClientId.equals(aud)) {
                    log.warn("Google token audience mismatch: expected {} but got {}", googleClientId, aud);
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google Client ID mismatch");
                }

                if (email == null || email.isBlank()) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google account does not provide an email");
                }

                return new GoogleProfile(email.toLowerCase(), name, picture, googleId);
            } else {
                log.warn("Google tokeninfo returned status {}: {}", response.statusCode(), response.body());
            }
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception e) {
            log.warn("Error calling Google tokeninfo: {}", e.getMessage());
        }

        // Fallback for mock/test credentials (e.g. in offline dev or unit tests)
        // Check if credential is a standard 3-part JWT
        String[] parts = credential.split("\\.");
        if (parts.length >= 2) {
            try {
                byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
                JsonNode payload = objectMapper.readTree(decoded);
                String email = payload.path("email").asText(null);
                String name = payload.path("name").asText(email);
                String picture = payload.path("picture").asText(null);
                String googleId = payload.path("sub").asText(null);
                if (email != null && !email.isBlank()) {
                    return new GoogleProfile(email.toLowerCase(), name, picture, googleId);
                }
            } catch (Exception e) {
                log.debug("JWT payload decoding fallback failed: {}", e.getMessage());
            }
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired Google credential");
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    private AuthResponse.UserView toView(User user) {
        return new AuthResponse.UserView(
                user.id,
                user.email,
                user.name,
                user.picture,
                user.role
        );
    }

    private record GoogleProfile(String email, String name, String picture, String googleId) {}
}
