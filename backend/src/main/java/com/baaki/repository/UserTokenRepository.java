package com.baaki.repository;

import com.baaki.model.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserTokenRepository extends JpaRepository<UserToken, String> {
    Optional<UserToken> findByTokenAndExpiresAtAfter(String token, LocalDateTime now);

    @Transactional
    @Modifying
    @Query("DELETE FROM UserToken t WHERE t.expiresAt < :now")
    void deleteExpired(LocalDateTime now);
}
