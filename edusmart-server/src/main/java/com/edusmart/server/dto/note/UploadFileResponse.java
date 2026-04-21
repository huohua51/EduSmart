package com.edusmart.server.dto.note;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadFileResponse {

    /** 站内可访问的 URL。 */
    private String url;

    /** 相对路径（相对于 upload-dir）。 */
    private String cloudPath;

    /** 与 CloudBase 兼容的字段名。 */
    private String fileId;
}
