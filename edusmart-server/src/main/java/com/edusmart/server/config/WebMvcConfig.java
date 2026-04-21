package com.edusmart.server.config;

import com.edusmart.server.security.TokenAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * 全局 Web 配置：
 *  - CORS 放通所有域（后端对外暴露给 Android/网页均无 session cookie）
 *  - 注册 Token 拦截器，白名单放行登录/注册/静态文件/健康检查/h2 控制台
 *  - 挂载本地上传目录为静态资源
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TokenAuthInterceptor tokenAuthInterceptor;
    private final EdusmartProperties properties;

    public WebMvcConfig(TokenAuthInterceptor tokenAuthInterceptor, EdusmartProperties properties) {
        this.tokenAuthInterceptor = tokenAuthInterceptor;
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(false)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/health/**"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String prefix = properties.getPublicFilePrefix();
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        // 绝对 file:// URI 映射到磁盘目录
        String location = Paths.get(properties.getUploadDir()).toAbsolutePath().toUri().toString();
        registry.addResourceHandler(prefix + "**")
                .addResourceLocations(location);
    }
}
