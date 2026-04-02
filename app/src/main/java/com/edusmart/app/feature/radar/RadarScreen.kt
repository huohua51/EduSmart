package com.edusmart.app.feature.radar

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RadarScreen() {
    var isTesting by remember { mutableStateOf(false) }
    var showWrongQuestionScreen by remember { mutableStateOf(false) }

    if (showWrongQuestionScreen) {
        WrongQuestionScreen(onBack = { showWrongQuestionScreen = false })
    } else {
        Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "学科知识雷达",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "快速测评，精准定位薄弱点",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        // 错题本入口按钮
        OutlinedButton(
            onClick = {
                showWrongQuestionScreen = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "错题本",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("📚 我的错题本")
        }

        Button(
            onClick = {
                isTesting = true
                // TODO: 启动快速测评
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("10分钟快速测评")
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "功能说明",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("• 拍摄最近3次考试成绩单，AI分析失分率")
                Text("• 自适应出题，精准定位薄弱知识点")
                Text("• 生成个性化7天突破计划")
                Text("• 匹配同校同弱项学习伙伴")
            }
        }
        
        if (isTesting) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("测评进行中...")
                    Text(
                        text = "请拍摄成绩单或回答题目",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        // TODO: 雷达图可视化
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "知识掌握度雷达图",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "（图表开发中）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
    }
}

