package dev.barrycade.voicecore

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.content.ContextCompat
import dev.barrycade.voicecore.stt.SpeechToText
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var txtOutput: TextView

    private var stt: SpeechToText? = null
    private var isRecording = false

    private val requestPermissionLauncher = registerForActivityResult(
        RequestPermission()
    ) { granted ->
        if (granted) startRecording()
        else txtOutput.text = "Microphone permission is required"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        txtOutput = findViewById(R.id.txtOutput)

        btnStart.setOnClickListener {
            if (hasRecordAudioPermission()) startRecording()
            else requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        btnStop.setOnClickListener {
            if (isRecording) stopAndTranscribe()
        }

        updateUi()
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startRecording() {
        isRecording = true
        txtOutput.text = "Recording..."
        updateUi()

        val modelPath = getModelPath()
        val runtimeConfig = AppSttConfigLoader.loadFromAssets(this)
        stt = SpeechToText(
            energyThreshold = runtimeConfig.energyThreshold,
            silencePaddingMs = runtimeConfig.silencePaddingMs,
            preRollMs = runtimeConfig.preRollMs,
            maxUtteranceLengthMs = runtimeConfig.maxUtteranceLengthMs,
            stableChunkSizeMs = runtimeConfig.stableChunkSizeMs,
            highPassCutoffHz = runtimeConfig.highPassCutoffHz,
            motionModeEnergyThreshold = runtimeConfig.motionMode.energyThreshold,
            motionModeSilencePaddingMs = runtimeConfig.motionMode.silencePaddingMs,
            modelPath = modelPath
        ).also {
            it.setOnResultListener { result ->
                runOnUiThread { txtOutput.text = result }
            }
            it.setOnErrorListener { t ->
                runOnUiThread { txtOutput.text = "Error: ${t.message}" }
            }
            it.start()
        }
    }

    private fun stopAndTranscribe() {
        isRecording = false
        txtOutput.text = "Processing..."
        updateUi()

        Thread {
            try {
                Log.d("MainActivity", "STOP pressed → using deterministic stopAndTranscribe()")
                stt?.stopAndTranscribe()
            } catch (t: Throwable) {
                runOnUiThread { txtOutput.text = "Error: ${t.message}" }
            }
        }.start()
    }

    private fun updateUi() {
        btnStart.isEnabled = !isRecording
        btnStop.isEnabled = isRecording
    }

    private fun getModelPath(): String {
        val targetFile = File(filesDir, "model.bin")
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