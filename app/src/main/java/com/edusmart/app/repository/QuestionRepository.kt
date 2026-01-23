package com.edusmart.app.repository

import com.edusmart.app.data.dao.QuestionDao
import com.edusmart.app.data.dao.WrongQuestionDao
import com.edusmart.app.data.entity.QuestionEntity
import com.edusmart.app.data.entity.WrongQuestionEntity
import com.edusmart.app.service.AIService
import com.edusmart.app.service.OCRService
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import java.util.UUID

class QuestionRepository(
    private val questionDao: QuestionDao,
    private val wrongQuestionDao: WrongQuestionDao,
    private val ocrService: OCRService,
    private val aiService: AIService
) {
    
    fun getQuestionsBySubject(subject: String): Flow<List<QuestionEntity>> {
        return questionDao.getQuestionsBySubject(subject)
    }
    
    suspend fun getQuestionById(id: String): QuestionEntity? {
        return questionDao.getQuestionById(id)
    }
    
    suspend fun searchQuestions(keyword: String): List<QuestionEntity> {
        return questionDao.searchQuestions(keyword)
    }
    
    /**
     * 扫描题目并识别
     */
    suspend fun scanQuestion(imagePath: String): QuestionRecognitionResult {
        val recognitionResult = ocrService.recognizeQuestion(imagePath)
        
        // 保存识别结果到数据库
        val question = QuestionEntity(
            id = UUID.randomUUID().toString(),
            content = recognitionResult.content,
            subject = recognitionResult.subject,
            chapter = "", // 可以从内容中提取
            knowledgePoints = recognitionResult.knowledgePoints,
            answer = "", // 需要匹配题库或AI生成
            solution = "", // 需要AI生成
            difficulty = recognitionResult.difficulty,
            imageUrl = imagePath
        )
        
        // 尝试匹配题库
        val matchedQuestions = questionDao.searchQuestions(recognitionResult.content)
        if (matchedQuestions.isNotEmpty()) {
            val matched = matchedQuestions.first()
            questionDao.insertQuestion(matched)
            return QuestionRecognitionResult(
                question = matched,
                matched = true
            )
        }
        
        // 如果没有匹配，使用AI生成答案
        val answer = aiService.answerQuestion(recognitionResult.content)
        val updatedQuestion = question.copy(
            answer = answer,
            solution = answer
        )
        questionDao.insertQuestion(updatedQuestion)
        
        return QuestionRecognitionResult(
            question = updatedQuestion,
            matched = false
        )
    }
    
    /**
     * 添加到错题本
     */
    suspend fun addToWrongQuestions(questionId: String, userAnswer: String?, wrongReason: String?) {
        val question = questionDao.getQuestionById(questionId) ?: return

        // 将解题步骤转换为JSON数组格式
        val stepsJson = try {
            // 如果solution包含换行符，按行分割；否则作为单个步骤
            val stepsList = if (question.solution.contains("\n")) {
                question.solution.split("\n").filter { it.isNotBlank() }
            } else {
                listOf(question.solution)
            }
            JSONArray(stepsList).toString()
        } catch (e: Exception) {
            JSONArray(listOf(question.solution)).toString()
        }

        // 将知识点列表转换为JSON数组格式
        val knowledgePointsJson = JSONArray(question.knowledgePoints).toString()

        val wrongQuestion = WrongQuestionEntity(
            id = UUID.randomUUID().toString(),
            questionText = question.content,
            answer = question.answer,
            steps = stepsJson,
            knowledgePoints = knowledgePointsJson,
            analysis = wrongReason ?: "",
            imagePath = question.imageUrl,
            userAnswer = userAnswer,
            wrongReason = wrongReason
        )
        wrongQuestionDao.insertWrongQuestion(wrongQuestion)
    }
    
    /**
     * 获取错题本
     */
    fun getWrongQuestions(): Flow<List<WrongQuestionEntity>> {
        return wrongQuestionDao.getAllWrongQuestions()
    }
    
    /**
     * 获取需要复习的题目
     */
    suspend fun getQuestionsToReview(): List<WrongQuestionEntity> {
        return wrongQuestionDao.getQuestionsToReview()
    }
    
    /**
     * 生成变式题
     */
    suspend fun generateVariations(questionId: String): List<QuestionEntity> {
        val question = questionDao.getQuestionById(questionId) ?: return emptyList()
        val variations = aiService.generateVariations(question.content)
        
        return variations.mapIndexed { index, content ->
            QuestionEntity(
                id = UUID.randomUUID().toString(),
                content = content,
                subject = question.subject,
                chapter = question.chapter,
                knowledgePoints = question.knowledgePoints,
                answer = "",
                solution = "",
                difficulty = question.difficulty
            )
        }
    }
}

data class QuestionRecognitionResult(
    val question: QuestionEntity,
    val matched: Boolean // 是否从题库匹配到
)

