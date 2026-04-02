package com.edusmart.ar;

import com.edusmart.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ar")
public class ArController {

    @RequestMapping
    public ApiResponse<Void> notImplemented() {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "AR 模块后端暂未实现");
    }
}

