package com.baaki.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_tokens")
public class UserToken {
    @Id
    @Column(length = 128)
    public String token;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @Column(nullable = false)
    public LocalDateTime expiresAt;

    @Column(nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    public UserToken() {}

    public UserToken(String token, User user, LocalDateTime expiresAt) {
        this.token = token;
        this.user = user;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }
}
