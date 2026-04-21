package com.edusmart.server.service;

import com.edusmart.server.common.BusinessException;
import com.edusmart.server.dto.auth.LoginRequest;
import com.edusmart.server.dto.auth.RegisterRequest;
import com.edusmart.server.dto.auth.UpdateUserRequest;
import com.edusmart.server.dto.auth.UploadAvatarRequest;
import com.edusmart.server.dto.auth.UserDto;
import com.edusmart.server.entity.AuthToken;
import com.edusmart.server.entity.User;
import com.edusmart.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

/**
 * 用户认证 & 个人资料业务逻辑。
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final FileStorageService fileStorageService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository,
                       TokenService tokenService,
                       FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public UserDto register(RegisterRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw BusinessException.badRequest("邮箱已被注册");
        }
        long now = System.currentTimeMillis();
        String userId = "user_" + now + "_" + randomSuffix();
        User user = User.builder()
                .userId(userId)
                .username(req.getUsername().trim())
                .email(email)
                .password(encoder.encode(req.getPassword()))
                .avatarUrl(null)
                .createdAt(now)
                .updatedAt(now)
                .build();
        userRepository.save(user);
        AuthToken token = tokenService.issue(userId);
        log.info("✅ 用户注册成功: {} ({})", userId, email);
        return UserDto.of(user, token.getToken());
    }

    @Transactional
    public UserDto login(LoginRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> BusinessException.badRequest("邮箱或密码错误"));
        if (!encoder.matches(req.getPassword(), user.getPassword())) {
            throw BusinessException.badRequest("邮箱或密码错误");
        }
        AuthToken token = tokenService.issue(user.getUserId());
        log.info("✅ 用户登录成功: {}", user.getUserId());
        return UserDto.of(user, token.getToken());
    }

    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            tokenService.revoke(token);
        }
    }

    public UserDto getUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        return UserDto.of(user);
    }

    @Transactional
    public UserDto updateUser(String userId, UpdateUserRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        boolean dirty = false;
        if (req.getUsername() != null && !req.getUsername().isBlank()) {
            user.setUsername(req.getUsername().trim());
            dirty = true;
        }
        if (req.getAvatarUrl() != null) {
            user.setAvatarUrl(req.getAvatarUrl());
            dirty = true;
        }
        if (dirty) {
            user.setUpdatedAt(System.currentTimeMillis());
            userRepository.save(user);
        }
        return UserDto.of(user);
    }

    @Transactional
    public AvatarUploadResult uploadAvatar(String userId, UploadAvatarRequest req) {
        String base64 = req.resolveImage();
        if (base64 == null || base64.isBlank()) {
            throw BusinessException.badRequest("缺少图片数据");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        FileStorageService.StoredFile stored = fileStorageService.saveBase64(
                "avatars",
                userId,
                base64,
                req.getFileName() == null ? "avatar.jpg" : req.getFileName()
        );
        user.setAvatarUrl(stored.url());
        user.setUpdatedAt(System.currentTimeMillis());
        userRepository.save(user);
        return new AvatarUploadResult(stored.url(), stored.cloudPath());
    }

    private static String randomSuffix() {
        byte[] buf = new byte[5];
        RANDOM.nextBytes(buf);
        StringBuilder sb = new StringBuilder();
        for (byte b : buf) {
            sb.append(Character.forDigit(((b >> 4) & 0x0F), 16));
            sb.append(Character.forDigit((b & 0x0F), 16));
        }
        return sb.toString().substring(0, 9);
    }

    public record AvatarUploadResult(String url, String cloudPath) {}
}
