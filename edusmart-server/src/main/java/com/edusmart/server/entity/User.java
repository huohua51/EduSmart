package com.edusmart.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email", unique = true)
        }
)
public class User {

    /** 用户 ID，格式 <code>user_&lt;ts&gt;_&lt;rand&gt;</code>。 */
    @Id
    @Column(length = 64, nullable = false, updatable = false)
    private String userId;

    @Column(length = 64, nullable = false)
    private String username;

    @Column(length = 128, nullable = false, unique = true)
    private String email;

    /** BCrypt 哈希值。 */
    @Column(length = 128, nullable = false)
    private String password;

    /** 头像 URL（可能是站内静态资源路径，也可能是 data: 形式的备用方案）。 */
    @Column(length = 2048)
    private String avatarUrl;

    @Column(nullable = false)
    private Long createdAt;

    @Column(nullable = false)
    private Long updatedAt;
}
