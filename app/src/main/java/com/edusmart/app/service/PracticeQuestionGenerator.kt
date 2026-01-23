package com.edusmart.app.service

import android.util.Log
import org.json.JSONArray

/**
 * 生成举一反三的练习题（选择题或填空题格式）
 */
suspend fun ExamPaperAnalysisService.generatePracticeQuestions(
    wrongQuestion: WrongQuestionInfo,
    count: Int = 3
): List<PracticeQuestionInfo> {
    return try {
        val prompt = """
            基于以下错题，生成${count}道类似的练习题，用于巩固相关知识点。

            原题：${wrongQuestion.questionText}
            正确答案：${wrongQuestion.correctAnswer}
            知识点：${wrongQuestion.knowledgePoints.joinToString(", ")}

            要求：
            1. 题目难度相同或略简单
            2. 考察相同的知识点
            3. 生成选择题（ABCD四个选项）或填空题
            4. 提供详细的解题步骤

            请以JSON格式返回：
            {
              "questions": [
                {
                  "questionText": "题目内容",
                  "questionType": "choice",
                  "options": ["A. 选项1", "B. 选项2", "C. 选项3", "D. 选项4"],
                  "correctAnswer": "A",
                  "steps": ["步骤1", "步骤2"],
                  "knowledgePoints": ["知识点1"]
                }
              ]
            }

            注意：questionType 可以是 "choice"（选择题）或 "fill"（填空题）
            如果是填空题，options 字段可以为空数组
        """.trimIndent()

        val qwenService = QwenAIService()
        val aiResponse = qwenService.chat(prompt)
        val jsonResult = parseAIResponseHelper(aiResponse)
        val questionsArray = jsonResult?.optJSONArray("questions") ?: JSONArray()
        val practiceQuestions = mutableListOf<PracticeQuestionInfo>()

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

            val optionsArray = questionObj.optJSONArray("options") ?: JSONArray()
            val options = mutableListOf<String>()
            for (j in 0 until optionsArray.length()) {
                options.add(optionsArray.getString(j))
            }

            practiceQuestions.add(
                PracticeQuestionInfo(
                    questionText = questionObj.optString("questionText", ""),
                    questionType = questionObj.optString("questionType", "fill"),
                    options = options,
                    correctAnswer = questionObj.optString("correctAnswer", ""),
                    steps = steps,
                    knowledgePoints = kps
                )
            )
        }

        practiceQuestions
    } catch (e: Exception) {
        Log.e("PracticeQuestionGen", "生成练习题失败", e)
        emptyList()
    }
}

private fun parseAIResponseHelper(response: String): org.json.JSONObject? {
    return try {
        org.json.JSONObject(response)
    } catch (e: Exception) {
        try {
            val jsonStart = response.indexOf("{")
            val jsonEnd = response.lastIndexOf("}") + 1
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                org.json.JSONObject(response.substring(jsonStart, jsonEnd))
            } else null
        } catch (e2: Exception) {
            null
        }
    }
}
