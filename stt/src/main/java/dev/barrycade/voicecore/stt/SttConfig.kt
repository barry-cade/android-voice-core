package dev.barrycade.voicecore.stt

data class SttConfig(
    val sampleRate: Int = 16000,
    val bufferSize: Int = 32000,
    val modelPath: String? = null,
    val debugInstrumentation: Boolean = false,
    val chunkSeconds: Int? = 3,
    val overlapSeconds: Int? = 1
)

internal data class RuntimeSttConfig(
    val energyThreshold: Float,
    val silencePaddingMs: Int,
    val preRollMs: Int,
    val maxUtteranceLengthMs: Int,
    val stableChunkSizeMs: Int,
    val highPassCutoffHz: Int,
    val motionMode: MotionModeConfig
)

internal data class MotionModeConfig(
    val energyThreshold: Float,
    val silencePaddingMs: Int
)
