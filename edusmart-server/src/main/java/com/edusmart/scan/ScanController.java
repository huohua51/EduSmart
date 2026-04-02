package com.edusmart.scan;

import com.edusmart.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/scan")
public class ScanController {

    @RequestMapping
    public ApiResponse<Void> notImplemented() {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "拍照识题模块后端暂未实现");
    }
}

