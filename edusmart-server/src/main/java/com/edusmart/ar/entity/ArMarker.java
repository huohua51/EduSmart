package com.edusmart.ar.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ar_markers")
@Data
public class ArMarker {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "content_id", nullable = false)
    private Long contentId;
    
    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;
    
    @Column(nullable = false, precision = 11, scale = 8) 
    private BigDecimal longitude;
    
    @Column(nullable = false)
    private BigDecimal altitude = BigDecimal.ZERO;
    
    @Column(name = "marker_name", nullable = false, length = 100)
    private String markerName;
    
    @Column(name = "marker_type", nullable = false)
    private String markerType = "GENERAL";
    
    @Column(name = "trigger_distance")
    private Integer triggerDistance = 50; // 触发距离(米)
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private Integer status = 1; // 1-正常, 0-禁用
    
    @Column(name = "created_by")
    private Long createdBy;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Getters for coordinate conversion
    public Double getLatitudeDouble() {
        return latitude != null ? latitude.doubleValue() : null;
    }
    
    public Double getLongitudeDouble() {
        return longitude != null ? longitude.doubleValue() : null;
    }
    
    public Double getAltitudeDouble() {
        return altitude != null ? altitude.doubleValue() : null;
    }
}