package com.edusmart.ar;

import com.edusmart.ar.dto.ArContentDto;
import com.edusmart.ar.dto.ArMarkerDto;
import com.edusmart.ar.dto.ArRequest;
import com.edusmart.ar.entity.ArContent;
import com.edusmart.ar.entity.ArMarker;
import com.edusmart.ar.entity.ArSession;
import com.edusmart.ar.service.ArService;
import com.edusmart.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ar")
@RequiredArgsConstructor
public class ArController {
    
    private final ArService arService;
    
    // AR内容管理接口
    
    /**
     * 创建AR内容
     */
    @PostMapping("/content")
    public ApiResponse<ArContentDto> createContent(@Valid @RequestBody ArRequest.CreateContentRequest request) {
        try {
            ArContent content = new ArContent();
            content.setTitle(request.getTitle());
            content.setDescription(request.getDescription());
            content.setContentType(ArContent.ContentType.valueOf(request.getContentType()));
            content.setResourceUrl(request.getResourceUrl());
            content.setThumbnailUrl(request.getThumbnailUrl());
            content.setFileSize(request.getFileSize());
            content.setCreatedBy(request.getCreatedBy());
            
            ArContent savedContent = arService.createContent(content);
            return ApiResponse.success("AR内容创建成功", new ArContentDto(savedContent));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }
    
    /**
     * 获取AR内容详情
     */
    @GetMapping("/content/{id}")
    public ApiResponse<ArContentDto> getContent(@PathVariable Long id) {
        Optional<ArContent> content = arService.getContentById(id);
        return content.map(arContent -> 
            ApiResponse.success("获取成功", new ArContentDto(arContent))
        ).orElseGet(() -> 
            ApiResponse.error(HttpStatus.NOT_FOUND.value(), "AR内容不存在")
        );
    }
    
    /**
     * 更新AR内容
     */
    @PutMapping("/content/{id}")
    public ApiResponse<ArContentDto> updateContent(
            @PathVariable Long id, 
            @Valid @RequestBody ArRequest.CreateContentRequest request) {
        try {
            ArContent updatedContent = new ArContent();
            updatedContent.setTitle(request.getTitle());
            updatedContent.setDescription(request.getDescription());
            updatedContent.setContentType(ArContent.ContentType.valueOf(request.getContentType()));
            updatedContent.setResourceUrl(request.getResourceUrl());
            updatedContent.setThumbnailUrl(request.getThumbnailUrl());
            updatedContent.setFileSize(request.getFileSize());
            
            ArContent content = arService.updateContent(id, updatedContent);
            return ApiResponse.success("AR内容更新成功", new ArContentDto(content));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage());
        }
    }
    
    /**
     * 删除AR内容
     */
    @DeleteMapping("/content/{id}")
    public ApiResponse<Void> deleteContent(@PathVariable Long id) {
        try {
            arService.deleteContent(id);
            return ApiResponse.success("AR内容删除成功");
        } catch (Exception e) {
            return ApiResponse.error(HttpStatus.NOT_FOUND.value(), "删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 分页查询AR内容列表
     */
    @GetMapping("/content")
    public ApiResponse<Page<ArContentDto>> getContentList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ArContent> contentPage = arService.getContentList(pageable);
        Page<ArContentDto> dtoPage = contentPage.map(ArContentDto::new);
        return ApiResponse.success("查询成功", dtoPage);
    }
    
    // AR标记点管理接口
    
    /**
     * 创建AR标记点
     */
    @PostMapping("/markers")
    public ApiResponse<ArMarkerDto> createMarker(@Valid @RequestBody ArRequest.CreateMarkerRequest request) {
        try {
            ArMarker marker = new ArMarker();
            marker.setContentId(request.getContentId());
            marker.setLatitude(request.getLatitude());
            marker.setLongitude(request.getLongitude());
            marker.setAltitude(request.getAltitude());
            marker.setMarkerName(request.getMarkerName());
            marker.setMarkerType(request.getMarkerType());
            marker.setTriggerDistance(request.getTriggerDistance());
            marker.setDescription(request.getDescription());
            marker.setCreatedBy(request.getCreatedBy());
            
            ArMarker savedMarker = arService.createMarker(marker);
            return ApiResponse.success("AR标记点创建成功", new ArMarkerDto(savedMarker));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }
    
    /**
     * 查找附近的AR标记点
     */
    @GetMapping("/markers/nearby")
    public ApiResponse<List<ArMarkerDto>> getNearbyMarkers(@Valid ArRequest.NearbyMarkersRequest request) {
        try {
            List<ArMarker> markers = arService.findNearbyMarkers(
                request.getLatitude(), 
                request.getLongitude(), 
                request.getRadius()
            );
            
            List<ArMarkerDto> markerDtos = markers.stream()
                .map(ArMarkerDto::new)
                .collect(Collectors.toList());
            
            return ApiResponse.success("查询成功", markerDtos);
        } catch (Exception e) {
            return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据内容ID获取标记点列表
     */
    @GetMapping("/markers/content/{contentId}")
    public ApiResponse<List<ArMarkerDto>> getMarkersByContentId(@PathVariable Long contentId) {
        try {
            List<ArMarker> markers = arService.getMarkersByContentId(contentId);
            List<ArMarkerDto> markerDtos = markers.stream()
                .map(ArMarkerDto::new)
                .collect(Collectors.toList());
            
            return ApiResponse.success("查询成功", markerDtos);
        } catch (Exception e) {
            return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "查询失败: " + e.getMessage());
        }
    }
    
    // AR会话管理接口
    
    /**
     * 开始AR会话
     */
    @PostMapping("/sessions/start")
    public ApiResponse<ArSession> startSession(@Valid @RequestBody ArRequest.StartSessionRequest request) {
        try {
            ArSession session = arService.startSession(
                request.getUserId(), 
                request.getMarkerId(), 
                request.getDeviceInfo()
            );
            return ApiResponse.success("AR会话开始", session);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }
    
    /**
     * 结束AR会话
     */
    @PostMapping("/sessions/{sessionId}/end")
    public ApiResponse<Void> endSession(@PathVariable String sessionId) {
        try {
            arService.endSession(sessionId);
            return ApiResponse.success("AR会话结束成功");
        } catch (Exception e) {
            return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "结束会话失败: " + e.getMessage());
        }
    }
    
    /**
     * 增加会话互动次数
     */
    @PostMapping("/sessions/{sessionId}/interact")
    public ApiResponse<Void> incrementInteraction(@PathVariable String sessionId) {
        try {
            arService.incrementInteraction(sessionId);
            return ApiResponse.success("互动次数更新成功");
        } catch (Exception e) {
            return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "更新互动次数失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户AR使用统计
     */
    @GetMapping("/stats/user/{userId}")
    public ApiResponse<ArService.UserArStats> getUserStats(@PathVariable Long userId) {
        try {
            ArService.UserArStats stats = arService.getUserStats(userId);
            return ApiResponse.success("获取统计成功", stats);
        } catch (Exception e) {
            return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "获取统计失败: " + e.getMessage());
        }
    }
}

