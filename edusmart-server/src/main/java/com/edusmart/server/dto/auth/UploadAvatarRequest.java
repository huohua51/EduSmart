package com.edusmart.server.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UploadAvatarRequest {

    /** Base64 编码后的图片数据。支持两种字段名（image / imageBase64）以兼容旧客户端。 */
    private String image;
    private String imageBase64;

    private String fileName;

    public String resolveImage() {
        if (imageBase64 != null && !imageBase64.isEmpty()) return imageBase64;
        return image;
    }
}
