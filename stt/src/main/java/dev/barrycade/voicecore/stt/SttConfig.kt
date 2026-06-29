package dev.barrycade.voicecore.stt

data class SttConfig(
    val sampleRate: Int = 16000,
    val bufferSize: Int = 2048,
    val modelPath: String? = null
)
