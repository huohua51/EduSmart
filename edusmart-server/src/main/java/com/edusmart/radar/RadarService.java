package com.edusmart.radar;

import com.edusmart.radar.dto.KnowledgePointStat;
import com.edusmart.radar.dto.RadarAnalysisResponse;
import com.edusmart.security.AuthenticatedUser;
import com.edusmart.wrongquestion.WrongQuestion;
import com.edusmart.wrongquestion.WrongQuestionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RadarService {

    private final WrongQuestionRepository wrongQuestionRepository;
    private final ObjectMapper objectMapper;

    public RadarService(WrongQuestionRepository wrongQuestionRepository, ObjectMapper objectMapper) {
        this.wrongQuestionRepository = wrongQuestionRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 根据错题分析用户的知识点掌握情况
     */
    public RadarAnalysisResponse analyze(AuthenticatedUser user) {
        List<WrongQuestion> allWrong = wrongQuestionRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId());

        // 统计每个知识点出现的错题次数
        Map<String, Integer> countMap = new HashMap<>();
        for (WrongQuestion wq : allWrong) {
            for (String kp : readList(wq.getKnowledgePointsJson())) {
                if (kp != null && !kp.isBlank()) {
                    countMap.merge(kp.trim(), 1, Integer::sum);
                }
            }
        }

        // 找出最多错误次数，用来归一化掌握度
        int maxCount = countMap.values().stream().mapToInt(Integer::intValue).max().orElse(1);

        List<KnowledgePointStat> stats = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
            // mastery = 1 - (wrongCount / maxCount)，错得越多掌握度越低
            double mastery = 1.0 - (double) entry.getValue() / maxCount;
            stats.add(new KnowledgePointStat(entry.getKey(), entry.getValue(), mastery));
        }

        // 按掌握度升序排（最弱的在前）
        stats.sort(Comparator.comparingDouble(KnowledgePointStat::getMastery));

        // 取掌握度低于 0.6 的作为薄弱点
        List<String> weakPoints = stats.stream()
                .filter(s -> s.getMastery() < 0.6)
                .map(KnowledgePointStat::getName)
                .toList();

        RadarAnalysisResponse response = new RadarAnalysisResponse();
        response.setTotalWrongQuestions(allWrong.size());
        response.setKnowledgePoints(stats);
        response.setWeakPoints(weakPoints);
        return response;
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
