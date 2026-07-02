package dev.barrycade.voicecore.stt

internal interface SttRuntimeConfig {
    val energyThreshold: Float
    val silencePaddingMs: Int
    val preRollMs: Int
    val maxUtteranceLengthMs: Int
    val stableChunkSizeMs: Int
    val highPassCutoffHz: Int
    val motionMode: SttMotionModeConfig
}

internal interface SttMotionModeConfig {
    val energyThreshold: Float
    val silencePaddingMs: Int
}
