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
    const val XUNFEI_APP_ID = "bddbdddc"
    const val XUNFEI_API_KEY = "7e5660798385addf9d13b73cde5eb6d0"
    const val XUNFEI_API_SECRET = "ZjM4ZTE1OThmNGIyOTA5MTFmN2EwNmUz"
    
    // ========== 百度SDK配置 ==========
    // 注册地址: https://ai.baidu.com/
    // 文档: https://ai.baidu.com/ai-doc/SPEECH/Vk38lxily
    const val BAIDU_API_KEY = "your-baidu-api-key"
    const val BAIDU_SECRET_KEY = "your-baidu-secret-key"
    
    // ========== AI服务配置 ==========

    // ========== 豆包TTS语音合成配置 ==========
    // 官方文档: https://www.volcengine.com/docs/6561/79824
    const val DOUBAO_APP_ID = "7638408005"
    const val DOUBAO_ACCESS_TOKEN = "Jt1APcCxsGKfhdh6Cqv4kNbSTZWcHx9H"
    const val DOUBAO_SECRET_KEY = "gO62aF3BaiQYXhDevOiCQyrNQZ24w-g3"
    const val DOUBAO_MODEL_ID = "seed-tts-2.0"
    
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
    
    // ========== 腾讯云开发配置 ==========
    // 控制台: https://console.cloud.tencent.com/tcb
    // 文档: https://cloud.tencent.com/document/product/876
    const val TCB_ENV_ID = "edusmart-dev-3gqo04ike66344ea-1327750873"  // ✅ 您的完整环境ID
    // API 基础URL（根据您的区域选择）
    // 上海: ap-shanghai, 北京: ap-beijing, 广州: ap-guangzhou
    const val TCB_REGION = "ap-shanghai"  // 上海地域
    val TCB_API_BASE_URL: String
        get() = "https://$TCB_ENV_ID.$TCB_REGION.app.tcloudbase.com"
    
    // ========== 阿里云函数计算配置 ==========
    // 控制台: https://fcnext.console.aliyun.com/
    // 文档: https://help.aliyun.com/product/50980.html
    // ✅ 请替换为您的函数 HTTP 触发器地址（不需要加 /auth 后缀）
    const val ALIYUN_FC_BASE_URL = "https://auth-dskbajmzlo.cn-hangzhou.fcapp.run"
    
    // ========== LeanCloud 配置（推荐：最简单的后端方案）==========
    // 控制台: https://console.leancloud.cn/
    // 文档: https://leancloud.cn/docs/sdk_setup-android.html
    // 注册后获取 AppID 和 AppKey
    // ✅ 请替换为您的 LeanCloud AppID 和 AppKey
    const val LEANCLOUD_APP_ID = "your-leancloud-app-id"
    const val LEANCLOUD_APP_KEY = "your-leancloud-app-key"
    const val LEANCLOUD_SERVER_URL = "https://your-server-url.leancloud.cn" // 可选，国内节点会自动选择
    
    /**
     * 检查配置是否完整
     */
    fun isConfigured(): Boolean {
        return (XUNFEI_APP_ID != "your-xunfei-app-id" && XUNFEI_APP_ID.isNotEmpty()) ||
                (TONGYI_API_KEY != "your-tongyi-api-key" && TONGYI_API_KEY.isNotEmpty())

    }
}

