package com.edusmart.server.dto.note;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 对外暴露的笔记 DTO。
 *
 * <p>兼顾 Android 端历史解析：同时保留 <code>id</code> 和 <code>_id</code>，
 * 因为原 CloudBase 的返回里字段叫 <code>_id</code>。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteDto {

    private String id;

    @JsonProperty("_id")
    public String getMongoLikeId() {
        return id;
    }

    private String userId;
    private String title;
    private String subject;
    private String content;
    private List<String> images;
    private String audioPath;
    private String transcript;
    private List<String> knowledgePoints;
    private Long createdAt;
    private Long updatedAt;
}
