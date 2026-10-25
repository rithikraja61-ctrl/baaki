package com.baaki.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true)
    public String email;

    @Column(nullable = false)
    public String name;

    @Column(length = 1024)
    public String picture;

    public String googleId;

    @Column(nullable = false)
    public String role = "OWNER";

    @Column(nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    public LocalDateTime lastLoginAt = LocalDateTime.now();

    public User() {}

    public User(String email, String name, String picture, String googleId, String role) {
        this.email = email;
        this.name = name;
        this.picture = picture;
        this.googleId = googleId;
        this.role = role != null ? role : "OWNER";
        this.createdAt = LocalDateTime.now();
        this.lastLoginAt = LocalDateTime.now();
    }
}
