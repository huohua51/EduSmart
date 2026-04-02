package com.edusmart.app.feature.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.edusmart.app.service.AIAnswerResult

@Composable
fun ScanResultScreen(
    questionText: String,
    aiAnswer: AIAnswerResult?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "识别结果",
            style = MaterialTheme.typography.headlineMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "题目",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = questionText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "答案",
                    style = MaterialTheme.typography.titleMedium
                )

                if (aiAnswer == null) {
                    Text(
                        text = "暂无答案",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = aiAnswer.answer,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    if (aiAnswer.steps.isNotEmpty()) {
                        Text(
                            text = "解题步骤",
                            style = MaterialTheme.typography.titleSmall
                        )
                        aiAnswer.steps.forEachIndexed { index, step ->
                            Text(
                                text = "${index + 1}. $step",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    if (aiAnswer.explanation.isNotBlank()) {
                        Text(
                            text = "思路分析",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = aiAnswer.explanation,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

