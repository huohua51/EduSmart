package com.edusmart.speaking;

import com.edusmart.security.AuthenticatedUser;
import com.edusmart.speaking.dto.SpeakingMessageRequest;
import com.edusmart.speaking.dto.SpeakingMessageResponse;
import com.edusmart.speaking.dto.SpeakingSessionRequest;
import com.edusmart.speaking.dto.SpeakingSessionResponse;
import com.edusmart.speaking.dto.SpeakingStatsResponse;
import com.edusmart.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SpeakingService {

    private final SpeakingSessionRepository sessionRepository;
    private final SpeakingMessageRepository messageRepository;
    private final UserRepository userRepository;

    public SpeakingService(SpeakingSessionRepository sessionRepository,
                           SpeakingMessageRepository messageRepository,
                           UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    public List<SpeakingSessionResponse> listSessions(AuthenticatedUser user) {
        List<SpeakingSession> sessions = sessionRepository.findByUser_IdOrderByCreatedAtDesc(user.getUserId());
        return sessions.stream().map(this::toSessionResponse).toList();
    }

    public SpeakingSessionResponse getSession(AuthenticatedUser user, Long sessionId) {
        SpeakingSession session = sessionRepository.findByIdAndUser_Id(sessionId, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "练习记录不存在"));
        List<SpeakingMessage> messages = messageRepository.findBySession_IdOrderByCreatedAtAsc(sessionId);
        SpeakingSessionResponse response = toSessionResponse(session);
        response.setMessages(messages.stream().map(this::toMessageResponse).toList());
        return response;
    }

    public SpeakingSessionResponse createSession(AuthenticatedUser user, SpeakingSessionRequest req) {
        var owner = userRepository.getReferenceById(user.getUserId());
        SpeakingSession session = new SpeakingSession();
        session.setUser(owner);
        session.setLearningPurpose(req.getLearningPurpose());
        session.setScene(req.getScene());
        session.setCustomTopic(req.getCustomTopic());
        session.setDuration(req.getDuration() != null ? req.getDuration() : 0L);
        session.setMessageCount(0);
        session.setCreatedAt(Instant.now());
        session.setUpdatedAt(Instant.now());
        session = sessionRepository.save(session);
        return toSessionResponse(session);
    }

    public SpeakingMessageResponse addMessage(AuthenticatedUser user, Long sessionId, SpeakingMessageRequest req) {
        SpeakingSession session = sessionRepository.findByIdAndUser_Id(sessionId, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "练习记录不存在"));

        SpeakingMessage message = new SpeakingMessage();
        message.setSession(session);
        message.setRole(req.getRole());
        message.setContent(req.getContent());
        message.setTranslation(req.getTranslation());
        message.setScore(req.getScore());
        message.setSuggestedReply(req.getSuggestedReply());
        message.setCreatedAt(Instant.now());
        message = messageRepository.save(message);

        session.setMessageCount(session.getMessageCount() + 1);
        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);

        return toMessageResponse(message);
    }

    public SpeakingStatsResponse getStats(AuthenticatedUser user) {
        List<SpeakingSession> sessions = sessionRepository.findByUser_IdOrderByCreatedAtDesc(user.getUserId());

        SpeakingStatsResponse stats = new SpeakingStatsResponse();
        stats.setTotalSessions(sessions.size());

        long totalDuration = 0L;
        for (SpeakingSession s : sessions) {
            if (s.getDuration() != null) {
                totalDuration += s.getDuration();
            }
        }
        stats.setTotalDuration(totalDuration);

        // Collect all scored messages for average score calculation
        List<Float> allScores = new ArrayList<>();
        for (SpeakingSession s : sessions) {
            List<SpeakingMessage> messages = messageRepository.findBySession_IdOrderByCreatedAtAsc(s.getId());
            for (SpeakingMessage m : messages) {
                if (m.getScore() != null) {
                    allScores.add(m.getScore());
                }
            }
        }

        if (!allScores.isEmpty()) {
            float sum = 0f;
            for (Float score : allScores) {
                sum += score;
            }
            stats.setAverageScore(sum / allScores.size());
        } else {
            stats.setAverageScore(0f);
        }

        // Recent 10 scores from sessions (totalScore), newest first
        List<Float> recentScores = new ArrayList<>();
        for (SpeakingSession s : sessions) {
            if (s.getTotalScore() != null) {
                recentScores.add(s.getTotalScore());
            }
            if (recentScores.size() >= 10) {
                break;
            }
        }
        stats.setRecentScores(recentScores);

        // Favorite scene: most frequently used scene
        if (!sessions.isEmpty()) {
            Map<String, Long> sceneCounts = sessions.stream()
                    .filter(s -> s.getScene() != null && !s.getScene().isBlank())
                    .collect(Collectors.groupingBy(SpeakingSession::getScene, Collectors.counting()));
            String favoriteScene = sceneCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
            stats.setFavoriteScene(favoriteScene);
        }

        return stats;
    }

    @Transactional
    public void deleteSession(AuthenticatedUser user, Long sessionId) {
        SpeakingSession session = sessionRepository.findByIdAndUser_Id(sessionId, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "练习记录不存在"));
        sessionRepository.delete(session);
    }

    private SpeakingSessionResponse toSessionResponse(SpeakingSession session) {
        SpeakingSessionResponse r = new SpeakingSessionResponse();
        r.setId(session.getId());
        r.setLearningPurpose(session.getLearningPurpose());
        r.setScene(session.getScene());
        r.setCustomTopic(session.getCustomTopic());
        r.setTotalScore(session.getTotalScore());
        r.setMessageCount(session.getMessageCount());
        r.setDuration(session.getDuration());
        r.setCreatedAt(session.getCreatedAt());
        r.setUpdatedAt(session.getUpdatedAt());
        return r;
    }

    private SpeakingMessageResponse toMessageResponse(SpeakingMessage message) {
        SpeakingMessageResponse r = new SpeakingMessageResponse();
        r.setId(message.getId());
        r.setRole(message.getRole());
        r.setContent(message.getContent());
        r.setTranslation(message.getTranslation());
        r.setScore(message.getScore());
        r.setSuggestedReply(message.getSuggestedReply());
        r.setCreatedAt(message.getCreatedAt());
        return r;
    }
}
