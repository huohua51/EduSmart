package com.edusmart.server.repository;

import com.edusmart.server.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    Optional<AuthToken> findByToken(String token);

    @Modifying
    @Transactional
    void deleteByToken(String token);

    @Modifying
    @Transactional
    @Query("delete from AuthToken t where t.expiresAt < :now")
    int deleteExpired(long now);
}
