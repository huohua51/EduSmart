package com.edusmart.server.dto.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建 / 更新笔记请求体。
 */
@Data
public class SaveNoteRequest {

    /** 客户端可指定 ID（UUID），为空时服务端自动生成。 */
    private String id;

    @NotBlank(message = "标题不能为空")
    @Size(max = 256)
    private String title;

    @Size(max = 64)
    private String subject;

    private String content;

    private List<String> images;

    private String audioPath;

    private String transcript;

    private List<String> knowledgePoints;
}
