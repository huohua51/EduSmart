package com.iflytek.sparkchain.core.asr

class ASR {
    private var callbacks: AsrCallbacks? = null

    fun registerCallbacks(callbacks: AsrCallbacks) {
        this.callbacks = callbacks
    }

    fun language(value: String) = Unit

    fun setParams(key: String, value: String) = Unit

    fun start(mode: String): Int {
        callbacks?.onBeginOfSpeech()
        return 0
    }

    fun write(data: ByteArray): Int = 0

    fun stop(force: Boolean): Int {
        callbacks?.onEndOfSpeech()
        callbacks?.onResult(ASRResult(bestMatchText = "", status = 2), null)
        return 0
    }

    data class ASRResult(
        val bestMatchText: String? = null,
        val status: Int = 2
    )

    data class ASRError(
        val errMsg: String? = null
    )
}

typealias ASRResult = ASR.ASRResult
typealias ASRError = ASR.ASRError

interface AsrCallbacks {
    fun onResult(asrResult: ASRResult?, o: Any?) = Unit
    fun onError(asrError: ASRError?, o: Any?) = Unit
    fun onBeginOfSpeech() = Unit
    fun onEndOfSpeech() = Unit
}
