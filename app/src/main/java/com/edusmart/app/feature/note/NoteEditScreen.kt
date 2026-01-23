package com.edusmart.app.feature.note

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.edusmart.app.data.entity.NoteEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    note: NoteEntity?,
    subjects: List<String>,
    onSave: (String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    // 初始化状态变量
    var title by remember { mutableStateOf(note?.title ?: "") }
    var subject by remember { mutableStateOf(note?.subject ?: subjects.firstOrNull() ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    
    // 当note更新时，同步更新状态变量（用于AI润色、总结等功能应用后刷新UI）
    // 使用note的id和content作为依赖项，确保内容变化时能检测到
    LaunchedEffect(note?.id, note?.content, note?.title, note?.subject) {
        android.util.Log.d("NoteEditScreen", "========== LaunchedEffect触发 ==========")
        android.util.Log.d("NoteEditScreen", "note.id=${note?.id}")
        android.util.Log.d("NoteEditScreen", "note.content长度=${note?.content?.length}")
        android.util.Log.d("NoteEditScreen", "当前content状态长度=${content.length}")
        android.util.Log.d("NoteEditScreen", "content是否相等: ${content == note?.content}")
        
        note?.let {
            val titleChanged = title != it.title
            val subjectChanged = subject != it.subject
            val contentChanged = content != it.content
            
            android.util.Log.d("NoteEditScreen", "title需要更新: $titleChanged")
            android.util.Log.d("NoteEditScreen", "subject需要更新: $subjectChanged")
            android.util.Log.d("NoteEditScreen", "content需要更新: $contentChanged")
            
            if (titleChanged) {
                android.util.Log.d("NoteEditScreen", "更新title: '$title' -> '${it.title}'")
                title = it.title
            }
            if (subjectChanged) {
                android.util.Log.d("NoteEditScreen", "更新subject: '$subject' -> '${it.subject}'")
                subject = it.subject
            }
            if (contentChanged) {
                android.util.Log.d("NoteEditScreen", "更新content")
                android.util.Log.d("NoteEditScreen", "旧content前50字符: ${content.take(50)}")
                android.util.Log.d("NoteEditScreen", "新content前50字符: ${it.content.take(50)}")
                content = it.content
                android.util.Log.d("NoteEditScreen", "✅ content已更新为: ${content.take(50)}...")
            } else {
                android.util.Log.d("NoteEditScreen", "⚠️ content未变化，跳过更新")
            }
        } ?: run {
            // 如果note为null，清空所有字段
            android.util.Log.d("NoteEditScreen", "note为null，清空所有字段")
            title = ""
            subject = subjects.firstOrNull() ?: ""
            content = ""
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (note == null) "新建笔记" else "编辑笔记") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (title.isNotBlank() && subject.isNotBlank()) {
                                onSave(title, subject, content)
                            }
                        }
                    ) {
                        Text("保存")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题输入
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // 科目选择 - 支持输入新科目或从已有科目中选择
            var expanded by remember { mutableStateOf(false) }
            val filteredSubjects = remember(subject, subjects) {
                if (subject.isEmpty()) {
                    subjects
                } else {
                    subjects.filter { it.contains(subject, ignoreCase = true) }
                }
            }
            
            ExposedDropdownMenuBox(
                expanded = expanded && filteredSubjects.isNotEmpty(),
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { 
                        subject = it
                        expanded = true // 输入时展开下拉菜单
                    },
                    label = { Text("科目") },
                    placeholder = { Text("输入或选择科目") },
                    trailingIcon = { 
                        if (filteredSubjects.isNotEmpty()) {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = expanded && filteredSubjects.isNotEmpty(),
                    onDismissRequest = { expanded = false }
                ) {
                    filteredSubjects.forEach { sub ->
                        DropdownMenuItem(
                            text = { Text(sub) },
                            onClick = {
                                subject = sub
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            // 内容输入
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("内容") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 300.dp), // 最小高度，允许根据内容自动扩展
                minLines = 15, // 最小显示15行
                maxLines = 500 // 最多显示500行，超出部分可通过整个界面滚动查看
            )
            
            // 知识点显示（只读）
            note?.knowledgePoints?.let { points ->
                if (points.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "知识点",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            points.forEach { point ->
                                Text(
                                    text = "• $point",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

