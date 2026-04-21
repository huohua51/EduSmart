package com.edusmart.server.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度应为 6~64 位")
    private String password;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 1, max = 32, message = "用户名长度应为 1~32 位")
    private String username;
}
