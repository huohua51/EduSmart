package com.edusmart.ar.service;

import com.edusmart.ar.entity.ArContent;
import com.edusmart.ar.entity.ArMarker;
import com.edusmart.ar.entity.ArSession;
import com.edusmart.ar.repository.ArContentRepository;
import com.edusmart.ar.repository.ArMarkerRepository;
import com.edusmart.ar.repository.ArSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArService {
    
    private final ArContentRepository arContentRepository;
    private final ArMarkerRepository arMarkerRepository;
    private final ArSessionRepository arSessionRepository;
    
    // AR内容管理功能
    
    /**
     * 创建AR内容
     */
    @Transactional
    public ArContent createContent(ArContent content) {
        // 验证资源URL是否重复
        if (arContentRepository.existsByResourceUrl(content.getResourceUrl())) {
            throw new IllegalArgumentException("资源URL已存在");
        }
        
        content.setCreatedAt(LocalDateTime.now());
        content.setUpdatedAt(LocalDateTime.now());
        return arContentRepository.save(content);
    }
    
    /**
     * 获取AR内容详情
     */
    public Optional<ArContent> getContentById(Long id) {
        return arContentRepository.findById(id);
    }
    
    /**
     * 更新AR内容
     */
    @Transactional
    public ArContent updateContent(Long id, ArContent updatedContent) {
        return arContentRepository.findById(id)
                .map(existingContent -> {
                    existingContent.setTitle(updatedContent.getTitle());
                    existingContent.setDescription(updatedContent.getDescription());
                    existingContent.setResourceUrl(updatedContent.getResourceUrl());
                    existingContent.setThumbnailUrl(updatedContent.getThumbnailUrl());
                    existingContent.setUpdatedAt(LocalDateTime.now());
                    return arContentRepository.save(existingContent);
                })
                .orElseThrow(() -> new IllegalArgumentException("AR内容不存在"));
    }
    
    /**
     * 删除AR内容（软删除）
     */
    @Transactional
    public void deleteContent(Long id) {
        arContentRepository.findById(id)
                .ifPresent(content -> {
                    content.setStatus(0);
                    content.setUpdatedAt(LocalDateTime.now());
                    arContentRepository.save(content);
                });
    }
    
    /**
     * 分页查询AR内容
     */
    public Page<ArContent> getContentList(Pageable pageable) {
        return arContentRepository.findAll(pageable);
    }
    
    // AR标记点管理功能
    
    /**
     * 创建AR标记点
     */
    @Transactional
    public ArMarker createMarker(ArMarker marker) {
        // 验证内容是否存在
        if (!arContentRepository.existsById(marker.getContentId())) {
            throw new IllegalArgumentException("关联的AR内容不存在");
        }
        
        marker.setCreatedAt(LocalDateTime.now());
        marker.setUpdatedAt(LocalDateTime.now());
        return arMarkerRepository.save(marker);
    }
    
    /**
     * 查找附近的AR标记点
     */
    public List<ArMarker> findNearbyMarkers(Double latitude, Double longitude, Double radius) {
        if (radius == null || radius <= 0) {
            radius = 1000.0; // 默认1公里范围
        }
        
        return arMarkerRepository.findNearbyMarkers(latitude, longitude, radius);
    }
    
    /**
     * 根据内容ID获取标记点列表
     */
    public List<ArMarker> getMarkersByContentId(Long contentId) {
        return arMarkerRepository.findByContentIdAndStatus(contentId, 1);
    }
    
    // AR会话管理功能
    
    /**
     * 开始AR会话
     */
    @Transactional
    public ArSession startSession(Long userId, Long markerId, String deviceInfo) {
        // 验证标记点是否存在
        if (!arMarkerRepository.existsActiveMarkerByContentId(markerId)) {
            throw new IllegalArgumentException("AR标记点不存在或已禁用");
        }
        
        ArSession session = new ArSession();
        session.setUserId(userId);
        session.setMarkerId(markerId);
        session.setSessionId(generateSessionId());
        session.setDeviceInfo(deviceInfo);
        session.setStartTime(LocalDateTime.now());
        
        return arSessionRepository.save(session);
    }
    
    /**
     * 结束AR会话
     */
    @Transactional
    public void endSession(String sessionId) {
        arSessionRepository.findBySessionId(sessionId)
                .ifPresent(session -> {
                    session.endSession();
                    arSessionRepository.save(session);
                });
    }
    
    /**
     * 增加会话互动次数
     */
    @Transactional
    public void incrementInteraction(String sessionId) {
        arSessionRepository.findBySessionId(sessionId)
                .ifPresent(session -> {
                    session.setInteractionCount(session.getInteractionCount() + 1);
                    arSessionRepository.save(session);
                });
    }
    
    /**
     * 获取用户统计数据
     */
    public UserArStats getUserStats(Long userId) {
        Long totalDuration = arSessionRepository.sumUserDuration(userId);
        Long totalInteractions = arSessionRepository.sumUserInteractions(userId);
        Long sessionCount = arSessionRepository.countByUserId(userId);
        
        return new UserArStats(
            userId,
            totalDuration != null ? totalDuration : 0L,
            totalInteractions != null ? totalInteractions : 0L,
            sessionCount != null ? sessionCount : 0L
        );
    }
    
    // 辅助方法
    
    private String generateSessionId() {
        return "ar_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
    
    // 统计结果类
    public static class UserArStats {
        private final Long userId;
        private final Long totalDurationSeconds;
        private final Long totalInteractions;
        private final Long sessionCount;
        
        public UserArStats(Long userId, Long totalDurationSeconds, Long totalInteractions, Long sessionCount) {
            this.userId = userId;
            this.totalDurationSeconds = totalDurationSeconds;
            this.totalInteractions = totalInteractions;
            this.sessionCount = sessionCount;
        }
        
        // Getters
        public Long getUserId() { return userId; }
        public Long getTotalDurationSeconds() { return totalDurationSeconds; }
        public Long getTotalInteractions() { return totalInteractions; }
        public Long getSessionCount() { return sessionCount; }
    }
}