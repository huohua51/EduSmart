package com.edusmart.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 笔记实体。
 *
 * <p>为了避免多表关联，<code>images</code> 与 <code>knowledgePoints</code> 以 JSON 字符串形式存放。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "notes",
        indexes = {
                @Index(name = "idx_notes_user", columnList = "userId"),
                @Index(name = "idx_notes_user_subject", columnList = "userId, subject"),
                @Index(name = "idx_notes_user_updated", columnList = "userId, updatedAt")
        }
)
public class Note {

    /** 笔记 ID，客户端 UUID 或服务端生成。 */
    @Id
    @Column(length = 64, nullable = false, updatable = false)
    private String id;

    @Column(length = 64, nullable = false)
    private String userId;

    @Column(length = 256, nullable = false)
    private String title;

    @Column(length = 64)
    private String subject;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    /** JSON array 字符串：<code>["url1","url2"]</code>。 */
    @Lob
    @Column(columnDefinition = "TEXT", name = "images_json")
    private String imagesJson;

    @Column(length = 1024)
    private String audioPath;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String transcript;

    /** JSON array 字符串。 */
    @Lob
    @Column(columnDefinition = "TEXT", name = "knowledge_points_json")
    private String knowledgePointsJson;

    @Column(nullable = false)
    private Long createdAt;

    @Column(nullable = false)
    private Long updatedAt;
}
