package com.edusmart.server.security;

/**
 * 请求级当前用户上下文，由 {@link TokenAuthInterceptor} 写入，供 Controller / Service 取用。
 */
public final class CurrentUserContext {

    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> TOKEN = new ThreadLocal<>();

    private CurrentUserContext() {}

    public static void set(String userId, String token) {
        USER_ID.set(userId);
        TOKEN.set(token);
    }

    public static String userId() {
        return USER_ID.get();
    }

    public static String token() {
        return TOKEN.get();
    }

    public static void clear() {
        USER_ID.remove();
        TOKEN.remove();
    }
}
