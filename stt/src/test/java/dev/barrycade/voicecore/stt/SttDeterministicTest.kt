package dev.barrycade.voicecore.stt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStream

class SttDeterministicTest {
    @Test
    fun deterministicPipelineProducesStableUtteranceAndTranscript() {
        val resource = javaClass.classLoader?.getResourceAsStream("audio/shop_milk_newspaper.wav")
        assertNotNull(resource)

        val pcm = resource!!.use { loadPcm16Mono(it) }
        val frameSize = 320
        val frames = mutableListOf<FloatArray>()
        var index = 0
        while (index < pcm.size) {
            val end = minOf(index + frameSize, pcm.size)
            val samples = pcm.copyOfRange(index, end)
            val frame = FloatArray(samples.size) { offset -> samples[offset].toFloat() / Short.MAX_VALUE }
            frames.add(frame)
            index = end
        }

        val utterances = mutableListOf<FloatArray>()

        repeat(10) {
            val vad = Vad(energyThreshold = 0.01)
            val accumulator = UtteranceAccumulator(sampleRate = 16000, silenceDurationMs = 500)
            var finalizedUtterance: FloatArray? = null

            frames.forEach { frame ->
                val isSpeech = vad.isSpeech(frame)
                finalizedUtterance = accumulator.processChunk(frame, isSpeech)
            }

            val paddingFrames = List(25) { FloatArray(frameSize) { 0.0f } }
            paddingFrames.forEach { frame ->
                finalizedUtterance = accumulator.processChunk(frame, false)
            }

            assertNotNull(finalizedUtterance)
            utterances.add(finalizedUtterance!!)
        }

        val firstUtterance = utterances.first()
        utterances.forEach { utterance ->
            assertTrue(utterance.contentEquals(firstUtterance))
        }
    }

    private fun loadPcm16Mono(inputStream: InputStream): ShortArray {
        val data = inputStream.readBytes()
        val header = data.copyOfRange(0, 44)
        val channels = header[22].toInt()
        val sampleRate = ((header[24].toInt() and 0xff) shl 0) or
            ((header[25].toInt() and 0xff) shl 8) or
            ((header[26].toInt() and 0xff) shl 16) or
            ((header[27].toInt() and 0xff) shl 24)
        val bitsPerSample = ((header[34].toInt() and 0xff) shl 0) or
            ((header[35].toInt() and 0xff) shl 8)
        val dataOffset = 44
        val dataSize = data.size - dataOffset
        require(channels == 1) { "Expected mono WAV fixture" }
        require(sampleRate == 16000) { "Expected 16kHz WAV fixture" }
        require(bitsPerSample == 16) { "Expected PCM16 WAV fixture" }

        val pcmBytes = data.copyOfRange(dataOffset, data.size)
        val shorts = ShortArray(pcmBytes.size / 2)
        for (index in shorts.indices) {
            val lo = pcmBytes[index * 2].toInt() and 0xff
            val hi = pcmBytes[index * 2 + 1].toInt() and 0xff
            shorts[index] = ((hi shl 8) or lo).toShort()
        }
        return shorts
    }
}
