# 豆包API配置指南

## 🎯 功能说明

已集成AI答题功能：
- ✅ OCR识别题目文字
- ✅ AI自动生成答案和解析
- ✅ 支持数学公式（sin、cos等）
- ✅ 支持复杂题目
- ✅ 提供解题步骤和知识点

## 📝 配置步骤

### 步骤1: 注册豆包账号

1. 访问：https://www.volcengine.com/product/doubao
2. 注册/登录账号
3. 进入控制台

### 步骤2: 创建应用并获取API Key

1. 在控制台创建新应用
2. 获取 **API Key**
3. 获取 **模型端点ID**（Model ID）

### 步骤3: 配置API密钥

在 `app/src/main/java/com/edusmart/app/config/SDKConfig.kt` 中：

```kotlin
// 豆包API配置
const val DOUBAO_API_KEY = "your-doubao-api-key"  // 替换为您的API Key
const val DOUBAO_MODEL_ID = "your-doubao-model-id"  // 替换为您的模型ID
```

### 步骤4: 更新API端点（如果需要）

在 `AIAnswerService.kt` 中，根据豆包API文档调整：
- API端点URL
- 请求格式
- 响应解析

## 🔄 工作流程

1. **用户拍照** → 获取题目图片
2. **OCR识别** → 提取题目文字（即使有公式也能识别部分）
3. **AI分析** → 将图片和文字一起发送给AI
4. **生成答案** → AI返回：
   - 正确答案
   - 详细解题步骤
   - 涉及的知识点
   - 解题思路

## 🎨 界面展示

拍照后，界面会显示：
1. **识别到的题目** - OCR识别的文字
2. **答案** - AI生成的答案
3. **解题步骤** - 分步骤解析
4. **涉及知识点** - 相关知识点列表
5. **思路分析** - 解题思路说明

## 🔧 其他AI服务支持

如果不想使用豆包，可以配置其他AI服务：

### 选项1: 通义千问（阿里云）

1. 注册：https://dashscope.aliyun.com/
2. 获取API Key
3. 在 `SDKConfig.kt` 中配置：
   ```kotlin
   const val TONGYI_API_KEY = "your-tongyi-api-key"
   ```
4. 修改 `AIAnswerService.kt` 中的API调用

### 选项2: 文心一言（百度）

1. 注册：https://cloud.baidu.com/product/wenxinworkshop
2. 获取API Key和Secret Key
3. 配置并修改API调用

### 选项3: OpenAI/Claude

类似配置方式，修改API端点即可。

## 📊 API调用示例

当前实现支持：
- **文本输入**：OCR识别的文字
- **图片输入**：原始题目图片（Base64编码）
- **多模态**：同时发送文字和图片，AI可以识别公式

## ⚠️ 注意事项

1. **API费用**：使用AI API会产生费用，注意控制调用频率
2. **网络要求**：需要网络连接才能使用
3. **响应时间**：AI生成答案可能需要几秒钟
4. **API限制**：注意API的调用频率限制

## 🚀 快速开始

1. **获取豆包API Key**
2. **在 `SDKConfig.kt` 中配置**
3. **同步Gradle**
4. **运行应用**
5. **拍照测试**

---

**现在就可以配置API Key并开始使用AI答题功能了！**

