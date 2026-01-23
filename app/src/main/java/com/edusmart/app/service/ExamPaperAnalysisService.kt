package com.edusmart.app.service

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * 试卷分析服务
 * 负责扫描试卷、识别错题、分析知识点
 */
class ExamPaperAnalysisService(
    private val ocrService: OCRService,
    private val qwenService: QwenAIService
) {

    /**
     * 分析试卷图片，识别错题
     */
    suspend fun analyzeExamPaper(imagePath: String): ExamPaperAnalysisResult {
        return try {
            // 步骤1: OCR识别试卷内容
            val paperText = ocrService.recognizeText(imagePath)

            if (paperText.isBlank()) {
                return ExamPaperAnalysisResult(
                    success = false,
                    errorMessage = "无法识别试卷内容，请确保图片清晰"
                )
            }

            // 步骤2: 使用AI分析试卷，提取错题
            val analysisPrompt = """
                你是一个专业的试卷分析助手。请仔细分析以下试卷内容，**严格只识别标记为错误的题目**。

                试卷内容：
                $paperText

                错题识别标准（必须满足以下任一条件）：
                1. 题目旁边有明确的×标记、叉号、错号
                2. 有红笔批改、圈出错误、划线标记
                3. 有扣分标记（如：-2分、扣3分）
                4. 有"错误"、"×"等文字标注
                5. 答案被划掉或修改

                严格要求：
                - ✅ 只提取有明确错误标记的题目
                - ❌ 绝对不要包含答对的题目（有✓、√、对号的）
                - ❌ 绝对不要包含没有任何标记的题目
                - ❌ 如果无法确定是否为错题，宁可不包含
                - ❌ 不要猜测或推断，必须有明确证据

                请以JSON格式返回：
                {
                  "subject": "学科名称（数学/物理/化学/英语等）",
                  "wrongQuestions": [
                    {
                      "questionNumber": "题号",
                      "questionText": "题目内容（完整题干）",
                      "correctAnswer": "正确答案",
                      "studentAnswer": "学生的错误答案（如果能识别到）",
                      "knowledgePoints": ["知识点1", "知识点2"],
                      "difficulty": 3
                    }
                  ]
                }

                注意：如果试卷中没有明确标记的错题，请返回空的wrongQuestions数组。
            """.trimIndent()

            val aiResponse = qwenService.chat(analysisPrompt)
            val jsonResult = parseAIResponse(aiResponse)

            if (jsonResult == null) {
                return ExamPaperAnalysisResult(
                    success = false,
                    errorMessage = "AI分析失败，请重试"
                )
            }

            val subject = jsonResult.optString("subject", "未知学科")
            val wrongQuestionsArray = jsonResult.optJSONArray("wrongQuestions") ?: JSONArray()
            val wrongQuestions = mutableListOf<WrongQuestionInfo>()

            for (i in 0 until wrongQuestionsArray.length()) {
                val questionObj = wrongQuestionsArray.getJSONObject(i)
                val knowledgePointsArray = questionObj.optJSONArray("knowledgePoints") ?: JSONArray()
                val knowledgePoints = mutableListOf<String>()
                for (j in 0 until knowledgePointsArray.length()) {
                    knowledgePoints.add(knowledgePointsArray.getString(j))
                }

                wrongQuestions.add(
                    WrongQuestionInfo(
                        questionNumber = questionObj.optString("questionNumber", ""),
                        questionText = questionObj.optString("questionText", ""),
                        correctAnswer = questionObj.optString("correctAnswer", ""),
                        studentAnswer = questionObj.optString("studentAnswer", ""),
                        knowledgePoints = knowledgePoints,
                        difficulty = questionObj.optInt("difficulty", 3)
                    )
                )
            }

            ExamPaperAnalysisResult(
                success = true,
                subject = subject,
                wrongQuestions = wrongQuestions,
                imagePath = imagePath
            )
        } catch (e: Exception) {
            Log.e("ExamPaperAnalysis", "分析试卷失败", e)
            ExamPaperAnalysisResult(
                success = false,
                errorMessage = e.message ?: "未知错误"
            )
        }
    }

    /**
     * 生成举一反三的测试题
     */
    suspend fun generateSimilarQuestions(
        wrongQuestion: WrongQuestionInfo,
        count: Int = 3
    ): List<SimilarQuestionInfo> {
        return try {
            val prompt = """
                基于以下错题，生成${count}道类似的练习题，用于巩固相关知识点。

                原题：${wrongQuestion.questionText}
                正确答案：${wrongQuestion.correctAnswer}
                知识点：${wrongQuestion.knowledgePoints.joinToString(", ")}

                要求：
                1. 题目难度相同或略简单
                2. 考察相同的知识点
                3. 题目形式可以变化
                4. 提供详细的解题步骤

                请以JSON格式返回：
                {
                  "questions": [
                    {
                      "questionText": "题目内容",
                      "answer": "答案",
                      "steps": ["步骤1", "步骤2"],
                      "knowledgePoints": ["知识点1"]
                    }
                  ]
                }
            """.trimIndent()

            val aiResponse = qwenService.chat(prompt)
            val jsonResult = parseAIResponse(aiResponse)
            val questionsArray = jsonResult?.optJSONArray("questions") ?: JSONArray()
            val similarQuestions = mutableListOf<SimilarQuestionInfo>()

            for (i in 0 until questionsArray.length()) {
                val questionObj = questionsArray.getJSONObject(i)
                val stepsArray = questionObj.optJSONArray("steps") ?: JSONArray()
                val steps = mutableListOf<String>()
                for (j in 0 until stepsArray.length()) {
                    steps.add(stepsArray.getString(j))
                }

                val kpArray = questionObj.optJSONArray("knowledgePoints") ?: JSONArray()
                val kps = mutableListOf<String>()
                for (j in 0 until kpArray.length()) {
                    kps.add(kpArray.getString(j))
                }

                similarQuestions.add(
                    SimilarQuestionInfo(
                        questionText = questionObj.optString("questionText", ""),
                        answer = questionObj.optString("answer", ""),
                        steps = steps,
                        knowledgePoints = kps
                    )
                )
            }

            similarQuestions
        } catch (e: Exception) {
            Log.e("ExamPaperAnalysis", "生成举一反三题目失败", e)
            emptyList()
        }
    }

    /**
     * 解析AI返回的JSON响应
     */
    private fun parseAIResponse(response: String): JSONObject? {
        return try {
            // 尝试直接解析
            JSONObject(response)
        } catch (e: Exception) {
            try {
                // 尝试提取JSON部分
                val jsonStart = response.indexOf("{")
                val jsonEnd = response.lastIndexOf("}") + 1
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    JSONObject(response.substring(jsonStart, jsonEnd))
                } else {
                    null
                }
            } catch (e2: Exception) {
                Log.e("ExamPaperAnalysis", "解析AI响应失败", e2)
                null
            }
        }
    }
}

/**
 * 试卷分析结果
 */
data class ExamPaperAnalysisResult(
    val success: Boolean,
    val subject: String = "",
    val wrongQuestions: List<WrongQuestionInfo> = emptyList(),
    val imagePath: String = "",
    val errorMessage: String? = null
)

/**
 * 错题信息
 */
data class WrongQuestionInfo(
    val questionNumber: String,
    val questionText: String,
    val correctAnswer: String,
    val studentAnswer: String,
    val knowledgePoints: List<String>,
    val difficulty: Int
)

/**
 * 举一反三题目信息
 */
data class SimilarQuestionInfo(
    val questionText: String,
    val answer: String,
    val steps: List<String>,
    val knowledgePoints: List<String>
)

