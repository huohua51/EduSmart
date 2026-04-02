package com.edusmart.app.feature.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import com.edusmart.app.R
import androidx.navigation.NavController
import com.edusmart.app.feature.auth.AuthViewModel
import com.edusmart.app.ui.navigation.Screen
import com.edusmart.app.ui.theme.*
import com.edusmart.app.utils.LocalAvatarManager
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsState()
    val user = authState.currentUser
    val context = LocalContext.current
    
    var showAIChatDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // 获取用户头像（优先本地，其次云端）
    val avatarUrl by remember(user?.userId) {
        mutableStateOf(
            user?.userId?.let { userId ->
                LocalAvatarManager.getLocalAvatarUri(context, userId) ?: user?.avatarUrl
            } ?: user?.avatarUrl
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 主卡片 - 使用新配色渐变
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .shadow(12.dp, RoundedCornerShape(32.dp)),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    SecondaryBlue,
                                    PrimaryBlue
                                )
                            )
                        )
                ) {
                    // 装饰性圆点
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp)
                    ) {
                        repeat(6) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .offset(
                                        x = (40 + it * 50).dp,
                                        y = (20 + it * 30).dp
                                    )
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.3f))
                            )
                        }
                    }
                    
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 顶部栏 - 左上角个人中心图标
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, start = 20.dp, end = 20.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 个人中心按钮（显示头像或默认图标）
                            IconButton(
                                onClick = { navController.navigate(Screen.Profile.route) },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Color.White.copy(alpha = 0.95f),
                                        CircleShape
                                    )
                            ) {
                                if (!avatarUrl.isNullOrBlank()) {
                                    // 有头像时显示头像
                                    val imageData: Any = if (avatarUrl!!.startsWith("/")) {
                                        File(avatarUrl!!)
                                    } else {
                                        avatarUrl!!
                                    }
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(imageData)
                                            .crossfade(true)
                                            .memoryCachePolicy(CachePolicy.DISABLED)
                                            .build(),
                                        contentDescription = "头像",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    // 没有头像时显示默认图标
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "个人中心",
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(40.dp))
                        
                        // 问候语 - 渐显动画
                        val username = user?.username ?: "同学"
                        val greetingAlpha by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = 1000,
                                easing = FastOutSlowInEasing
                            ),
                            label = "greeting_alpha"
                        )
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.alpha(greetingAlpha)
                        ) {
                            // HELLO! 用户名
                            Text(
                                text = "HELLO! $username",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            // 准备好开始今天的学习了吗？
                            Text(
                                text = "准备好开始今天的学习了吗？",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // 三个圆形按钮
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 拍照按钮 - 淡粉色
                            IconButton(
                                onClick = { navController.navigate(Screen.Note.route) },
                                modifier = Modifier
                                    .size(64.dp)
                                    .shadow(6.dp, CircleShape)
                                    .background(LightPink, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "拍照",
                                    tint = AccentCoral,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            
                            // 麦克风按钮（大）- 深蓝色主色
                            IconButton(
                                onClick = { navController.navigate(Screen.Speaking.route) },
                                modifier = Modifier
                                    .size(84.dp)
                                    .shadow(10.dp, CircleShape)
                                    .background(Color.White, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "语音",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            
                            // 视频按钮 - 淡蓝灰色
                            IconButton(
                                onClick = { navController.navigate(Screen.AR.route) },
                                modifier = Modifier
                                    .size(64.dp)
                                    .shadow(6.dp, CircleShape)
                                    .background(CardBackground, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "视频",
                                    tint = SecondaryBlue,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // 底部信息
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = LightPink,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "今日已学习 ${(0..100).random()} 分钟",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                            
                            Text(
                                text = "Powered by AI",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // AI搜索输入框
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { 
                    Text(
                        "问我任何问题...",
                        color = TextHint
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SecondaryBlue,
                    unfocusedBorderColor = CardBackground,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                showAIChatDialog = true
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(PrimaryBlue, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "发送",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }
        
        // 装饰性图片 - 左下方
        Image(
            painter = painterResource(id = R.drawable.home_decoration),
            contentDescription = "装饰图片",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(150.dp)
                .padding(start = 8.dp, bottom = 10.dp)
                .alpha(0.9f),
            contentScale = ContentScale.Fit
        )
        
        // AI聊天对话框
        if (showAIChatDialog) {
            com.edusmart.app.feature.note.AIChatDialog(
                onDismiss = { 
                    showAIChatDialog = false
                    searchQuery = ""
                },
                qwenAIService = remember { com.edusmart.app.service.QwenAIService() },
                initialMessage = searchQuery
            )
        }
    }
}
