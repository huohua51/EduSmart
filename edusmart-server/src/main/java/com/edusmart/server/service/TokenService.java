package com.edusmart.server.service;

import com.edusmart.server.config.EdusmartProperties;
import com.edusmart.server.entity.AuthToken;
import com.edusmart.server.repository.AuthTokenRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.HexFormat;

@Service
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthTokenRepository repository;
    private final EdusmartProperties properties;

    public TokenService(AuthTokenRepository repository, EdusmartProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /** 生成并持久化一个 token。 */
    public AuthToken issue(String userId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        long now = System.currentTimeMillis();
        AuthToken entity = AuthToken.builder()
                .token(token)
                .userId(userId)
                .createdAt(now)
                .expiresAt(now + properties.getTokenTtlMs())
                .build();
        return repository.save(entity);
    }

    public void revoke(String token) {
        repository.deleteByToken(token);
    }
}
