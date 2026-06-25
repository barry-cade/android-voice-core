package com.example.android_voice_core

object WhisperBridge {
    init {
        System.loadLibrary("whisper_bridge")
    }

    external fun init(modelPath: String): Long
    external fun transcribe(handle: Long, pcm: ShortArray): String
}
