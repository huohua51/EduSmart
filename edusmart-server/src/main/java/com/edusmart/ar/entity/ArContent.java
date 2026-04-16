package com.edusmart.ar.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "ar_content")
@Data
public class ArContent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    private ContentType contentType;
    
    @Column(name = "resource_url", nullable = false, length = 500)
    private String resourceUrl;
    
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(nullable = false)
    private Integer status = 1; // 1-正常, 0-禁用
    
    @Column(name = "created_by")
    private Long createdBy;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    public enum ContentType {
        MODEL_3D,    // 3D模型
        IMAGE_2D,    // 2D图片  
        VIDEO_360,   // 360度视频
        AUDIO,       // 音频
        TEXT,        // 文字信息
        MIXED        // 混合内容
    }
}