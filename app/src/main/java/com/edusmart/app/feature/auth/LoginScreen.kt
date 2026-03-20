package com.edusmart.app.feature.auth

import androidx.compose.foundation.background
import com.edusmart.app.ui.theme.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    
    val authState by viewModel.authState.collectAsState()
    
    // 监听登录成功
    LaunchedEffect(authState.isAuthenticated) {
        if (authState.isAuthenticated) {
            onLoginSuccess()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // 纯白背景
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo和标题
            Text(
                text = "EduSmart",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue, // 清爽蓝色
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "智能学习助手",
                style = MaterialTheme.typography.titleLarge,
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 48.dp)
            )
            
            // 登录卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardBackground // 清新淡蓝色背景
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 标题
                    Text(
                        text = if (isLoginMode) "登录" else "注册",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // 用户名输入（仅注册模式）
                    if (!isLoginMode) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("用户名", color = Color.Black) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Black.copy(alpha = 0.6f))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = Color.Black.copy(alpha = 0.3f)
                            )
                        )
                    }
                    
                    // 邮箱输入
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("邮箱", color = Color.Black) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = Color.Black.copy(alpha = 0.6f))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = Color.Black.copy(alpha = 0.3f)
                        )
                    )
                    
                    // 密码输入
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("密码", color = Color.Black) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Black.copy(alpha = 0.6f))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = Color.Black.copy(alpha = 0.3f)
                        )
                    )
                    
                    // 错误提示
                    authState.errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 登录/注册按钮
                    Button(
                        onClick = {
                            if (isLoginMode) {
                                viewModel.login(email, password)
                            } else {
                                if (username.isNotBlank()) {
                                    viewModel.register(email, password, username)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue, // 清爽蓝色
                            contentColor = Color.White
                        ),
                        enabled = !authState.isLoading && email.isNotBlank() && password.isNotBlank() && (isLoginMode || username.isNotBlank())
                    ) {
                        if (authState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = if (isLoginMode) "登录" else "注册",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // 切换登录/注册模式
                    TextButton(
                        onClick = {
                            isLoginMode = !isLoginMode
                            viewModel.clearError()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isLoginMode) "还没有账号？立即注册" else "已有账号？立即登录",
                            textAlign = TextAlign.Center,
                            color = Color.Black.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

