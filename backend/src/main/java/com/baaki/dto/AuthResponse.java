package com.baaki.dto;

public record AuthResponse(
    String token,
    UserView user
) {
    public record UserView(
        Long id,
        String email,
        String name,
        String picture,
        String role
    ) {}
}
