package com.edusmart.app.feature.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.edusmart.app.ui.components.BeautyFeatureCard
import com.edusmart.app.ui.theme.*

data class FeatureCard(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: String,
    val gradientColors: List<Color> = listOf(Gray100, Gray50)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val features = listOf(
        FeatureCard(
            "拍照识题",
            "拍摄题目，AI自动识别并匹配答案",
            Icons.Default.CameraAlt,
            "scan",
            gradientColors = listOf(
                Color(0xFFE3F2FD),
                Color(0xFFBBDEFB)
            )
        ),
        FeatureCard(
            "AR知识空间",
            "3D动态模型，沉浸式学习",
            Icons.Default.Visibility,
            "ar",
            gradientColors = listOf(
                Color(0xFFF3E5F5),
                Color(0xFFE1BEE7)
            )
        ),
        FeatureCard(
            "智能笔记",
            "自动OCR+语音转写，生成结构化笔记",
            Icons.Default.EditNote,
            "note",
            gradientColors = listOf(
                Color(0xFFE8F5E9),
                Color(0xFFC8E6C9)
            )
        ),
        FeatureCard(
            "AI口语私教",
            "情景对话模拟，实时发音纠错",
            Icons.Default.RecordVoiceOver,
            "speaking",
            gradientColors = listOf(
                Color(0xFFFFF3E0),
                Color(0xFFFFE0B2)
            )
        ),
        FeatureCard(
            "知识雷达",
            "快速测评，精准定位薄弱点",
            Icons.Default.Radar,
            "radar",
            gradientColors = listOf(
                Color(0xFFFCE4EC),
                Color(0xFFF8BBD0)
            )
        )
    )
    
    // 背景渐变动画
    val infiniteTransition = rememberInfiniteTransition(label = "background_animation")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_offset"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 16.dp)
        ) {
            // 标题区域
            Column(
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Text(
                    text = "EduSmart",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "智能学习助手",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                )
                            )
                        ),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "✨ 您的专属学习伙伴",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            
            // 功能卡片网格
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(features) { feature ->
                    BeautyFeatureCard(
                        title = feature.title,
                        description = feature.description,
                        icon = feature.icon,
                        onClick = { navController.navigate(feature.route) },
                        gradientColors = feature.gradientColors
                    )
                }
            }
        }
    }
}

