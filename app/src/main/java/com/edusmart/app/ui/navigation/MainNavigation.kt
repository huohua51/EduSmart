package com.edusmart.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.edusmart.app.feature.home.HomeScreen
import com.edusmart.app.feature.note.NoteScreen
import com.edusmart.app.feature.radar.RadarScreenNew
import com.edusmart.app.feature.scan.ScanScreen
import com.edusmart.app.feature.scan.ScanResultDetailScreen
import com.edusmart.app.feature.speaking.SpeakingScreen
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "首页", Icons.Default.Home)
    object Scan : Screen("scan", "拍照识题", Icons.Default.CameraAlt)
    object AR : Screen("ar", "AR知识空间", Icons.Default.Visibility)
    object Note : Screen("note", "智能笔记", Icons.Default.EditNote)
    object Speaking : Screen("speaking", "口语私教", Icons.Default.RecordVoiceOver)
    object Radar : Screen("radar", "知识雷达", Icons.Default.Radar)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry.value?.destination
    
    // 跟踪拍照识题页面是否正在拍照（用于隐藏底部导航栏）
    var isScanning by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            // 如果正在拍照，隐藏底部导航栏
            if (!isScanning) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    val screens = listOf(
                        Screen.Home,
                        Screen.Scan,
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
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Scan.route) {
                ScanScreen(
                    navController = navController,
                    onScanningStateChange = { isScanning = it }
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
            composable(Screen.Note.route) { NoteScreen() }
            composable(Screen.Speaking.route) { SpeakingScreen() }
            composable(Screen.Radar.route) { RadarScreenNew() }
        }
    }
}

