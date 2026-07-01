package dev.barrycade.voicecore.stt

import android.util.Log

object WhisperBridge {
    init {
        Log.d("WhisperBridge", "Kotlin bridge init start t=${System.currentTimeMillis()}")
        System.loadLibrary("c++_shared")
        System.loadLibrary("omp")
        System.loadLibrary("ggml-base")
        System.loadLibrary("ggml-cpu")
        System.loadLibrary("ggml")
        System.loadLibrary("whisper")
        System.loadLibrary("whisper_bridge")
        Log.d("WhisperBridge", "Kotlin bridge init end t=${System.currentTimeMillis()}")
    }

    external fun loadModel(modelPath: String)
    external fun transcribe(samples: ShortArray): String
    external fun unloadModel()
}
