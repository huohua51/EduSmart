package com.edusmart.app.repository

import com.edusmart.app.data.dao.KnowledgePointDao
import com.edusmart.app.data.dao.TestRecordDao
import com.edusmart.app.data.dao.WrongQuestionDao
import com.edusmart.app.data.entity.KnowledgePointEntity
import com.edusmart.app.data.entity.TestRecordEntity
import com.edusmart.app.data.entity.WrongQuestionEntity
import com.edusmart.app.service.AIService
import com.edusmart.app.service.OCRService
import com.edusmart.app.service.ExamPaperAnalysisService
import com.edusmart.app.service.WrongQuestionInfo
import com.edusmart.app.service.WrongQuestionCloudService
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import java.util.UUID

class RadarRepository(
    private val knowledgePointDao: KnowledgePointDao,
    private val testRecordDao: TestRecordDao,
    private val wrongQuestionDao: WrongQuestionDao,
    private val ocrService: OCRService,
    private val aiService: AIService,
    private val examPaperAnalysisService: ExamPaperAnalysisService,
    private val cloudService: WrongQuestionCloudService = WrongQuestionCloudService()
) {
    
    fun getKnowledgePointsBySubject(subject: String): Flow<List<KnowledgePointEntity>> {
        return knowledgePointDao.getKnowledgePointsBySubject(subject)
    }

    /**
     * 批量添加错题到错题本（直接上传云端，无本地副本）
     */
    suspend fun batchAddWrongQuestions(
        userId: String,
        token: String,
        wrongQuestions: List<WrongQuestionInfo>,
        imagePath: String
    ) {
        // ☁️ 验证用户认证
        if (userId.isEmpty() || token.isEmpty()) {
            android.util.Log.e("RadarRepository", "❌ userId或token为空，无法上传错题")
            throw IllegalArgumentException("userId和token不能为空")
        }
        
        val entities = mutableListOf<WrongQuestionEntity>()
        
        wrongQuestions.forEach { wrongQuestion ->
            val entity = WrongQuestionEntity(
                id = UUID.randomUUID().toString(),
                questionText = wrongQuestion.questionText,
                answer = wrongQuestion.correctAnswer,
                steps = JSONArray(listOf(wrongQuestion.correctAnswer)).toString(),
                knowledgePoints = JSONArray(wrongQuestion.knowledgePoints).toString(),
                analysis = "试卷错题",
                imagePath = imagePath,
                userAnswer = wrongQuestion.studentAnswer,
                wrongReason = "试卷中的错题"
            )
            entities.add(entity)
        }
        
        // ☁️ 直接批量上传到云端
        try {
            android.util.Log.d("RadarRepository", "🌐 开始批量上传 ${entities.size} 道试卷错题到云端...")
            cloudService.syncWrongQuestionsBatch(userId, token, entities)
                .onSuccess { cloudIds ->
                    android.util.Log.d("RadarRepository", "✅ 试卷错题已批量同步到云端: ${cloudIds.size} 条")
                }
                .onFailure { error ->
                    android.util.Log.e("RadarRepository", "❌ 云端批量同步失败: ${error.message}")
                    throw error
                }
        } catch (e: Exception) {
            android.util.Log.e("RadarRepository", "❌ 云端批量同步异常: ${e.message}")
            throw e
        }
    }

    /**
     * 获取所有错题
     */
    fun getAllWrongQuestions(): Flow<List<WrongQuestionEntity>> {
        return wrongQuestionDao.getAllWrongQuestions()
    }

    
    /**
     * 分析成绩单
     */
    suspend fun analyzeScoreSheet(imagePath: String): ScoreAnalysis {
        val text = ocrService.recognizeText(imagePath)
        // TODO: 解析成绩单，提取各章节得分
        // 这里返回模拟数据
        return ScoreAnalysis(
            subject = "数学",
            chapters = mapOf(
                "代数" to 0.75f,
                "几何" to 0.60f,
                "函数" to 0.80f
            ),
            weakPoints = listOf("几何", "三角函数")
        )
    }
    
    /**
     * 快速测评
     */
    suspend fun quickTest(subject: String, chapters: List<String>?): TestResult {
        // TODO: 自适应出题逻辑
        // 先测大方向，再精准定位子项
        return TestResult(
            totalQuestions = 10,
            correctQuestions = 7,
            wrongKnowledgePoints = listOf("二次函数", "三角函数"),
            score = 0.7f
        )
    }
    
    /**
     * 保存测试记录
     */
    suspend fun saveTestRecord(
        subject: String,
        chapter: String?,
        score: Float,
        totalQuestions: Int,
        correctQuestions: Int,
        wrongKnowledgePoints: List<String>
    ) {
        val record = TestRecordEntity(
            id = UUID.randomUUID().toString(),
            subject = subject,
            chapter = chapter,
            score = score,
            totalQuestions = totalQuestions,
            correctQuestions = correctQuestions,
            wrongKnowledgePoints = wrongKnowledgePoints
        )
        testRecordDao.insertTestRecord(record)
        
        // 更新知识点掌握度
        wrongKnowledgePoints.forEach { pointName ->
            val point = knowledgePointDao.getKnowledgePointById(pointName)
            if (point != null) {
                knowledgePointDao.updateKnowledgePoint(
                    point.copy(
                        mastery = point.mastery * 0.9f, // 降低掌握度
                        lastTestTime = System.currentTimeMillis()
                    )
                )
            }
        }
    }
    
    /**
     * 生成学习路径
     */
    suspend fun generateLearningPath(weakPoints: List<String>): com.edusmart.app.service.LearningPath {
        return aiService.generateLearningPath(weakPoints)
    }
    
    fun getAllTestRecords(): Flow<List<TestRecordEntity>> {
        return testRecordDao.getAllTestRecords()
    }
    
    fun getTestRecordsBySubject(subject: String): Flow<List<TestRecordEntity>> {
        return testRecordDao.getTestRecordsBySubject(subject)
    }
}

data class ScoreAnalysis(
    val subject: String,
    val chapters: Map<String, Float>, // 章节 -> 得分率
    val weakPoints: List<String>
)

data class TestResult(
    val totalQuestions: Int,
    val correctQuestions: Int,
    val wrongKnowledgePoints: List<String>,
    val score: Float
)

