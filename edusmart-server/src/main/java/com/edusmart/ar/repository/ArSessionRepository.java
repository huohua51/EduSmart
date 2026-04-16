package com.edusmart.ar.repository;

import com.edusmart.ar.entity.ArSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArSessionRepository extends JpaRepository<ArSession, Long> {
    
    // 根据用户ID查找会话记录
    List<ArSession> findByUserId(Long userId);
    
    // 根据标记点ID查找会话记录
    List<ArSession> findByMarkerId(Long markerId);
    
    // 根据会话ID查找记录
    Optional<ArSession> findBySessionId(String sessionId);
    
    // 查找某个用户的活跃会话（没有结束时间的会话）
    List<ArSession> findByUserIdAndEndTimeIsNull(Long userId);
    
    // 根据时间范围查找会话记录
    List<ArSession> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
    
    // 自定义查询：统计用户的AR使用时长
    @Query("SELECT SUM(s.durationSeconds) FROM ArSession s WHERE s.userId = :userId")
    Long sumUserDuration(@Param("userId") Long userId);
    
    // 自定义查询：统计用户的AR互动次数
    @Query("SELECT SUM(s.interactionCount) FROM ArSession s WHERE s.userId = :userId")
    Long sumUserInteractions(@Param("userId") Long userId);
    
    // 自定义查询：获取活跃用户数量（最近30天有活动的用户）
    @Query("SELECT COUNT(DISTINCT s.userId) FROM ArSession s WHERE s.startTime >= :since")
    Long countActiveUsers(@Param("since") LocalDateTime since);
    
    // 自定义查询：获取最热门的标记点（按会话次数排序）
    @Query("SELECT s.markerId, COUNT(s) as sessionCount FROM ArSession s " +
           "WHERE s.startTime >= :startDate GROUP BY s.markerId ORDER BY sessionCount DESC")
    List<Object[]> findPopularMarkers(@Param("startDate") LocalDateTime startDate);
    
    // 查找需要清理的未结束会话（超过24小时的活跃会话）
    @Query("SELECT s FROM ArSession s WHERE s.endTime IS NULL AND s.startTime < :cutoffTime")
    List<ArSession> findExpiredSessions(@Param("cutoffTime") LocalDateTime cutoffTime);
}