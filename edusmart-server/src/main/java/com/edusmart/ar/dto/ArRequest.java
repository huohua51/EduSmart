package com.edusmart.ar.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ArRequest {
    
    // 创建AR内容请求
    @Data
    public static class CreateContentRequest {
        @NotBlank(message = "标题不能为空")
        @Size(max = 255, message = "标题长度不能超过255个字符")
        private String title;
        
        @Size(max = 2000, message = "描述长度不能超过2000个字符")
        private String description;
        
        @NotNull(message = "内容类型不能为空")
        private String contentType;
        
        @NotBlank(message = "资源URL不能为空")
        @Size(max = 500, message = "资源URL长度不能超过500个字符")
        private String resourceUrl;
        
        @Size(max = 500, message = "缩略图URL长度不能超过500个字符")
        private String thumbnailUrl;
        
        private Long fileSize;
        private Long createdBy;
    }
    
    // 创建AR标记点请求
    @Data
    public static class CreateMarkerRequest {
        @NotNull(message = "内容ID不能为空")
        private Long contentId;
        
        @NotNull(message = "纬度不能为空")
        @DecimalMin(value = "-90", message = "纬度范围应在-90到90之间")
        @DecimalMax(value = "90", message = "纬度范围应在-90到90之间")
        private BigDecimal latitude;
        
        @NotNull(message = "经度不能为空")
        @DecimalMin(value = "-180", message = "经度范围应在-180到180之间")
        @DecimalMax(value = "180", message = "经度范围应在-180到180之间")
        private BigDecimal longitude;
        
        private BigDecimal altitude = BigDecimal.ZERO;
        
        @NotBlank(message = "标记点名称不能为空")
        @Size(max = 100, message = "标记点名称长度不能超过100个字符")
        private String markerName;
        
        @NotBlank(message = "标记点类型不能为空")
        private String markerType = "GENERAL";
        
        @Min(value = 10, message = "触发距离最小为10米")
        @Max(value = 1000, message = "触发距离最大为1000米")
        private Integer triggerDistance = 50;
        
        @Size(max = 1000, message = "描述长度不能超过1000个字符")
        private String description;
        
        private Long createdBy;
    }
    
    // 开始AR会话请求
    @Data
    public static class StartSessionRequest {
        @NotNull(message = "用户ID不能为空")
        private Long userId;
        
        @NotNull(message = "标记点ID不能为空")
        private Long markerId;
        
        @Size(max = 200, message = "设备信息长度不能超过200个字符")
        private String deviceInfo;
        
        private Double locationLat;
        private Double locationLng;
    }
    
    // 附近标记点查询请求
    @Data
    public static class NearbyMarkersRequest {
        @NotNull(message = "纬度不能为空")
        @DecimalMin(value = "-90", message = "纬度范围应在-90到90之间")
        @DecimalMax(value = "90", message = "纬度范围应在-90到90之间")
        private Double latitude;
        
        @NotNull(message = "经度不能为空")
        @DecimalMin(value = "-180", message = "经度范围应在-180到180之间")
        @DecimalMax(value = "180", message = "经度范围应在-180到180之间")
        private Double longitude;
        
        @Min(value = 10, message = "搜索半径最小为10米")
        @Max(value = 5000, message = "搜索半径最大为5000米")
        private Double radius = 1000.0;
    }
}