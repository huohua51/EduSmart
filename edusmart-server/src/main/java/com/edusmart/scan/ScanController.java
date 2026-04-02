package com.edusmart.scan;

import com.edusmart.scan.dto.ScanRequest;
import com.edusmart.security.SecurityUtils;
import com.edusmart.wrongquestion.dto.WrongQuestionResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scan")
public class ScanController {

    private final ScanService scanService;

    public ScanController(ScanService scanService) {
        this.scanService = scanService;
    }

    /**
     * 将拍照识题结果保存为错题
     * 前端 OCR + AI 分析在本地完成，只需上传题目文本和解析结果
     */
    @PostMapping("/questions")
    public WrongQuestionResponse saveQuestion(@RequestBody ScanRequest body) {
        return scanService.saveAsWrongQuestion(SecurityUtils.requireUser(), body);
    }
}

