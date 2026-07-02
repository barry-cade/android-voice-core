package dev.barrycade.voicecore.stt

internal object SpeechToTextFactory {
    fun create(
        energyThreshold: Float,
        silencePaddingMs: Int,
        preRollMs: Int,
        maxUtteranceLengthMs: Int,
        stableChunkSizeMs: Int,
        highPassCutoffHz: Int,
        motionModeEnergyThreshold: Float,
        motionModeSilencePaddingMs: Int,
        modelPath: String
    ): SpeechToText {
        return SpeechToText(
            energyThreshold = energyThreshold,
            silencePaddingMs = silencePaddingMs,
            preRollMs = preRollMs,
            maxUtteranceLengthMs = maxUtteranceLengthMs,
            stableChunkSizeMs = stableChunkSizeMs,
            highPassCutoffHz = highPassCutoffHz,
            motionModeEnergyThreshold = motionModeEnergyThreshold,
            motionModeSilencePaddingMs = motionModeSilencePaddingMs,
            modelPath = modelPath
        )
    }
}
