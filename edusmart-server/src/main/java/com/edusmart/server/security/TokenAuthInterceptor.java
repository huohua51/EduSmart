package com.edusmart.server.security;

import com.edusmart.server.common.ApiResponse;
import com.edusmart.server.common.BusinessException;
import com.edusmart.server.entity.AuthToken;
import com.edusmart.server.repository.AuthTokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Bearer Token 拦截器。
 *
 * <ul>
 *   <li>从 <code>Authorization: Bearer xxx</code> 或 <code>?token=xxx</code> 读取 token</li>
 *   <li>查库校验 token 存在 & 未过期</li>
 *   <li>校验可选参数 <code>userId</code> 与 token 对应的用户一致（若请求体/路径提供）</li>
 *   <li>将 userId/token 放入 {@link CurrentUserContext}</li>
 * </ul>
 */
@Component
public class TokenAuthInterceptor implements HandlerInterceptor {

    private final AuthTokenRepository tokenRepository;
    private final ObjectMapper objectMapper;

    public TokenAuthInterceptor(AuthTokenRepository tokenRepository, ObjectMapper objectMapper) {
        this.tokenRepository = tokenRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行非处理器方法（例如静态资源、错误页）
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String token = extractToken(request);
        if (token == null || token.isBlank()) {
            return writeError(response, 401, "未认证：缺少 token");
        }
        Optional<AuthToken> opt = tokenRepository.findByToken(token);
        if (opt.isEmpty()) {
            return writeError(response, 401, "Token 无效");
        }
        AuthToken auth = opt.get();
        if (auth.getExpiresAt() < System.currentTimeMillis()) {
            return writeError(response, 401, "Token 已过期，请重新登录");
        }
        CurrentUserContext.set(auth.getUserId(), token);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserContext.clear();
    }

    /** 供 Controller 在业务层显式获取当前 userId（若未登录则抛业务异常）。 */
    public static String requireUserId() {
        String uid = CurrentUserContext.userId();
        if (uid == null || uid.isBlank()) {
            throw BusinessException.unauthorized("未认证");
        }
        return uid;
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring("Bearer ".length()).trim();
        }
        String param = request.getParameter("token");
        if (param != null && !param.isBlank()) {
            return param.trim();
        }
        return null;
    }

    private boolean writeError(HttpServletResponse response, int httpStatus, String message) throws Exception {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiResponse<Void> body = ApiResponse.fail(httpStatus, message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
        return false;
    }
}
