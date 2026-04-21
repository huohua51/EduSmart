package com.edusmart.server.dto.auth;

import com.edusmart.server.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private String userId;
    private String username;
    private String email;
    private String avatarUrl;
    private Long createdAt;
    private Long updatedAt;
    /** 仅在登录 / 注册 / 刷新场景下返回。 */
    private String token;

    public static UserDto of(User user) {
        return UserDto.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public static UserDto of(User user, String token) {
        UserDto dto = of(user);
        dto.setToken(token);
        return dto;
    }
}
