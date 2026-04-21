package com.edusmart.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录 Token，保持与原 cloudbase-auth 中 <code>tokens</code> 集合一致的语义：
 * 每次登录/注册生成一条新记录，到期后失效。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "auth_tokens",
        indexes = {
                @Index(name = "idx_auth_tokens_token", columnList = "token", unique = true),
                @Index(name = "idx_auth_tokens_user", columnList = "userId")
        }
)
public class AuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, nullable = false, unique = true)
    private String token;

    @Column(length = 64, nullable = false)
    private String userId;

    @Column(nullable = false)
    private Long createdAt;

    @Column(nullable = false)
    private Long expiresAt;
}
