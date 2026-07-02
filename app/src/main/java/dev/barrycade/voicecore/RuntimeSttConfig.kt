package dev.barrycade.voicecore

data class RuntimeSttConfig(
    val energyThreshold: Float,
    val silencePaddingMs: Int,
    val preRollMs: Int,
    val maxUtteranceLengthMs: Int,
    val stableChunkSizeMs: Int,
    val highPassCutoffHz: Int,
    val motionMode: MotionModeConfig
)

data class MotionModeConfig(
    val energyThreshold: Float,
    val silencePaddingMs: Int
)
