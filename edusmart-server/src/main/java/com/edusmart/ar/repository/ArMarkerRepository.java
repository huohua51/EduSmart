package com.edusmart.ar.repository;

import com.edusmart.ar.entity.ArMarker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArMarkerRepository extends JpaRepository<ArMarker, Long> {
    
    // 根据状态查找标记点
    List<ArMarker> findByStatus(Integer status);
    
    // 根据内容ID查找标记点
    List<ArMarker> findByContentId(Long contentId);
    
    // 根据标记类型查找标记点
    List<ArMarker> findByMarkerType(String markerType);
    
    // 查找某个用户创建的所有标记点
    List<ArMarker> findByCreatedBy(Long createdBy);
    
    // 根据内容ID和状态查找标记点
    List<ArMarker> findByContentIdAndStatus(Long contentId, Integer status);
    
    // 自定义查询：根据地理位置范围查找标记点
    @Query("SELECT m FROM ArMarker m WHERE " +
           "m.latitude BETWEEN :minLat AND :maxLat AND " +
           "m.longitude BETWEEN :minLng AND :maxLng AND " +
           "m.status = 1")
    List<ArMarker> findMarkersInBounds(
        @Param("minLat") BigDecimal minLat,
        @Param("maxLat") BigDecimal maxLat,
        @Param("minLng") BigDecimal minLng,
        @Param("maxLng") BigDecimal maxLng
    );
    
    // 查找距离指定坐标一定范围内的标记点（球面距离近似计算）
    @Query("SELECT m FROM ArMarker m WHERE " +
           "6371 * 2 * ASIN(SQRT(POW(SIN((RADIANS(:lat) - RADIANS(m.latitudeDouble())) / 2), 2) + " +
           "COS(RADIANS(:lat)) * COS(RADIANS(m.latitudeDouble())) * " +
           "POW(SIN((RADIANS(:lng) - RADIANS(m.longitudeDouble())) / 2), 2))) * 1000 <= :radius AND " +
           "m.status = 1")
    List<ArMarker> findNearbyMarkers(
        @Param("lat") Double lat,
        @Param("lng") Double lng,
        @Param("radius") Double radius
    );
    
    // 检查指定内容ID是否存在有效标记点
    @Query("SELECT COUNT(m) > 0 FROM ArMarker m WHERE m.contentId = :contentId AND m.status = 1")
    boolean existsActiveMarkerByContentId(@Param("contentId") Long contentId);
    
    // 统计某个内容的标记点数量
    @Query("SELECT COUNT(m) FROM ArMarker m WHERE m.contentId = :contentId AND m.status = 1")
    Long countActiveMarkersByContentId(@Param("contentId") Long contentId);
}