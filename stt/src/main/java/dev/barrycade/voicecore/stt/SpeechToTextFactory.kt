package dev.barrycade.voicecore.stt

internal object SpeechToTextFactory {
    fun create(
        config: RuntimeSttConfig,
        modelPath: String
    ): SpeechToText {
        return SpeechToText(
            config = config,
            modelPath = modelPath
        )
    }
}
