package com.edusmart.server.controller;

import com.edusmart.server.common.ApiResponse;
import com.edusmart.server.dto.auth.LoginRequest;
import com.edusmart.server.dto.auth.RegisterRequest;
import com.edusmart.server.dto.auth.UpdateUserRequest;
import com.edusmart.server.dto.auth.UploadAvatarRequest;
import com.edusmart.server.dto.auth.UserDto;
import com.edusmart.server.security.CurrentUserContext;
import com.edusmart.server.security.TokenAuthInterceptor;
import com.edusmart.server.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 登录 / 注册 / 当前用户资料 / 头像上传。
 *
 * <p>登录注册走白名单放行；其他路由由 {@link com.edusmart.server.security.TokenAuthInterceptor} 校验。</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<UserDto> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("注册成功", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<UserDto> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout(CurrentUserContext.token());
        return ApiResponse.success("已登出", null);
    }

    @GetMapping("/me")
    public ApiResponse<UserDto> me() {
        return ApiResponse.success(authService.getUser(TokenAuthInterceptor.requireUserId()));
    }

    @PutMapping("/me")
    public ApiResponse<UserDto> updateMe(@Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success("更新成功",
                authService.updateUser(TokenAuthInterceptor.requireUserId(), request));
    }

    @PostMapping("/avatar")
    public ApiResponse<Map<String, Object>> uploadAvatar(@RequestBody UploadAvatarRequest request) {
        AuthService.AvatarUploadResult result = authService.uploadAvatar(
                TokenAuthInterceptor.requireUserId(), request);
        return ApiResponse.success("上传成功", Map.of(
                "url", result.url(),
                "cloudPath", result.cloudPath(),
                // 兼容 CloudBase 旧字段
                "fileId", result.cloudPath()
        ));
    }
}
