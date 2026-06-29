package dev.barrycade.voicecore.stt

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

class AudioCapture(
    private val sampleRate: Int,
    private val bufferSize: Int
) {
    private var listener: ((ShortArray) -> Unit)? = null

    @Volatile
    var isRunning: Boolean = false
        private set

    private var audioRecord: AudioRecord? = null
    private var workerThread: Thread? = null

    @SuppressLint("MissingPermission")
    fun start() {
        if (isRunning) return

        if (bufferSize <= 0) return

        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufferSize <= 0) return

        val finalBufferSize = maxOf(bufferSize, minBufferSize)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            finalBufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord?.release()
            audioRecord = null
            return
        }

        audioRecord?.startRecording()

        isRunning = true
        workerThread = Thread {
            val buffer = ShortArray(finalBufferSize / 2)
            while (isRunning) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    listener?.invoke(buffer.copyOf(read))
                }
            }
        }
        workerThread?.start()
    }

    fun setOnAudioFrameListener(l: (ShortArray) -> Unit) {
        listener = l
    }

    fun stop() {
        if (!isRunning) return

        isRunning = false
        workerThread?.join(1000)
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        workerThread = null
    }
}
