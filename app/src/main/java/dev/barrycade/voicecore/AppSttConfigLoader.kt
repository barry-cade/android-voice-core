package dev.barrycade.voicecore

import android.content.Context
import android.util.Log
import dev.barrycade.voicecore.MotionModeConfig
import dev.barrycade.voicecore.RuntimeSttConfig
import org.json.JSONObject

object AppSttConfigLoader {
    private const val TAG = "STT_CONFIG"

    fun loadFromAssets(context: Context): RuntimeSttConfig {
        val json = context.assets.open("stt_config.json")
            .use { it.readBytes().toString(Charsets.UTF_8) }

        return try {
            parse(json)
        } catch (t: Throwable) {
            Log.e(TAG, "Invalid STT configuration", t)
            throw IllegalStateException("Invalid STT configuration: ${t.message}", t)
        }
    }

    private fun parse(json: String): RuntimeSttConfig {
        val root = JSONObject(json)
        val motionMode = root.getJSONObject("motionMode")
        return RuntimeSttConfig(
            energyThreshold = root.getDouble("energyThreshold").toFloat(),
            silencePaddingMs = root.getInt("silencePaddingMs"),
            preRollMs = root.getInt("preRollMs"),
            maxUtteranceLengthMs = root.getInt("maxUtteranceLengthMs"),
            stableChunkSizeMs = root.getInt("stableChunkSizeMs"),
            highPassCutoffHz = root.getInt("highPassCutoffHz"),
            motionMode = MotionModeConfig(
                energyThreshold = motionMode.getDouble("energyThreshold").toFloat(),
                silencePaddingMs = motionMode.getInt("silencePaddingMs")
            )
        )
    }
}
