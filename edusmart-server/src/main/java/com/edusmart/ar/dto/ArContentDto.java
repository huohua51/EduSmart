package com.edusmart.ar.dto;

import com.edusmart.ar.entity.ArContent;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArContentDto {
    private Long id;
    private String title;
    private String description;
    private ArContent.ContentType contentType;
    private String resourceUrl;
    private String thumbnailUrl;
    private Long fileSize;
    private Integer status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 从实体转换的构造函数
    public ArContentDto(ArContent content) {
        this.id = content.getId();
        this.title = content.getTitle();
        this.description = content.getDescription();
        this.contentType = content.getContentType();
        this.resourceUrl = content.getResourceUrl();
        this.thumbnailUrl = content.getThumbnailUrl();
        this.fileSize = content.getFileSize();
        this.status = content.getStatus();
        this.createdBy = content.getCreatedBy();
        this.createdAt = content.getCreatedAt();
        this.updatedAt = content.getUpdatedAt();
    }
    
    public ArContentDto() {}
}