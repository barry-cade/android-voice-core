package dev.barrycade.voicecore.stt

data class SttConfig(
    val sampleRate: Int = 16000,
    val bufferSize: Int = 32000,
    val modelPath: String? = null,
    val debugInstrumentation: Boolean = false,
    val chunkSeconds: Int? = 3,
    val overlapSeconds: Int? = 1
)
