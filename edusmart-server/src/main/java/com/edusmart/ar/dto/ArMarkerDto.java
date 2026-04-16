package com.edusmart.ar.dto;

import com.edusmart.ar.entity.ArMarker;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ArMarkerDto {
    private Long id;
    private Long contentId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal altitude;
    private String markerName;
    private String markerType;
    private Integer triggerDistance;
    private String description;
    private Integer status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 方便前端使用的double坐标
    private Double lat;
    private Double lng;
    private Double alt;
    
    // 从实体转换的构造函数
    public ArMarkerDto(ArMarker marker) {
        this.id = marker.getId();
        this.contentId = marker.getContentId();
        this.latitude = marker.getLatitude();
        this.longitude = marker.getLongitude();
        this.altitude = marker.getAltitude();
        this.markerName = marker.getMarkerName();
        this.markerType = marker.getMarkerType();
        this.triggerDistance = marker.getTriggerDistance();
        this.description = marker.getDescription();
        this.status = marker.getStatus();
        this.createdBy = marker.getCreatedBy();
        this.createdAt = marker.getCreatedAt();
        this.updatedAt = marker.getUpdatedAt();
        
        // 设置方便使用的double坐标
        this.lat = marker.getLatitudeDouble();
        this.lng = marker.getLongitudeDouble();
        this.alt = marker.getAltitudeDouble();
    }
    
    public ArMarkerDto() {}
}