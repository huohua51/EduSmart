package com.edusmart.ar.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "ar_sessions")
@Data
public class ArSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "marker_id", nullable = false)
    private Long markerId;
    
    @Column(name = "session_id", nullable = false, unique = true, length = 64)
    private String sessionId;
    
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime = LocalDateTime.now();
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Column(name = "duration_seconds")
    private Integer durationSeconds;
    
    @Column(name = "device_info", length = 200)
    private String deviceInfo;
    
    @Column(name = "location_lat", precision = 10, scale = 8)
    private Double locationLat;
    
    @Column(name = "location_lng", precision = 11, scale = 8)
    private Double locationLng;
    
    @Column(name = "interaction_count", nullable = false)
    private Integer interactionCount = 0;
    
    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON格式的额外数据
    
    // 计算会话持续时间
    public void calculateDuration() {
        if (endTime != null && startTime != null) {
            this.durationSeconds = (int) java.time.Duration.between(startTime, endTime).getSeconds();
        }
    }
    
    // 结束会话
    public void endSession() {
        this.endTime = LocalDateTime.now();
        calculateDuration();
    }
}