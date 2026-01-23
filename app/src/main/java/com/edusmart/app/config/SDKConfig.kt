package com.edusmart.app.config

/**
 * SDK配置类
 * 集中管理所有第三方SDK的配置信息
 * 
 * 使用前请：
 * 1. 注册对应平台账号
 * 2. 获取API密钥
 * 3. 替换下面的占位符
 */
object SDKConfig {
    
    // ========== 讯飞SparkChain SDK配置 ==========
    // 注册地址: https://www.xfyun.cn/
    // 文档: https://www.xfyun.cn/doc/asr/android-sdk.html
    // 获取方式: 登录控制台 https://console.xfyun.cn/services/iat 查看三元组
    const val XUNFEI_APP_ID = "d84681e5"
    const val XUNFEI_API_KEY = "a46af60f99fb72f8ad2880ccfb59c0f5"
    const val XUNFEI_API_SECRET = "ZmI1Y2MxNTFiOTUzYmY2OWQ2NmQ1OGU5"
    
    // ========== 百度SDK配置 ==========
    // 注册地址: https://ai.baidu.com/
    // 文档: https://ai.baidu.com/ai-doc/SPEECH/Vk38lxily
    const val BAIDU_API_KEY = "your-baidu-api-key"
    const val BAIDU_SECRET_KEY = "your-baidu-secret-key"
    
    // ========== AI服务配置 ==========
    
    // ========== 豆包API配置（字节跳动）==========
    // 
    // 📝 配置步骤：
    // 1. 访问 https://www.volcengine.com/product/doubao 注册/登录账号
    // 2. 进入控制台 https://console.volcengine.com/
    // 3. 创建智能体（Bot），获取 API Key（格式：sk-xxxxxxxx 或 xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx）
    // 4. 获取智能体ID（格式：bot-xxxxxxxx-xxxxx）
    // 5. 将下面的 "your-doubao-api-key" 替换为你的 API Key
    // 6. 将下面的 "your-bot-id" 替换为你的智能体ID
    //
    // 📚 详细配置指南：请查看项目根目录下的 "豆包API详细配置指南.md"
    // 🔗 官方文档: https://www.volcengine.com/docs/82379
    //
    // ⚠️ 注意：API Key 是敏感信息，不要提交到公开的代码仓库！
    //
    // ⚠️ 重要：如果控制台显示的是 UUID 格式的 API Key，请使用该 UUID
    // 如果控制台显示的是 "sk-" 开头的 API Key，请使用 "sk-" 开头的格式
    const val DOUBAO_API_KEY = "d8ded741-2441-4ace-8e2f-b1eabc0fcb6f"  // API Key (UUID格式)
    // Bot ID 是智能体的ID，格式通常是 "bot-xxxxxxxx-xxxxx"
    // 在智能体详情页可以找到
    const val DOUBAO_BOT_ID = "bot-20260115210659-jvj9l" // 智能体ID，请替换为你的智能体ID
    // 保留 MODEL_ID 用于兼容，但实际使用 BOT_ID
    const val DOUBAO_MODEL_ID = "doubao-pro-32k" // 已废弃，保留用于兼容
    
    // Claude API (Anthropic)
    // 注册地址: https://console.anthropic.com/
    const val CLAUDE_API_KEY = "your-claude-api-key"
    
    // 文心一言API (百度)
    // 注册地址: https://cloud.baidu.com/product/wenxinworkshop
    const val WENXIN_API_KEY = "your-wenxin-api-key"
    const val WENXIN_SECRET_KEY = "your-wenxin-secret-key"
    
    // OpenAI API (如果使用)
    // 注册地址: https://platform.openai.com/
    const val OPENAI_API_KEY = "your-openai-api-key"
    
    // 通义千问API (阿里云)
    // 注册地址: https://dashscope.aliyun.com/
    const val TONGYI_API_KEY = "sk-2c11207fb7a84dbb985f93cf3edf648a"
    
    /**
     * 检查配置是否完整
     */
    fun isConfigured(): Boolean {
        return XUNFEI_APP_ID != "your-xunfei-app-id" ||
               (BAIDU_API_KEY != "your-baidu-api-key" && BAIDU_SECRET_KEY != "your-baidu-secret-key")
    }
}

