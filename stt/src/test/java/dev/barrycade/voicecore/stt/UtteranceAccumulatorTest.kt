package dev.barrycade.voicecore.stt

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UtteranceAccumulatorTest {
    @Test
    fun emitsUtteranceAfterSilence() {
        val accumulator = UtteranceAccumulator(sampleRate = 16000, silenceDurationMs = 20)
        val speechFrame = FloatArray(160) { 0.2f }
        val silenceFrame = FloatArray(160) { 0.0f }

        assertTrue(accumulator.processFrame(speechFrame) == null)
        assertTrue(accumulator.processFrame(speechFrame) == null)
        val finalized = accumulator.processFrame(silenceFrame)
        assertNotNull(finalized)
        assertTrue(finalized!!.isNotEmpty())
    }
}
