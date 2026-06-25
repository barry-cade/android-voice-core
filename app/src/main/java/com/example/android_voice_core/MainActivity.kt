package com.example.android_voice_core

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.android_voice_core.audio.AudioTestService
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
            startAudioService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.startAudioTestButton).setOnClickListener {
            val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                permissions.add(Manifest.permission.FOREGROUND_SERVICE_MICROPHONE)
            }

            val needsRequest = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (needsRequest.isEmpty()) {
                startAudioService()
            } else {
                requestPermissionLauncher.launch(needsRequest.toTypedArray())
            }
        }
    }

    private fun startAudioService() {
        val modelPath = copyModelAssetToCache()
        val handle = WhisperBridge.init(modelPath)
        val pcm = ShortArray(1600) { 0 }
        val text = WhisperBridge.transcribe(handle, pcm)
        Log.d("WhisperBridge", "text=$text")

        val intent = Intent(this, AudioTestService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun copyModelAssetToCache(): String {
        val targetFile = File(cacheDir, "models/ggml-tiny.en.bin")
        if (!targetFile.exists()) {
            targetFile.parentFile?.mkdirs()
            assets.open("models/ggml-tiny.en.bin").use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return targetFile.absolutePath
    }
}