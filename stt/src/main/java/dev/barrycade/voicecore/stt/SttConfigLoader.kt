package dev.barrycade.voicecore.stt

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.InputStream

internal object SttConfigLoader {
    private const val TAG = "STT_CONFIG"

    private val fallbackConfig = RuntimeSttConfig(
        energyThreshold = 0.03f,
        silencePaddingMs = 600,
        preRollMs = 200,
        maxUtteranceLengthMs = 7000,
        stableChunkSizeMs = 500,
        highPassCutoffHz = 0,
        motionMode = MotionModeConfig(
            energyThreshold = 0.05f,
            silencePaddingMs = 300
        )
    )

    fun load(context: Context): RuntimeSttConfig {
        return try {
            val assetStream = context.assets.open("stt_config.json")
            val config = assetStream.use(InputStream::readBytes).toString(Charsets.UTF_8)
            val parsed = parseConfig(config)
            Log.i(TAG, "Loaded STT config: $parsed")
            parsed
        } catch (t: Throwable) {
            Log.w(TAG, "Falling back to default STT config", t)
            fallbackConfig
        }
    }

    private fun parseConfig(json: String): RuntimeSttConfig {
        val root = JSONObject(json)
        val motionMode = root.optJSONObject("motionMode") ?: JSONObject()
        return RuntimeSttConfig(
            energyThreshold = root.optDouble("energyThreshold", fallbackConfig.energyThreshold.toDouble()).toFloat(),
            silencePaddingMs = root.optInt("silencePaddingMs", fallbackConfig.silencePaddingMs),
            preRollMs = root.optInt("preRollMs", fallbackConfig.preRollMs),
            maxUtteranceLengthMs = root.optInt("maxUtteranceLengthMs", fallbackConfig.maxUtteranceLengthMs),
            stableChunkSizeMs = root.optInt("stableChunkSizeMs", fallbackConfig.stableChunkSizeMs),
            highPassCutoffHz = root.optInt("highPassCutoffHz", fallbackConfig.highPassCutoffHz),
            motionMode = MotionModeConfig(
                energyThreshold = motionMode.optDouble("energyThreshold", fallbackConfig.motionMode.energyThreshold.toDouble()).toFloat(),
                silencePaddingMs = motionMode.optInt("silencePaddingMs", fallbackConfig.motionMode.silencePaddingMs)
            )
        )
    }
}
