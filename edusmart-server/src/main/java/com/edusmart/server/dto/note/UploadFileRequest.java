package com.edusmart.server.dto.note;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UploadFileRequest {

    @NotBlank(message = "fileBase64 不能为空")
    private String fileBase64;

    @NotBlank(message = "fileName 不能为空")
    private String fileName;

    /** image / audio / file。为空时按扩展名自动判断。 */
    private String fileType;
}
