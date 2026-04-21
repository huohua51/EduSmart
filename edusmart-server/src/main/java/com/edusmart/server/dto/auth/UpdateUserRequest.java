package com.edusmart.server.dto.auth;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(max = 32, message = "用户名长度应 ≤ 32 位")
    private String username;

    @Size(max = 2048)
    private String avatarUrl;
}
