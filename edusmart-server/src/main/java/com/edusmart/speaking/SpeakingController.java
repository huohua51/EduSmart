package com.edusmart.speaking;

import com.edusmart.security.SecurityUtils;
import com.edusmart.speaking.dto.SpeakingMessageRequest;
import com.edusmart.speaking.dto.SpeakingMessageResponse;
import com.edusmart.speaking.dto.SpeakingSessionRequest;
import com.edusmart.speaking.dto.SpeakingSessionResponse;
import com.edusmart.speaking.dto.SpeakingStatsResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/speaking")
public class SpeakingController {

    private final SpeakingService speakingService;

    public SpeakingController(SpeakingService speakingService) {
        this.speakingService = speakingService;
    }

    @GetMapping("/sessions")
    public List<SpeakingSessionResponse> listSessions() {
        return speakingService.listSessions(SecurityUtils.requireUser());
    }

    @GetMapping("/sessions/{id}")
    public SpeakingSessionResponse getSession(@PathVariable Long id) {
        return speakingService.getSession(SecurityUtils.requireUser(), id);
    }

    @PostMapping("/sessions")
    public SpeakingSessionResponse createSession(@RequestBody SpeakingSessionRequest body) {
        return speakingService.createSession(SecurityUtils.requireUser(), body);
    }

    @PostMapping("/sessions/{id}/messages")
    public SpeakingMessageResponse addMessage(@PathVariable Long id, @RequestBody SpeakingMessageRequest body) {
        return speakingService.addMessage(SecurityUtils.requireUser(), id, body);
    }

    @DeleteMapping("/sessions/{id}")
    public void deleteSession(@PathVariable Long id) {
        speakingService.deleteSession(SecurityUtils.requireUser(), id);
    }

    @GetMapping("/stats")
    public SpeakingStatsResponse getStats() {
        return speakingService.getStats(SecurityUtils.requireUser());
    }
}
