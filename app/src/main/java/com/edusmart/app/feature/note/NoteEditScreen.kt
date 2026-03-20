package com.edusmart.app.feature.note

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.edusmart.app.data.entity.NoteEntity
import com.edusmart.app.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

// 复制流的辅助函数
private fun copyStream(input: InputStream, output: OutputStream) {
    val buffer = ByteArray(1024)
    var bytesRead: Int
    while (input.read(buffer).also { bytesRead = it } != -1) {
        output.write(buffer, 0, bytesRead)
    }
}

// 编辑工具类型
enum class EditToolType {
    BOLD, ITALIC, UNDERLINE, STRIKETHROUGH,
    HEADING, BULLET_LIST, NUMBERED_LIST, CHECKBOX,
    QUOTE, CODE, DIVIDER, TIMESTAMP, HIGHLIGHT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    note: NoteEntity?,
    subjects: List<String>,
    onSave: (String, String, String, List<String>) -> Unit,
    onCancel: () -> Unit,
    onGenerateSubject: suspend (String, String?) -> String = { _, _ -> "其他" }
) {
    val context = LocalContext.current
    
    // 初始化状态变量
    var title by remember { mutableStateOf(note?.title ?: "") }
    // 确保科目有默认值，如果列表为空则使用"其他"
    var subject by remember { 
        mutableStateOf(note?.subject ?: subjects.firstOrNull() ?: if (subjects.isEmpty()) "其他" else "") 
    }
    var contentFieldValue by remember { 
        mutableStateOf(TextFieldValue(note?.content ?: "")) 
    }
    var imagePaths by remember { mutableStateOf(note?.images ?: emptyList()) }
    var showSaveError by remember { mutableStateOf(false) }
    var isGeneratingSubject by remember { mutableStateOf(false) }
    var showManualInput by remember { mutableStateOf(false) }
    var manualSubjectInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        
        val destFile = File(
            context.getExternalFilesDir("images"),
            "note_image_${System.currentTimeMillis()}.jpg"
        )
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    copyStream(inputStream, outputStream)
                }
            }
            imagePaths = imagePaths + destFile.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("NoteEditScreen", "保存图片失败", e)
        }
    }
    
    // 插入文本的函数
    fun insertText(prefix: String, suffix: String = "", placeholder: String = "") {
        val text = contentFieldValue.text
        val selection = contentFieldValue.selection
        
        val selectedText = if (selection.collapsed) {
            placeholder
        } else {
            text.substring(selection.min, selection.max)
        }
        
        val newText = text.substring(0, selection.min) + 
                      prefix + selectedText + suffix + 
                      text.substring(selection.max)
        
        val newCursorPos = selection.min + prefix.length + selectedText.length + suffix.length
        
        contentFieldValue = TextFieldValue(
            text = newText,
            selection = TextRange(newCursorPos)
        )
    }
    
    // 在行首插入文本的函数
    fun insertAtLineStart(prefix: String) {
        val text = contentFieldValue.text
        val cursorPos = contentFieldValue.selection.start
        
        // 找到当前行的开始位置
        val lineStart = text.lastIndexOf('\n', cursorPos - 1) + 1
        
        val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
        val newCursorPos = cursorPos + prefix.length
        
        contentFieldValue = TextFieldValue(
            text = newText,
            selection = TextRange(newCursorPos)
        )
    }
    
    // 处理工具栏点击
    fun handleToolClick(tool: EditToolType) {
        when (tool) {
            EditToolType.BOLD -> insertText("**", "**", "粗体文字")
            EditToolType.ITALIC -> insertText("*", "*", "斜体文字")
            EditToolType.UNDERLINE -> insertText("<u>", "</u>", "下划线文字")
            EditToolType.STRIKETHROUGH -> insertText("~~", "~~", "删除线文字")
            EditToolType.HEADING -> insertAtLineStart("## ")
            EditToolType.BULLET_LIST -> insertAtLineStart("• ")
            EditToolType.NUMBERED_LIST -> insertAtLineStart("1. ")
            EditToolType.CHECKBOX -> insertAtLineStart("☐ ")
            EditToolType.QUOTE -> insertAtLineStart("> ")
            EditToolType.CODE -> insertText("`", "`", "代码")
            EditToolType.DIVIDER -> {
                val text = contentFieldValue.text
                val cursorPos = contentFieldValue.selection.start
                val divider = "\n─────────────────\n"
                val newText = text.substring(0, cursorPos) + divider + text.substring(cursorPos)
                contentFieldValue = TextFieldValue(
                    text = newText,
                    selection = TextRange(cursorPos + divider.length)
                )
            }
            EditToolType.TIMESTAMP -> {
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .format(Date())
                val text = contentFieldValue.text
                val cursorPos = contentFieldValue.selection.start
                val timeStr = "[$timestamp] "
                val newText = text.substring(0, cursorPos) + timeStr + text.substring(cursorPos)
                contentFieldValue = TextFieldValue(
                    text = newText,
                    selection = TextRange(cursorPos + timeStr.length)
                )
            }
            EditToolType.HIGHLIGHT -> insertText("【", "】", "高亮内容")
        }
    }
    
    // 当note或subjects更新时，同步更新状态变量
    LaunchedEffect(note?.id, note?.content, note?.title, note?.subject, note?.images, subjects) {
        android.util.Log.d("NoteEditScreen", "========== LaunchedEffect触发 ==========")
        note?.let {
            if (title != it.title) title = it.title
            if (subject != it.subject) subject = it.subject
            if (contentFieldValue.text != it.content) {
                contentFieldValue = TextFieldValue(it.content)
            }
            if (imagePaths != it.images) {
                imagePaths = it.images ?: emptyList()
            }
        } ?: run {
            title = ""
            // 确保科目有默认值
            subject = subjects.firstOrNull() ?: if (subjects.isEmpty()) "其他" else ""
            contentFieldValue = TextFieldValue("")
            imagePaths = emptyList()
        }
        
        // 如果当前科目不在列表中，且列表不为空，则使用第一个科目
        if (subject.isEmpty() && subjects.isNotEmpty()) {
            subject = subjects.first()
        } else if (subjects.isEmpty() && subject.isEmpty()) {
            subject = "其他"
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (note == null) "新建笔记" else "编辑笔记", color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                ),
                actions = {
                    TextButton(
                        onClick = {
                            // 验证标题
                            if (title.isBlank()) {
                                showSaveError = true
                                android.widget.Toast.makeText(
                                    context,
                                    "请输入标题",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                return@TextButton
                            }
                            
                            // 验证科目
                            val finalSubject = if (subject.isBlank()) {
                                // 如果科目为空，使用默认值
                                subjects.firstOrNull() ?: "其他"
                            } else {
                                subject
                            }
                            
                            // 确保科目在列表中，如果不在则添加到列表（仅用于显示）
                            onSave(title, finalSubject, contentFieldValue.text, imagePaths)
                            showSaveError = false
                        }
                    ) {
                        Text(
                            "保存", 
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
        ) {
            // 标题输入
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("输入标题...", color = Color.Black.copy(alpha = 0.4f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = PrimaryBlue
                )
            )
            
            // 科目和标签行
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 科目选择
                    var expanded by remember { mutableStateOf(false) }
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                            color = CardBackground
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Book,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = subject.ifEmpty { 
                                        if (subjects.isEmpty()) "其他" else "选择科目" 
                                    },
                                    color = if (subject.isEmpty()) Color.Black.copy(alpha = 0.5f) else Color.Black,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color.Black.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            // 如果科目列表为空，显示默认选项
                            val displaySubjects = if (subjects.isEmpty()) {
                                listOf("其他")
                            } else {
                                subjects
                            }
                            
                            displaySubjects.forEach { sub ->
                                DropdownMenuItem(
                                    text = { Text(sub, color = Color.Black) },
                                    onClick = {
                                        subject = sub
                                        expanded = false
                                        showManualInput = false
                                    }
                                )
                            }
                            // 添加手动输入选项
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = PrimaryBlue
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("手动输入", color = PrimaryBlue)
                                    }
                                },
                                onClick = {
                                    expanded = false
                                    showManualInput = true
                                    manualSubjectInput = subject
                                }
                            )
                        }
                    }
                    
                    // AI生成科目按钮
                    IconButton(
                        onClick = {
                            scope.launch {
                                isGeneratingSubject = true
                                try {
                                    val generatedSubject = onGenerateSubject(
                                        contentFieldValue.text,
                                        title.takeIf { it.isNotBlank() }
                                    )
                                    subject = generatedSubject
                                    android.widget.Toast.makeText(
                                        context,
                                        "已生成科目: $generatedSubject",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "生成科目失败: ${e.message}",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                } finally {
                                    isGeneratingSubject = false
                                }
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        if (isGeneratingSubject) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryBlue
                            )
                        } else {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "AI生成科目",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                // 手动输入科目
                if (showManualInput) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualSubjectInput,
                            onValueChange = { manualSubjectInput = it },
                            placeholder = { Text("输入科目名称", fontSize = 14.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = Color.Black.copy(alpha = 0.2f),
                                cursorColor = PrimaryBlue
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                        )
                        IconButton(
                            onClick = {
                                if (manualSubjectInput.isNotBlank()) {
                                    subject = manualSubjectInput.trim()
                                    showManualInput = false
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "确认",
                                tint = PrimaryBlue
                            )
                        }
                        IconButton(
                            onClick = {
                                showManualInput = false
                                manualSubjectInput = ""
                            }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "取消",
                                tint = Color.Black.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 编辑工具栏
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardBackground,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // 格式化按钮组
                    EditToolButton(Icons.Default.FormatBold, "粗体") { handleToolClick(EditToolType.BOLD) }
                    EditToolButton(Icons.Default.FormatItalic, "斜体") { handleToolClick(EditToolType.ITALIC) }
                    EditToolButton(Icons.Default.FormatUnderlined, "下划线") { handleToolClick(EditToolType.UNDERLINE) }
                    EditToolButton(Icons.Default.FormatStrikethrough, "删除线") { handleToolClick(EditToolType.STRIKETHROUGH) }
                    
                    Divider(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .padding(vertical = 4.dp),
                        color = Color.Black.copy(alpha = 0.1f)
                    )
                    
                    // 标题和列表
                    EditToolButton(Icons.Default.Title, "标题") { handleToolClick(EditToolType.HEADING) }
                    EditToolButton(Icons.Default.FormatListBulleted, "无序列表") { handleToolClick(EditToolType.BULLET_LIST) }
                    EditToolButton(Icons.Default.FormatListNumbered, "有序列表") { handleToolClick(EditToolType.NUMBERED_LIST) }
                    EditToolButton(Icons.Default.CheckBox, "复选框") { handleToolClick(EditToolType.CHECKBOX) }
                    
                    Divider(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .padding(vertical = 4.dp),
                        color = Color.Black.copy(alpha = 0.1f)
                    )
                    
                    // 其他功能
                    EditToolButton(Icons.Default.FormatQuote, "引用") { handleToolClick(EditToolType.QUOTE) }
                    EditToolButton(Icons.Default.Code, "代码") { handleToolClick(EditToolType.CODE) }
                    EditToolButton(Icons.Default.Highlight, "高亮") { handleToolClick(EditToolType.HIGHLIGHT) }
                    EditToolButton(Icons.Default.HorizontalRule, "分割线") { handleToolClick(EditToolType.DIVIDER) }
                    EditToolButton(Icons.Default.Schedule, "时间戳") { handleToolClick(EditToolType.TIMESTAMP) }
                    
                    Divider(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .padding(vertical = 4.dp),
                        color = Color.Black.copy(alpha = 0.1f)
                    )
                    
                    // 插入图片
                    EditToolButton(Icons.Default.Image, "图片") { imagePickerLauncher.launch("image/*") }
                }
            }
            
            // 内容编辑区域
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 显示已选择的图片
                if (imagePaths.isNotEmpty()) {
                    Text(
                        text = "图片 (${imagePaths.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        imagePaths.forEachIndexed { index, imagePath ->
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                AsyncImage(
                                    model = File(imagePath),
                                    contentDescription = "图片 $index",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // 删除按钮
                                Surface(
                                    onClick = {
                                        imagePaths = imagePaths.filterIndexed { i, _ -> i != index }
                                    },
                                    shape = RoundedCornerShape(50),
                                    color = Color.Black.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(24.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "删除图片",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // 内容输入框
                OutlinedTextField(
                    value = contentFieldValue,
                    onValueChange = { contentFieldValue = it },
                    placeholder = { 
                        Text(
                            "开始记录你的笔记...\n\n提示：使用工具栏快速插入格式",
                            color = Color.Black.copy(alpha = 0.4f)
                        ) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 400.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = PrimaryBlue,
                        selectionColors = TextSelectionColors(
                            handleColor = PrimaryBlue,
                            backgroundColor = PrimaryBlue.copy(alpha = 0.3f)
                        )
                    )
                )
                
                // 知识点显示（只读）
                note?.knowledgePoints?.let { points ->
                    if (points.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "知识点",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Black.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            points.forEach { point ->
                                val displayText = if (point.length > 12) point.take(12) + "..." else point
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CardBackground
                                ) {
                                    Text(
                                        text = displayText,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        fontSize = 12.sp,
                                        color = PrimaryBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // 底部信息栏
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = PageBackground,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${contentFieldValue.text.length} 字",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black.copy(alpha = 0.5f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (imagePaths.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.Black.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "${imagePaths.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Black.copy(alpha = 0.5f)
                                )
                            }
                        }
                        note?.let {
                            Text(
                                text = "更新于 ${SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(it.updatedAt))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Black.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditToolButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.Black.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}
