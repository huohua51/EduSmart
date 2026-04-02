package com.edusmart.speaking;

import com.edusmart.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/speaking")
public class SpeakingController {

    @RequestMapping
    public ApiResponse<Void> notImplemented() {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "口语模块后端暂未实现");
    }
}

