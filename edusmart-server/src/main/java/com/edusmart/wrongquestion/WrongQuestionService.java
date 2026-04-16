package com.edusmart.wrongquestion;

import com.edusmart.security.AuthenticatedUser;
import com.edusmart.user.UserRepository;
import com.edusmart.wrongquestion.dto.WrongQuestionRequest;
import com.edusmart.wrongquestion.dto.WrongQuestionResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
public class WrongQuestionService {

    private final WrongQuestionRepository wrongQuestionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public WrongQuestionService(WrongQuestionRepository wrongQuestionRepository,
                                UserRepository userRepository,
                                ObjectMapper objectMapper) {
        this.wrongQuestionRepository = wrongQuestionRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public List<WrongQuestionResponse> list(AuthenticatedUser user) {
        return wrongQuestionRepository.findByUser_IdOrderByCreatedAtDesc(user.getUserId())
                .stream().map(this::toResponse).toList();
    }

    public List<WrongQuestionResponse> listDueForReview(AuthenticatedUser user) {
        long now = System.currentTimeMillis();
        return wrongQuestionRepository.findDueForReview(user.getUserId(), now)
                .stream().map(this::toResponse).toList();
    }

    public WrongQuestionResponse getById(AuthenticatedUser user, Long id) {
        WrongQuestion wq = wrongQuestionRepository.findByIdAndUser_Id(id, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "错题不存在"));
        return toResponse(wq);
    }

    public WrongQuestionResponse create(AuthenticatedUser user, WrongQuestionRequest req) {
        var owner = userRepository.getReferenceById(user.getUserId());
        WrongQuestion wq = new WrongQuestion();
        wq.setUser(owner);
        applyRequest(wq, req);
        if (req.getCreatedAt() != null) {
            wq.setCreatedAt(Instant.ofEpochMilli(req.getCreatedAt()));
        } else {
            wq.setCreatedAt(Instant.now());
        }
        wq.setUpdatedAt(Instant.now());
        wq = wrongQuestionRepository.save(wq);
        return toResponse(wq);
    }

    public WrongQuestionResponse update(AuthenticatedUser user, Long id, WrongQuestionRequest req) {
        WrongQuestion wq = wrongQuestionRepository.findByIdAndUser_Id(id, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "错题不存在"));
        applyRequest(wq, req);
        wq.setUpdatedAt(Instant.now());
        wq = wrongQuestionRepository.save(wq);
        return toResponse(wq);
    }

    @Transactional
    public void delete(AuthenticatedUser user, Long id) {
        wrongQuestionRepository.findByIdAndUser_Id(id, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "错题不存在"));
        wrongQuestionRepository.deleteByIdAndUser_Id(id, user.getUserId());
    }

    private void applyRequest(WrongQuestion wq, WrongQuestionRequest req) {
        wq.setQuestionText(req.getQuestionText());
        wq.setAnswer(req.getAnswer());
        wq.setAnalysis(req.getAnalysis());
        wq.setUserAnswer(req.getUserAnswer());
        wq.setWrongReason(req.getWrongReason());
        wq.setReviewCount(req.getReviewCount());
        wq.setLastReviewTime(req.getLastReviewTime());
        wq.setNextReviewTime(req.getNextReviewTime());
        try {
            wq.setStepsJson(req.getSteps() == null || req.getSteps().isEmpty()
                    ? null : objectMapper.writeValueAsString(req.getSteps()));
            wq.setKnowledgePointsJson(req.getKnowledgePoints() == null || req.getKnowledgePoints().isEmpty()
                    ? null : objectMapper.writeValueAsString(req.getKnowledgePoints()));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "序列化 JSON 失败");
        }
    }

    WrongQuestionResponse toResponse(WrongQuestion wq) {
        WrongQuestionResponse r = new WrongQuestionResponse();
        r.setId(wq.getId());
        r.setQuestionText(wq.getQuestionText());
        r.setAnswer(wq.getAnswer());
        r.setAnalysis(wq.getAnalysis());
        r.setUserAnswer(wq.getUserAnswer());
        r.setWrongReason(wq.getWrongReason());
        r.setReviewCount(wq.getReviewCount());
        r.setLastReviewTime(wq.getLastReviewTime());
        r.setNextReviewTime(wq.getNextReviewTime());
        r.setCreatedAt(wq.getCreatedAt());
        r.setUpdatedAt(wq.getUpdatedAt());
        r.setSteps(readList(wq.getStepsJson()));
        r.setKnowledgePoints(readList(wq.getKnowledgePointsJson()));
        return r;
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
