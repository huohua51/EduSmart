package com.edusmart.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class NotImplementedController {

    @GetMapping("/_framework")
    public ApiResponse<String> framework() {
        return ApiResponse.ok("Backend framework is up");
    }

    @GetMapping("/_todo")
    public ApiResponse<Void> todo() {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "该模块接口尚未实现");
    }
}

