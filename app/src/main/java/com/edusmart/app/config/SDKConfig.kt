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
    
    // 豆包API（字节跳动）
    // 注册地址: https://www.volcengine.com/product/doubao
    // 文档: https://www.volcengine.com/docs/82379
    // 获取方式: 登录控制台，创建应用，获取API Key
    const val DOUBAO_API_KEY = "your-doubao-api-key"
    const val DOUBAO_MODEL_ID = "your-doubao-model-id" // 模型端点ID
    
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
    const val TONGYI_API_KEY = "your-tongyi-api-key"
    
    /**
     * 检查配置是否完整
     */
    fun isConfigured(): Boolean {
        return XUNFEI_APP_ID != "your-xunfei-app-id" ||
               (BAIDU_API_KEY != "your-baidu-api-key" && BAIDU_SECRET_KEY != "your-baidu-secret-key")
    }
}

