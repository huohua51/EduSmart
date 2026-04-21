package com.edusmart.server.controller;

import com.edusmart.server.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查，无需认证。
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ApiResponse<Map<String, Object>> ping() {
        return ApiResponse.success(Map.of(
                "status", "UP",
                "timestamp", System.currentTimeMillis()
        ));
    }
}
