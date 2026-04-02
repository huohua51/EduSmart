package com.edusmart.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.edusmart.app.feature.ar.ARScreen
import com.edusmart.app.feature.auth.AuthViewModel
import com.edusmart.app.feature.auth.LoginScreen
import com.edusmart.app.feature.home.HomeScreen
import com.edusmart.app.feature.note.NoteScreen
import com.edusmart.app.feature.profile.ProfileScreen
import com.edusmart.app.feature.radar.RadarScreenNew
import com.edusmart.app.feature.scan.ScanScreen
import com.edusmart.app.feature.scan.ScanResultDetailScreen
import com.edusmart.app.feature.speaking.SpeakingScreen
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edusmart.app.feature.auth.AuthViewModelFactory
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Login : Screen("login", "登录", Icons.Default.Lock)
    object Home : Screen("home", "首页", Icons.Default.Home)
    object Scan : Screen("scan", "拍照识题", Icons.Default.CameraAlt)
    object AR : Screen("ar", "AR空间", Icons.Default.Visibility)
    object Note : Screen("note", "智能笔记", Icons.Default.EditNote)
    object Speaking : Screen("speaking", "口语私教", Icons.Default.RecordVoiceOver)
    object Radar : Screen("radar", "知识雷达", Icons.Default.Radar)
    object Profile : Screen("profile", "个人中心", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry.value?.destination
    val context = LocalContext.current
    // 使用腾讯云开发（已优化）
    // 如需切换：useAliyun = true（阿里云）或 useLeanCloud = true（LeanCloud）
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(
            context = context,
            useCloudBase = true  // 使用腾讯云开发
        )
    )
    
    // 跟踪拍照识题页面是否正在拍照（用于隐藏底部导航栏）
    var isScanning by remember { mutableStateOf(false) }
    
    // 检查认证状态
    val authState by authViewModel.authState.collectAsState()
    val isAuthenticated = authState.isAuthenticated
    
    // 根据认证状态决定启动页面
    val startDestination = if (isAuthenticated) Screen.Home.route else Screen.Login.route

    Scaffold(
        bottomBar = {
            // 如果已登录且不在登录/个人主页，且不在拍照，显示底部导航栏
            if (isAuthenticated && !isScanning && 
                currentDestination?.route != Screen.Login.route && 
                currentDestination?.route != Screen.Profile.route) {
                // 底部导航栏 - 清新简约风格
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                        ),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    color = Color.White, // 纯白背景
                    tonalElevation = 0.dp
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp
                    ) {
                    val screens = listOf(
                        Screen.Home,
                        Screen.AR,
                        Screen.Note,
                        Screen.Speaking,
                        Screen.Radar
                    )
                    
                    screens.forEach { screen ->
                        // 特殊处理：scan_result 路由也应该高亮 Scan 标签
                        val isSelected = if (screen.route == "scan") {
                            currentDestination?.route?.startsWith("scan") == true
                        } else {
                            currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        }
                        
                        NavigationBarItem(
                            icon = { 
                                Icon(
                                    imageVector = screen.icon, 
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(24.dp)
                                ) 
                            },
                            label = { 
                                Text(
                                    text = screen.title,
                                    style = MaterialTheme.typography.labelSmall
                                ) 
                            },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF35568a), // 选中状态：深蓝色
                                selectedTextColor = Color(0xFF35568a), // 选中状态：深蓝色
                                indicatorColor = Color(0xFFf1d6d6), // 淡粉色指示器背景
                                unselectedIconColor = Color(0xFF8b9bc1), // 未选中状态：蓝紫色
                                unselectedTextColor = Color(0xFF8b9bc1) // 未选中状态：蓝紫色
                            )
                        )
                    }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    viewModel = authViewModel
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    navController = navController,
                    authViewModel = authViewModel
                )
            }
            composable(Screen.Scan.route) {
                ScanScreen(
                    navController = navController,
                    onScanningStateChange = { isScanning = it },
                    autoStartScanning = false
                )
            }
            composable(
                route = "scan?autoStart={autoStart}",
                arguments = listOf(navArgument("autoStart") {
                    type = NavType.BoolType
                    defaultValue = false
                })
            ) { backStackEntry ->
                val autoStart = backStackEntry.arguments?.getBoolean("autoStart") ?: false
                ScanScreen(
                    navController = navController,
                    onScanningStateChange = { isScanning = it },
                    autoStartScanning = autoStart
                )
            }
            composable(
                route = "scan_result/{imagePath}",
                arguments = listOf(navArgument("imagePath") { type = NavType.StringType })
            ) { backStackEntry ->
                val encodedPath = backStackEntry.arguments?.getString("imagePath") ?: ""
                val imagePath = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.toString())
                ScanResultDetailScreen(
                    navController = navController,
                    imagePath = imagePath
                )
            }
            composable(Screen.AR.route) { ARScreen() }
            composable(Screen.Note.route) { NoteScreen(navController) }
            composable(Screen.Speaking.route) { SpeakingScreen() }
            composable(Screen.Radar.route) { RadarScreenNew() }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    authViewModel = authViewModel
                )
            }
        }
    }
}

