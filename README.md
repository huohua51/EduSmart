# EduSmart 智能学习助手

一个功能丰富的Android教育应用，集成AR、OCR、语音识别、AI问答等多项技术，为学生提供全方位的学习辅助。

## 📱 核心功能

### 1. 拍照识题
- 拍摄题目自动OCR识别
- AI匹配题库答案
- 自动生成解题步骤
- 错题本管理
- 生成变式题练习

### 2. AR知识空间
- 拍摄课本插图显示3D动态模型
- 几何图形可手势旋转、拆解
- 化学分子结构动态组装
- 物理实验过程AR模拟
- 历史地理场景重现

### 3. 智能笔记精灵
- 拍摄黑板自动OCR识别
- 语音转写老师讲解（支持方言）
- 自动合并图文生成结构化笔记
- AI提取知识点并高亮重点
- 笔记问答功能

### 4. AI口语私教
- 情景对话模拟（机场、面试、餐厅等）
- 实时语音识别+发音纠错
- 发音评分（0-100分）
- AI提供3种地道替换说法
- 每日新闻跟读练习

### 5. 学科知识雷达
- 10分钟快速测评
- 拍摄成绩单分析失分率
- 自适应出题精准定位薄弱点
- 生成个性化7天突破计划
- 雷达图可视化掌握度
- 匹配同校同弱项学习伙伴

## 🛠 技术架构

### 核心技术栈
- **UI框架**: Jetpack Compose + Material 3
- **架构模式**: MVVM + Repository
- **数据库**: Room
- **相机**: CameraX
- **AR**: ARCore
- **OCR**: PaddleOCR / Google ML Kit
- **语音**: 讯飞/百度语音SDK
- **网络**: Retrofit + OkHttp
- **协程**: Kotlin Coroutines
- **依赖注入**: 手动依赖注入（可升级为Hilt）

### 项目结构
```
app/
├── data/
│   ├── database/          # Room数据库
│   ├── dao/               # 数据访问对象
│   └── entity/            # 数据实体
├── feature/
│   ├── home/              # 首页
│   ├── scan/              # 拍照识题
│   ├── ar/                # AR知识空间
│   ├── note/              # 智能笔记
│   ├── speaking/          # 口语私教
│   └── radar/             # 知识雷达
├── repository/            # 数据仓库层
├── service/               # 业务服务层
│   ├── OCRService         # OCR识别
│   ├── SpeechService      # 语音识别
│   └── AIService          # AI服务
└── ui/
    ├── components/        # 通用组件
    ├── navigation/        # 导航
    └── theme/             # 主题样式
```

## 🚀 快速开始

### 环境要求
- Android Studio Hedgehog | 2023.1.1 或更高版本
- JDK 17
- Android SDK 26+ (Android 8.0+)
- Gradle 8.0+

### 安装步骤

1. **克隆项目**
```bash
git clone <repository-url>
cd 软件创新大赛
```

2. **配置API密钥**
在 `app/src/main/java/com/edusmart/app/service/AIService.kt` 中配置：
- Claude API Key
- 文心一言 API Key
- 讯飞/百度语音 SDK Key

3. **同步依赖**
```bash
./gradlew build
```

4. **运行应用**
- 连接Android设备或启动模拟器
- 点击 Run 按钮

## 📦 依赖说明

### 主要依赖
- `androidx.compose.*` - Jetpack Compose UI框架
- `androidx.camera.*` - CameraX相机库
- `com.google.ar:core` - ARCore AR支持
- `androidx.room:*` - Room数据库
- `com.squareup.retrofit2:*` - 网络请求
- `org.jetbrains.kotlinx:kotlinx-coroutines-android` - 协程支持

### OCR集成
推荐使用以下方案之一：
- **PaddleOCR**: 轻量级，支持离线
- **Google ML Kit**: 官方支持，易集成
- **百度OCR API**: 云端识别，准确率高

### 语音识别
- **讯飞SDK**: 支持离线识别，方言识别
- **百度语音SDK**: 云端识别，准确率高

## 🎨 UI设计

应用采用**黑白极简主题**：
- 白色背景 + 黑色文字
- 黑色背景 + 白色文字
- 灰色悬停状态
- 无彩色和渐变
- 统一的卡片、按钮、标签样式

## 📝 开发计划

### MVP版本（4周）
- [x] 项目基础架构
- [x] 数据层设计
- [ ] 第1周: 拍照识题+OCR识别+基础题库匹配
- [ ] 第2周: 错题本管理+知识点关联图谱
- [ ] 第3周: 语音答疑+AI生成变式题
- [ ] 第4周: 数据可视化+复习提醒系统

### 后续优化
- [ ] AR功能完整实现
- [ ] 离线模式优化
- [ ] 性能优化（启动速度<3秒）
- [ ] 安装包大小优化（<150MB）
- [ ] 功耗优化（后台录音<5%/小时）

## 🔧 配置说明

### 权限配置
应用需要以下权限：
- `CAMERA` - 拍照和AR功能
- `RECORD_AUDIO` - 录音功能
- `READ_EXTERNAL_STORAGE` - 读取图片
- `INTERNET` - 网络请求
- `FOREGROUND_SERVICE` - 后台录音服务

### 文件存储
- 图片: `getExternalFilesDir("images")`
- 音频: `getExternalFilesDir("audio")`
- 笔记: `getExternalFilesDir("notes")`

## 🐛 已知问题

- AR功能需要真机测试（模拟器不支持ARCore）
- OCR识别准确率依赖图片质量
- 语音识别需要网络连接（离线模式待优化）

## 📄 许可证

本项目为软件创新大赛参赛作品。

## 👥 贡献

欢迎提交Issue和Pull Request！

---

**注意**: 本项目为演示版本，部分功能需要配置相应的API密钥才能正常使用。

