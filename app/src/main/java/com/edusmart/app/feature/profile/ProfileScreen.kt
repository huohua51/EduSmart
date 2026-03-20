package com.edusmart.app.feature.profile

import android.net.Uri
import com.edusmart.app.ui.theme.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import com.edusmart.app.feature.auth.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val user = authState.currentUser
    val context = LocalContext.current
    
    var isEditing by remember { mutableStateOf(false) }
    var editedUsername by remember { mutableStateOf(user?.username ?: "") }
    var editedAvatarUrl by remember { mutableStateOf(user?.avatarUrl ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingAvatar by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var avatarRefreshKey by remember { mutableStateOf(0) } // 用于强制刷新头像
    
    // 使用正确的协程作用域
    val scope = rememberCoroutineScope()
    
    // 当用户信息更新时，同步编辑状态
    // 【优先】从本地加载头像
    LaunchedEffect(user) {
        if (user != null) {
            editedUsername = user.username
            
            // 优先从本地加载头像
            val currentUserId = user.userId
            val localAvatarUri = withContext(Dispatchers.IO) {
                if (com.edusmart.app.utils.LocalAvatarManager.hasLocalAvatar(context, currentUserId)) {
                    android.util.Log.d("ProfileScreen", "📁 检测到本地头像，优先使用")
                    com.edusmart.app.utils.LocalAvatarManager.getLocalAvatarUri(context, currentUserId)
                } else {
                    android.util.Log.d("ProfileScreen", "☁️ 本地无头像，使用云端URL")
                    null
                }
            }
            
            // 如果本地有头像，优先使用本地的；否则使用云端的
            editedAvatarUrl = localAvatarUri ?: user.avatarUrl ?: ""
        }
    }
    
    // 从相册选择图片并上传到云存储
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { imageUri ->
            selectedImageUri = imageUri
            isUploadingAvatar = true
            uploadError = null
            
            // 在协程中处理上传
            scope.launch {
                try {
                    android.util.Log.d("ProfileScreen", "开始处理图片: $imageUri")
                    
                    // 读取并压缩图片
                    val imageBytes = withContext(Dispatchers.IO) {
                        val inputStream = context.contentResolver.openInputStream(imageUri)
                        if (inputStream == null) {
                            android.util.Log.e("ProfileScreen", "无法打开图片输入流")
                            throw Exception("无法读取图片，请重试")
                        }
                        
                        try {
                            // 压缩图片到合适大小（最大 100KB，避免 CloudBase 413 错误）
                            compressImage(inputStream, maxSizeKB = 100)
                        } catch (e: Exception) {
                            android.util.Log.e("ProfileScreen", "压缩图片失败", e)
                            throw Exception("图片处理失败: ${e.message}")
                        }
                    }
                    
                    android.util.Log.d("ProfileScreen", "压缩后图片大小: ${imageBytes.size} bytes (${imageBytes.size / 1024}KB)")
                    
                    val currentUserId = user?.userId
                    if (currentUserId.isNullOrEmpty()) {
                        android.util.Log.e("ProfileScreen", "用户ID为空")
                        isUploadingAvatar = false
                        uploadError = "用户未登录，请重新登录"
                        return@launch
                    }
                    
                    // 【优先】先保存到本地
                    android.util.Log.d("ProfileScreen", "📁 优先保存头像到本地")
                    val localPath = withContext(Dispatchers.IO) {
                        com.edusmart.app.utils.LocalAvatarManager.saveAvatarLocally(
                            context,
                            currentUserId,
                            imageBytes
                        )
                    }
                    
                    if (localPath != null) {
                        // 本地保存成功，立即更新显示
                        val localUri = com.edusmart.app.utils.LocalAvatarManager.getLocalAvatarUri(
                            context,
                            currentUserId
                        )
                        
                        // 更新URL和刷新key
                        editedAvatarUrl = localUri ?: ""
                        avatarRefreshKey++ // 强制AsyncImage重新加载
                        
                        android.util.Log.d("ProfileScreen", "✅ 头像已保存到本地，立即显示")
                        android.util.Log.d("ProfileScreen", "本地URI长度: ${editedAvatarUrl.length}")
                        android.util.Log.d("ProfileScreen", "刷新Key: $avatarRefreshKey")
                        uploadError = null
                        
                        // 异步上传到云端（不阻塞UI）
                        scope.launch {
                            try {
                                android.util.Log.d("ProfileScreen", "☁️ 开始后台上传到云端")
                                val result = authViewModel.uploadAvatar(currentUserId, imageBytes)
                                
                                if (result.isSuccess) {
                                    val cloudUrl = result.getOrNull()
                                    android.util.Log.d("ProfileScreen", "✅ 云端上传成功")
                                    android.util.Log.d("ProfileScreen", "云端URL长度: ${cloudUrl?.length ?: 0}")
                                    // 保持使用本地URI，不更新为云端的（因为云端也是Base64）
                                    // editedAvatarUrl = cloudUrl
                                } else {
                                    android.util.Log.w("ProfileScreen", "⚠️ 云端上传失败，但本地已保存")
                                }
                            } catch (e: Exception) {
                                android.util.Log.w("ProfileScreen", "⚠️ 云端上传异常，但本地已保存", e)
                            }
                        }
                    } else {
                        // 本地保存失败，尝试直接上传到云端
                        android.util.Log.w("ProfileScreen", "⚠️ 本地保存失败，尝试直接上传云端")
                        val result = authViewModel.uploadAvatar(currentUserId, imageBytes)
                        
                        if (result.isSuccess) {
                            val avatarUrl = result.getOrNull()
                            if (avatarUrl.isNullOrEmpty()) {
                                uploadError = "上传成功但未获取到头像地址"
                            } else {
                                android.util.Log.d("ProfileScreen", "✅ 云端上传成功: $avatarUrl")
                                editedAvatarUrl = avatarUrl
                                uploadError = null
                            }
                        } else {
                            val error = result.exceptionOrNull()
                            android.util.Log.e("ProfileScreen", "❌ 云端上传失败", error)
                            uploadError = error?.message ?: "上传失败，请重试"
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ProfileScreen", "❌ 头像上传过程出错", e)
                    uploadError = e.message ?: "上传失败，请重试"
                } finally {
                    isUploadingAvatar = false
                }
            }
        }
    }
    
    // 极简白底设计
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人中心", color = Color.Black) },
                actions = {
                    if (isEditing) {
                        TextButton(onClick = {
                            isEditing = false
                            editedUsername = user?.username ?: ""
                            editedAvatarUrl = user?.avatarUrl ?: ""
                        }) {
                            Text("取消")
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = {
                        if (isEditing) {
                            // 保存更改
                            authViewModel.updateUserInfo(
                                username = if (editedUsername != user?.username) editedUsername else null,
                                avatarUrl = if (editedAvatarUrl != user?.avatarUrl) editedAvatarUrl else null
                            )
                            isEditing = false
                        } else {
                            isEditing = true
                        }
                    }) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = if (isEditing) "保存" else "编辑"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                
                // 头像
                Box(
                    modifier = Modifier.size(120.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(enabled = isEditing && !isUploadingAvatar) {
                                if (isEditing && !isUploadingAvatar) {
                                    imagePickerLauncher.launch("image/*")
                                }
                            },
                        shape = CircleShape,
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUploadingAvatar) {
                                // 上传中显示加载指示器
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = Color.Black
                                )
                            } else {
                                // 优先显示编辑中的头像（包括新上传的），否则显示用户原头像
                                val avatarUrlToShow = if (editedAvatarUrl.isNotBlank()) {
                                    editedAvatarUrl
                                } else {
                                    user?.avatarUrl
                                }
                                
                                android.util.Log.d("ProfileScreen", "🖼️ 当前显示路径: $avatarUrlToShow, RefreshKey: $avatarRefreshKey")
                                
                                if (avatarUrlToShow != null && avatarUrlToShow.isNotBlank()) {
                                    // 判断是本地文件路径还是网络URL
                                    val imageData: Any = if (avatarUrlToShow.startsWith("/")) {
                                        // 本地文件路径
                                        File(avatarUrlToShow)
                                    } else {
                                        // 网络URL
                                        avatarUrlToShow
                                    }
                                    
                                    // 使用key参数强制刷新，禁用缓存
                                    key(avatarRefreshKey) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(imageData)
                                                .crossfade(true)
                                                .memoryCachePolicy(CachePolicy.DISABLED) // 禁用内存缓存
                                                .diskCachePolicy(CachePolicy.DISABLED) // 禁用磁盘缓存
                                                .build(),
                                            contentDescription = "头像",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "默认头像",
                                        modifier = Modifier.size(64.dp),
                                        tint = Color.Black
                                    )
                                }
                            }
                        }
                    }
                    
                    // 编辑图标
                    if (isEditing && !isUploadingAvatar) {
                        FloatingActionButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(40.dp),
                            containerColor = Color.Black
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "更换头像",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                // 上传错误提示
                if (uploadError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LightPink
                    ) {
                        Text(
                            text = uploadError!!,
                            color = AccentCoral,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 用户名
                if (isEditing) {
                    OutlinedTextField(
                        value = editedUsername,
                        onValueChange = { editedUsername = it },
                        label = { Text("用户名") },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                } else {
                    Text(
                        text = user?.username ?: "未设置用户名",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 邮箱
                Text(
                    text = user?.email ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // 信息卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CardBackground // 清新淡蓝色背景
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 用户ID
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = Color.Black.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "用户ID",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Black
                                )
                            }
                            Text(
                                text = user?.userId?.take(8) ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black.copy(alpha = 0.6f)
                            )
                        }
                        
                        Divider()
                        
                        // 注册时间
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = Color.Black.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "注册时间",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Black
                                )
                            }
                            Text(
                                text = user?.createdAt?.let {
                                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                        .format(java.util.Date(it))
                                } ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // 登出按钮
                Button(
                    onClick = {
                        authViewModel.logout()
                        onLogout()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue, // 清爽蓝色
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "退出登录",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * 压缩图片到指定大小（KB）
 * @param inputStream 原始图片输入流
 * @param maxSizeKB 最大文件大小（KB），默认 100KB（CloudBase限制）
 * @return 压缩后的图片字节数组
 */
private fun compressImage(inputStream: InputStream, maxSizeKB: Int = 100): ByteArray {
    // 读取原始图片
    val originalBitmap = BitmapFactory.decodeStream(inputStream)
    inputStream.close()
    
    if (originalBitmap == null) {
        throw Exception("无法解码图片")
    }
    
    // 计算压缩后的尺寸（头像最大 300x300，保持宽高比）
    val maxDimension = 300
    val width = originalBitmap.width
    val height = originalBitmap.height
    val scale = if (width > height) {
        maxDimension.toFloat() / width
    } else {
        maxDimension.toFloat() / height
    }.coerceAtMost(1.0f)
    
    val scaledWidth = (width * scale).toInt()
    val scaledHeight = (height * scale).toInt()
    
    android.util.Log.d("ProfileScreen", "原始尺寸: ${width}x${height}, 缩放后: ${scaledWidth}x${scaledHeight}")
    
    // 缩放图片
    val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
    originalBitmap.recycle()
    
    // 压缩质量（从 80 开始，逐步降低）
    var quality = 80
    val maxSizeBytes = maxSizeKB * 1024
    val outputStream = ByteArrayOutputStream()
    
    do {
        outputStream.reset()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        android.util.Log.d("ProfileScreen", "压缩尝试: 质量=$quality, 大小=${outputStream.size() / 1024}KB")
        quality -= 10
    } while (outputStream.size() > maxSizeBytes && quality > 10)
    
    scaledBitmap.recycle()
    
    android.util.Log.d("ProfileScreen", "图片压缩完成: ${outputStream.size()} bytes (${outputStream.size() / 1024}KB), 质量: ${quality + 10}")
    
    return outputStream.toByteArray()
}

/**
 * 复制流
 */
private fun copyStream(input: InputStream, output: OutputStream) {
    val buffer = ByteArray(1024)
    var bytesRead: Int
    while (input.read(buffer).also { bytesRead = it } != -1) {
        output.write(buffer, 0, bytesRead)
    }
}

