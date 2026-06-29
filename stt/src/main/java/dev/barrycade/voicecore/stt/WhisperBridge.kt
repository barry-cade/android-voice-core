package dev.barrycade.voicecore.stt

object WhisperBridge {
    init {
        System.loadLibrary("c++_shared")
        System.loadLibrary("omp")
        System.loadLibrary("ggml-base")
        System.loadLibrary("ggml-cpu")
        System.loadLibrary("ggml")
        System.loadLibrary("whisper")
        System.loadLibrary("whisper_bridge")
    }

    external fun loadModel(modelPath: String)
    external fun transcribe(samples: ShortArray): String

    internal external fun unloadModel()
}
