package com.edusmart.ar.repository;

import com.edusmart.ar.entity.ArContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArContentRepository extends JpaRepository<ArContent, Long> {
    
    // 根据状态查找AR内容
    List<ArContent> findByStatus(Integer status);
    
    // 根据内容类型查找AR内容
    List<ArContent> findByContentType(ArContent.ContentType contentType);
    
    // 根据标题搜索AR内容
    List<ArContent> findByTitleContainingIgnoreCase(String title);
    
    // 查找某个用户创建的所有AR内容
    List<ArContent> findByCreatedBy(Long createdBy);
    
    // 根据类型和状态查找内容
    List<ArContent> findByContentTypeAndStatus(ArContent.ContentType contentType, Integer status);
    
    // 检查资源URL是否已存在
    boolean existsByResourceUrl(String resourceUrl);
    
    // 自定义查询：根据多个ID查找内容（用于批量操作）
    @Query("SELECT c FROM ArContent c WHERE c.id IN :ids AND c.status = 1")
    List<ArContent> findByIdsAndActive(@Param("ids") List<Long> ids);
    
    // 统计某个用户的AR内容数量
    @Query("SELECT COUNT(c) FROM ArContent c WHERE c.createdBy = :userId")
    Long countByCreatedBy(@Param("userId") Long userId);
}