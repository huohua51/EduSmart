package com.edusmart.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 业务自定义配置，来自 application.yml 的 <code>edusmart.*</code> 段。
 */
@Data
@Component
@ConfigurationProperties(prefix = "edusmart")
public class EdusmartProperties {

    /** Token 过期时间（毫秒），默认 30 天。 */
    private long tokenTtlMs = 30L * 24 * 60 * 60 * 1000;

    /** 文件本地存储目录。 */
    private String uploadDir = "./uploads";

    /** 静态文件对外暴露路径前缀。 */
    private String publicFilePrefix = "/files";

    /** 对外基础 URL，用于拼接返回给客户端的附件地址；空则返回相对路径。 */
    private String publicBaseUrl = "";
}
