package com.iflytek.sparkchain.core.tts

open class TTS {
    data class TTSEvent(val eventId: Int = 0)

    data class TTSResult(
        private val data: ByteArray = ByteArray(0),
        private val status: Int = 2,
        private val len: Int = data.size
    ) {
        fun getData(): ByteArray = data
        fun getStatus(): Int = status
        fun getLen(): Int = len
    }

    data class TTSError(
        private val code: Int = 0,
        private val errMsg: String = "debug stub",
        private val sid: String = "debug"
    ) {
        fun getCode(): Int = code
        fun getErrMsg(): String = errMsg
        fun getSid(): String = sid
    }
}

interface TTSCallbacks {
    fun onResult(result: TTS.TTSResult?, usrTag: Any?) = Unit
    fun onError(error: TTS.TTSError?, usrTag: Any?) = Unit
}

class OnlineTTS(private val speaker: String) {
    private var callbacks: TTSCallbacks? = null

    fun registerCallbacks(callbacks: TTSCallbacks) {
        this.callbacks = callbacks
    }

    fun aue(value: String) = Unit
    fun auf(value: String) = Unit
    fun speed(value: Int) = Unit
    fun pitch(value: Int) = Unit
    fun volume(value: Int) = Unit
    fun bgs(value: Int) = Unit
    fun tte(value: String) = Unit

    fun aRun(text: String): Int {
        callbacks?.onResult(TTS.TTSResult(), null)
        return 0
    }

    fun stop(): Int = 0
}
