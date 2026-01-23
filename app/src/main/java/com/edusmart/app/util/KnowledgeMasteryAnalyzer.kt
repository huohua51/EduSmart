package com.edusmart.app.util

import com.edusmart.app.data.entity.WrongQuestionEntity
import org.json.JSONArray

/**
 * 知识点掌握度分析器
 */
class KnowledgeMasteryAnalyzer {

    /**
     * 分析学科掌握度
     */
    fun analyzeSubjectMastery(wrongQuestions: List<WrongQuestionEntity>): Map<String, SubjectMastery> {
        val subjectMap = mutableMapOf<String, MutableList<WrongQuestionEntity>>()

        // 按学科分组
        wrongQuestions.forEach { question ->
            val subject = extractSubjectFromKnowledgePoints(question.knowledgePoints)
            subjectMap.getOrPut(subject) { mutableListOf() }.add(question)
        }

        // 计算每个学科的掌握度
        return subjectMap.mapValues { (subject, questions) ->
            calculateSubjectMastery(subject, questions)
        }
    }

    /**
     * 计算单个学科的掌握度
     */
    private fun calculateSubjectMastery(
        subject: String,
        wrongQuestions: List<WrongQuestionEntity>
    ): SubjectMastery {
        if (wrongQuestions.isEmpty()) {
            return SubjectMastery(
                subject = subject,
                masteryScore = 100f,
                wrongCount = 0,
                reviewedCount = 0,
                weakKnowledgePoints = emptyList()
            )
        }

        val baseScore = 100f
        val wrongPenalty = wrongQuestions.size * 5f
        val reviewBonus = wrongQuestions.sumOf { it.reviewCount } * 2f

        // 计算时间衰减（超过30天未复习的错题）
        val currentTime = System.currentTimeMillis()
        val oldQuestionsCount = wrongQuestions.count {
            currentTime - it.createdAt > 30 * 24 * 60 * 60 * 1000L
        }
        val timePenalty = oldQuestionsCount * 3f

        val masteryScore = (baseScore - wrongPenalty + reviewBonus - timePenalty).coerceIn(0f, 100f)

        // 统计薄弱知识点
        val knowledgePointFrequency = mutableMapOf<String, Int>()
        wrongQuestions.forEach { question ->
            val points = parseKnowledgePoints(question.knowledgePoints)
            points.forEach { point ->
                knowledgePointFrequency[point] = knowledgePointFrequency.getOrDefault(point, 0) + 1
            }
        }

        val weakKnowledgePoints = knowledgePointFrequency.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { WeakKnowledgePoint(it.key, it.value) }

        return SubjectMastery(
            subject = subject,
            masteryScore = masteryScore,
            wrongCount = wrongQuestions.size,
            reviewedCount = wrongQuestions.count { it.reviewCount > 0 },
            weakKnowledgePoints = weakKnowledgePoints
        )
    }

    /**
     * 从知识点JSON字符串中提取学科
     */
    private fun extractSubjectFromKnowledgePoints(knowledgePointsJson: String): String {
        return try {
            val points = parseKnowledgePoints(knowledgePointsJson)
            if (points.isEmpty()) return "其他"

            // 根据知识点关键词判断学科
            val firstPoint = points.first()
            when {
                firstPoint.contains("函数") || firstPoint.contains("方程") ||
                firstPoint.contains("几何") || firstPoint.contains("代数") -> "数学"
                firstPoint.contains("力") || firstPoint.contains("电") ||
                firstPoint.contains("光") || firstPoint.contains("热") -> "物理"
                firstPoint.contains("化学") || firstPoint.contains("元素") ||
                firstPoint.contains("反应") || firstPoint.contains("分子") -> "化学"
                firstPoint.contains("语法") || firstPoint.contains("词汇") ||
                firstPoint.contains("阅读") || firstPoint.contains("写作") -> "英语"
                else -> "其他"
            }
        } catch (e: Exception) {
            "其他"
        }
    }

    /**
     * 解析知识点JSON字符串
     */
    private fun parseKnowledgePoints(knowledgePointsJson: String): List<String> {
        return try {
            val array = JSONArray(knowledgePointsJson)
            List(array.length()) { array.getString(it) }
        } catch (e: Exception) {
            // 如果不是JSON格式，尝试按逗号分割
            knowledgePointsJson.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }
}

/**
 * 学科掌握度
 */
data class SubjectMastery(
    val subject: String,
    val masteryScore: Float,
    val wrongCount: Int,
    val reviewedCount: Int,
    val weakKnowledgePoints: List<WeakKnowledgePoint>
)

/**
 * 薄弱知识点
 */
data class WeakKnowledgePoint(
    val name: String,
    val errorCount: Int
)

