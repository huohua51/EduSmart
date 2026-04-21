package com.edusmart.server.service;

import com.edusmart.server.common.BusinessException;
import com.edusmart.server.config.EdusmartProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.Locale;

/**
 * 本地文件存储。所有落地文件存放于 {@code edusmart.upload-dir} 下，
 * 并通过 {@code edusmart.public-file-prefix} 对外暴露为静态资源。
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final EdusmartProperties properties;

    public FileStorageService(EdusmartProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() throws IOException {
        Path root = Paths.get(properties.getUploadDir()).toAbsolutePath();
        Files.createDirectories(root);
        log.info("📁 上传目录: {}", root);
    }

    /** 保存 base64 数据到 {@code <category>/<userId>/<fileName>} 并返回可公开访问的 URL。 */
    public StoredFile saveBase64(String category, String userId, String base64, String fileName) {
        if (base64 == null || base64.isBlank()) {
            throw BusinessException.badRequest("缺少文件数据");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64.replaceAll("\\s", ""));
        } catch (IllegalArgumentException e) {
            throw BusinessException.badRequest("base64 数据无效: " + e.getMessage());
        }

        String safeName = sanitize(fileName, category);
        String relative = category + "/" + userId + "/" + System.currentTimeMillis() + "_" + safeName;
        Path dest = Paths.get(properties.getUploadDir()).toAbsolutePath().resolve(relative);

        try {
            Files.createDirectories(dest.getParent());
            Files.write(dest, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.error("写入文件失败: {}", dest, e);
            throw new BusinessException("写入文件失败: " + e.getMessage());
        }

        String publicUrl = toPublicUrl(relative);
        return new StoredFile(publicUrl, relative);
    }

    /** 拼接公开 URL。若配置了 {@code public-base-url}，则返回绝对地址；否则返回相对路径。 */
    public String toPublicUrl(String relativePath) {
        String prefix = properties.getPublicFilePrefix();
        if (!prefix.startsWith("/")) prefix = "/" + prefix;
        if (!prefix.endsWith("/")) prefix = prefix + "/";
        String base = properties.getPublicBaseUrl();
        if (base == null || base.isBlank()) {
            return prefix + relativePath;
        }
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + prefix + relativePath;
    }

    private String sanitize(String fileName, String category) {
        String fallback = switch (category) {
            case "avatars" -> "avatar.jpg";
            case "notes" -> "file.bin";
            default -> "file.bin";
        };
        if (fileName == null || fileName.isBlank()) return fallback;
        String name = fileName.replaceAll("[^A-Za-z0-9._\\-]", "_");
        if (name.length() > 64) {
            String lower = name.toLowerCase(Locale.ROOT);
            int dot = lower.lastIndexOf('.');
            if (dot > 0 && dot > name.length() - 8) {
                name = name.substring(0, 56) + name.substring(dot);
            } else {
                name = name.substring(0, 64);
            }
        }
        return name;
    }

    public record StoredFile(String url, String cloudPath) {}
}
