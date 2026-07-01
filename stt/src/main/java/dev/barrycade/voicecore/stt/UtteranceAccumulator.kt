package dev.barrycade.voicecore.stt

/**
 * UtteranceAccumulator transforms incoming FloatArray frames into complete utterance buffers.
 * It supports a short pre-roll, silence-based finalization, and a hard safety cap.
 */
internal class UtteranceAccumulator(
    private val sampleRate: Int = 16000,
    private val preRollMs: Int = 200,
    private val silenceDurationMs: Int = 500,
    private val maxUtteranceLengthMs: Int = 7000,
    private val stableBlockMs: Int = 500,
    private val vad: Vad = Vad()
) {
    private val preRollSamples = (sampleRate * preRollMs / 1000).coerceAtLeast(1)
    private val silenceFrameDurationMs = 20
    private val maxSilenceFrames = (silenceDurationMs / silenceFrameDurationMs).coerceAtLeast(1)
    private val stableBlockSamples = (sampleRate * stableBlockMs / 1000).coerceAtLeast(1)

    private val speechAccumulator = mutableListOf<Float>()
    private var speechActive = false
    private var silenceFrameCount = 0
    private var totalDurationMs = 0
    private val preRollBuffer = mutableListOf<Float>()

    fun processChunk(frame: FloatArray, isSpeechFrame: Boolean): FloatArray? {
        if (frame.isEmpty()) return null

        val frameDurationMs = frame.size * 1000 / sampleRate
        totalDurationMs += frameDurationMs

        return if (speechActive) {
            if (isSpeechFrame) {
                speechAccumulator.addAll(frame.toList())
                silenceFrameCount = 0
                if (totalDurationMs > maxUtteranceLengthMs) {
                    finalizeUtterance()
                } else {
                    null
                }
            } else {
                speechAccumulator.addAll(frame.toList())
                silenceFrameCount += 1
                if (silenceFrameCount >= maxSilenceFrames) {
                    finalizeUtterance()
                } else {
                    null
                }
            }
        } else {
            if (isSpeechFrame) {
                speechActive = true
                silenceFrameCount = 0
                speechAccumulator.clear()
                speechAccumulator.addAll(preRollBuffer)
                speechAccumulator.addAll(frame.toList())
                preRollBuffer.clear()
                totalDurationMs = 0
                null
            } else {
                preRollBuffer.addAll(frame.toList())
                if (preRollBuffer.size > preRollSamples) {
                    val excess = preRollBuffer.size - preRollSamples
                    repeat(excess) {
                        preRollBuffer.removeAt(0)
                    }
                }
                null
            }
        }
    }

    fun processFrame(frame: FloatArray): FloatArray? = processChunk(frame, vad.isSpeech(frame))

    fun reset() {
        speechAccumulator.clear()
        preRollBuffer.clear()
        speechActive = false
        silenceFrameCount = 0
        totalDurationMs = 0
    }

    private fun finalizeUtterance(): FloatArray {
        val utterance = speechAccumulator.toFloatArray()
        val paddedLength = if (utterance.size % stableBlockSamples == 0) {
            utterance.size
        } else {
            ((utterance.size / stableBlockSamples) + 1) * stableBlockSamples
        }

        val padded = FloatArray(paddedLength)
        utterance.copyInto(padded, 0)
        speechAccumulator.clear()
        preRollBuffer.clear()
        speechActive = false
        silenceFrameCount = 0
        totalDurationMs = 0
        return padded
    }
}
