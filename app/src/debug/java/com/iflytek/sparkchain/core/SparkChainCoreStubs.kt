package com.iflytek.sparkchain.core

import android.content.Context

enum class LogLvl {
    VERBOSE;

    fun getValue(): Int = 0
}

class SparkChainConfig {
    fun appID(value: String): SparkChainConfig = this
    fun apiKey(value: String): SparkChainConfig = this
    fun apiSecret(value: String): SparkChainConfig = this
    fun logPath(value: String?): SparkChainConfig = this
    fun logLevel(value: Int): SparkChainConfig = this

    companion object {
        fun builder(): SparkChainConfig = SparkChainConfig()
    }
}

class SparkChain private constructor() {
    fun init(context: Context, config: SparkChainConfig): Int = 0

    fun unInit() = Unit

    companion object {
        private val INSTANCE = SparkChain()

        fun getInst(): SparkChain = INSTANCE
    }
}

data class LLMOutput(
    val content: String = "Debug 构建已屏蔽 SparkChain，当前仅保留登录、建笔记与 AI 润色验证。"
)

data class LLMResult(
    val content: String? = null,
    val status: Int = 2
)

data class LLMError(
    val errMsg: String? = null
)

data class LLMEvent(
    val eventID: Int = 0
)

interface LLMCallbacks {
    fun onLLMResult(result: LLMResult?, usrTag: Any?) = Unit
    fun onLLMError(error: LLMError?, usrTag: Any?) = Unit
    fun onLLMEvent(event: LLMEvent?, usrTag: Any?) = Unit
}

class LLM {
    private var callbacks: LLMCallbacks? = null

    fun registerLLMCallbacks(callbacks: LLMCallbacks) {
        this.callbacks = callbacks
    }

    fun run(prompt: String): LLMOutput {
        return LLMOutput()
    }

    fun arun(prompt: String): Int {
        callbacks?.onLLMResult(LLMResult(content = LLMOutput().content, status = 2), null)
        return 0
    }
}

object LLMFactory {
    fun textGeneration(): LLM = LLM()
}

class LLMConfig
