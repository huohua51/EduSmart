package com.edusmart.radar;

import com.edusmart.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/radar")
public class RadarController {

    @RequestMapping
    public ApiResponse<Void> notImplemented() {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "知识雷达模块后端暂未实现");
    }
}

