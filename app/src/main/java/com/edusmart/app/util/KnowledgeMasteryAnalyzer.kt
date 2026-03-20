package com.edusmart.app.util

import com.edusmart.app.data.entity.WrongQuestionEntity
import org.json.JSONArray
import kotlin.math.log10

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
     * 计算单个学科的掌握度（改进版）
     *
     * 计算逻辑：
     * 1. 基础分：根据错题数量使用对数衰减（避免线性扣分过快归零）
     * 2. 复习加成：复习次数越多，掌握度越高（指数增长）
     * 3. 时间衰减：错题越久未复习，掌握度越低（渐进式衰减）
     * 4. 知识点覆盖度：错题涉及的知识点越集中，说明某个点没掌握好
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

        // 1. 基础分：使用对数函数，避免错题多时分数过快归零
        // 公式：100 - 30 * log10(错题数 + 1)
        // 1道错题≈91分，5道≈79分，10道≈70分，20道≈61分，50道≈49分
        val wrongCount = wrongQuestions.size
        val baseScore = 100f - (30f * kotlin.math.log10(wrongCount.toFloat() + 1))

        // 2. 复习加成：复习率越高，掌握度越好
        // 公式：复习率 * 20分（最多加20分）
        val reviewedCount = wrongQuestions.count { it.reviewCount > 0 }
        val reviewRate = if (wrongCount > 0) reviewedCount.toFloat() / wrongCount else 0f
        val reviewBonus = reviewRate * 20f

        // 3. 时间衰减：根据错题的平均"年龄"计算衰减
        // 7天内：无衰减，7-30天：轻微衰减，30天以上：明显衰减
        val currentTime = System.currentTimeMillis()
        val avgAge = wrongQuestions.map {
            (currentTime - it.createdAt) / (24 * 60 * 60 * 1000L) // 转换为天数
        }.average().toFloat()

        val timePenalty = when {
            avgAge < 7 -> 0f           // 7天内：无衰减
            avgAge < 30 -> (avgAge - 7) * 0.3f  // 7-30天：每天扣0.3分
            else -> 7f + (avgAge - 30) * 0.5f   // 30天以上：基础扣7分，之后每天扣0.5分
        }.coerceAtMost(25f) // 最多扣25分

        // 4. 知识点集中度惩罚：如果错题都集中在少数几个知识点，说明这些点没掌握好
        val knowledgePointFrequency = mutableMapOf<String, Int>()
        wrongQuestions.forEach { question ->
            val points = parseKnowledgePoints(question.knowledgePoints)
            points.forEach { point ->
                knowledgePointFrequency[point] = knowledgePointFrequency.getOrDefault(point, 0) + 1
            }
        }

        // 如果某个知识点错误次数超过总错题的50%，额外扣分
        val maxFrequency = knowledgePointFrequency.values.maxOrNull() ?: 0
        val concentrationPenalty = if (maxFrequency > wrongCount * 0.5f) {
            (maxFrequency - wrongCount * 0.5f) * 2f  // 超过50%的部分，每题扣2分
        } else 0f

        // 最终分数
        val masteryScore = (baseScore + reviewBonus - timePenalty - concentrationPenalty)
            .coerceIn(0f, 100f)

        // 统计薄弱知识点（取前5个）
        val weakKnowledgePoints = knowledgePointFrequency.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { WeakKnowledgePoint(it.key, it.value) }

        return SubjectMastery(
            subject = subject,
            masteryScore = masteryScore,
            wrongCount = wrongQuestions.size,
            reviewedCount = reviewedCount,
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

