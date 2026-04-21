package com.edusmart.server.controller;

import com.edusmart.server.common.ApiResponse;
import com.edusmart.server.dto.ai.AiNoteRequests;
import com.edusmart.server.dto.ai.AiNoteRequests.AnswerResponse;
import com.edusmart.server.dto.ai.AiNoteRequests.KnowledgePointsResponse;
import com.edusmart.server.dto.ai.AiNoteRequests.SubjectResponse;
import com.edusmart.server.dto.ai.AiNoteRequests.SummaryResponse;
import com.edusmart.server.dto.ai.AiNoteRequests.TextResponse;
import com.edusmart.server.dto.ai.AiNoteRequests.TitleResponse;
import com.edusmart.server.service.AiNoteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 笔记 AI 能力（Spring AI 驱动）。全部需要登录。
 */
@RestController
@RequestMapping("/api/ai/notes")
public class AiNoteController {

    private final AiNoteService aiNoteService;

    public AiNoteController(AiNoteService aiNoteService) {
        this.aiNoteService = aiNoteService;
    }

    @PostMapping("/polish")
    public ApiResponse<TextResponse> polish(@Valid @RequestBody AiNoteRequests.ContentRequest req) {
        return ApiResponse.success(new TextResponse(
                aiNoteService.polish(req.getContent(), req.getSubject())));
    }

    @PostMapping("/summary")
    public ApiResponse<SummaryResponse> summary(@Valid @RequestBody AiNoteRequests.ContentRequest req) {
        return ApiResponse.success(aiNoteService.summarize(req.getContent(), req.getSubject()));
    }

    @PostMapping("/title")
    public ApiResponse<TitleResponse> title(@Valid @RequestBody AiNoteRequests.ContentRequest req) {
        return ApiResponse.success(new TitleResponse(
                aiNoteService.generateTitle(req.getContent(), req.getSubject())));
    }

    @PostMapping("/knowledge-points")
    public ApiResponse<KnowledgePointsResponse> knowledgePoints(
            @Valid @RequestBody AiNoteRequests.ContentRequest req) {
        return ApiResponse.success(new KnowledgePointsResponse(
                aiNoteService.extractKnowledgePoints(req.getContent(), req.getSubject())));
    }

    @PostMapping("/subject")
    public ApiResponse<SubjectResponse> subject(@Valid @RequestBody AiNoteRequests.SubjectRequest req) {
        return ApiResponse.success(new SubjectResponse(
                aiNoteService.generateSubject(req.getContent(), req.getTitle())));
    }

    @PostMapping("/qa")
    public ApiResponse<AnswerResponse> qa(@Valid @RequestBody AiNoteRequests.QaRequest req) {
        return ApiResponse.success(new AnswerResponse(
                aiNoteService.answer(req.getContent(), req.getQuestion())));
    }
}
