package com.edusmart.radar;

import com.edusmart.radar.dto.RadarAnalysisResponse;
import com.edusmart.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/radar")
public class RadarController {

    private final RadarService radarService;

    public RadarController(RadarService radarService) {
        this.radarService = radarService;
    }

    /**
     * 获取当前用户的知识点掌握度分析（用于雷达图）
     */
    @GetMapping("/analysis")
    public RadarAnalysisResponse analysis() {
        return radarService.analyze(SecurityUtils.requireUser());
    }
}

