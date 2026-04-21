package com.iflytek.cloud

import android.content.Context
import android.os.Bundle

object SpeechConstant {
    const val ENGINE_MODE = "engine_mode"
    const val MODE_MSC = "msc"
    const val LANGUAGE = "language"
    const val ISE_CATEGORY = "ise_category"
    const val TEXT_ENCODING = "text_encoding"
    const val RESULT_LEVEL = "result_level"
    const val VAD_BOS = "vad_bos"
    const val VAD_EOS = "vad_eos"
    const val AUDIO_FORMAT = "audio_format"
    const val SAMPLE_RATE = "sample_rate"
}

class SpeechUtility {
    companion object {
        fun createUtility(context: Context, params: String): SpeechUtility = SpeechUtility()
    }
}

data class EvaluatorResult(
    val resultString: String = "<xml><total_score>0</total_score></xml>"
)

data class SpeechError(
    val errorCode: Int = 0,
    val errorDescription: String = "debug stub"
)

interface EvaluatorListener {
    fun onResult(result: EvaluatorResult?, isLast: Boolean) = Unit
    fun onError(error: SpeechError?) = Unit
    fun onBeginOfSpeech() = Unit
    fun onEndOfSpeech() = Unit
    fun onVolumeChanged(volume: Int, data: ByteArray?) = Unit
    fun onEvent(eventType: Int, arg1: Int, arg2: Int, obj: Bundle?) = Unit
}

class SpeechEvaluator {
    private var listener: EvaluatorListener? = null

    fun setParameter(key: String, value: String) = Unit

    fun startEvaluating(referenceText: String, grammarText: String?, listener: EvaluatorListener) {
        this.listener = listener
        listener.onBeginOfSpeech()
    }

    fun writeAudio(data: ByteArray, offset: Int, length: Int) = Unit

    fun stopEvaluating() {
        listener?.onResult(EvaluatorResult(), true)
        listener?.onEndOfSpeech()
    }

    fun destroy() = Unit

    companion object {
        fun createEvaluator(context: Context, initListener: Any?): SpeechEvaluator = SpeechEvaluator()
    }
}
