package dev.barrycade.voicecore.stt

import android.content.Context
import android.util.Log
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * SpeechToText implements an industry-standard sliding-window FIFO pipeline for Whisper.
 * It ensures continuous transcription with CHUNK=3s, OVERLAP=1s, and STEP=2s.
 */
class SpeechToText(
    @Suppress("UNUSED_PARAMETER") private val context: Context,
    private val config: SttConfig = SttConfig()
) {
    companion object {
        private const val TAG = "STT_FIFO"
        private const val CHUNK = 48000  // 3 seconds @ 16kHz
        private const val OVERLAP = 16000 // 1 second @ 16kHz
        private const val STEP = 32000    // 2 seconds @ 16kHz
    }

    private var onResult: ((String) -> Unit)? = null
    private var onError: ((Throwable) -> Unit)? = null

    private val isRunning = AtomicBoolean(false)
    private val stateLock = Any()

    private var audioCapture: AudioCapture? = null
    private var nativeSession: NativeSession? = null

    private val audioQueue: BlockingQueue<ShortArray> = LinkedBlockingQueue()
    private var inferenceWorker: ExecutorService? = null

    private val transcriptBuilder = StringBuilder()
    private val transcriptLock = Any()

    // High-performance primitive FIFO buffer
    private val fifo = PrimitiveFifo(CHUNK * 4)

    fun setOnResultListener(listener: (String) -> Unit) {
        onResult = listener
    }

    fun setOnErrorListener(listener: (Throwable) -> Unit) {
        onError = listener
    }

    fun start() {
        synchronized(stateLock) {
            if (isRunning.get()) return

            val modelPath = config.modelPath ?: throw IllegalArgumentException("modelPath required")

            try {
                resetInternalState()
                nativeSession = NativeSession(config.debugInstrumentation).apply { loadModel(modelPath) }
                
                isRunning.set(true)
                startInferenceWorker()

                audioCapture = AudioCapture(
                    sampleRate = config.sampleRate,
                    requestedBufferSizeInBytes = config.bufferSize
                ).apply {
                    setOnAudioFrameListener { frame ->
                        if (!audioQueue.offer(frame)) {
                            Log.w(TAG, "Audio FIFO overflow")
                        }
                    }
                    start()
                }
                Log.d(TAG, "Industry-standard FIFO Pipeline started")
            } catch (t: Throwable) {
                stopInternal()
                dispatchError(t)
            }
        }
    }

    fun stopAndTranscribe() {
        synchronized(stateLock) {
            if (!isRunning.get()) return
            isRunning.set(false)

            try {
                audioCapture?.stop()
                audioCapture = null

                inferenceWorker?.shutdown()
                val finished = inferenceWorker?.awaitTermination(20, TimeUnit.SECONDS) ?: true
                if (!finished) {
                    Log.w(TAG, "Inference worker timeout during drain")
                    inferenceWorker?.shutdownNow()
                }
                inferenceWorker = null

                val finalText = synchronized(transcriptLock) {
                    transcriptBuilder.toString().trim()
                }
                onResult?.invoke(finalText)
            } catch (t: Throwable) {
                dispatchError(t)
            } finally {
                stopInternal()
            }
        }
    }

    fun stop() = stopAndTranscribe()
    fun transcribeRecorded() = stopAndTranscribe()

    private fun resetInternalState() {
        audioQueue.clear()
        synchronized(fifo) { fifo.clear() }
        synchronized(transcriptLock) { transcriptBuilder.setLength(0) }
    }

    private fun startInferenceWorker() {
        inferenceWorker = Executors.newSingleThreadExecutor()
        inferenceWorker?.execute {
            while (isRunning.get() || audioQueue.isNotEmpty()) {
                try {
                    val frame = audioQueue.poll(100, TimeUnit.MILLISECONDS)
                    if (frame != null) {
                        synchronized(fifo) { fifo.write(frame) }
                    }

                    // Sliding-window chunking loop
                    synchronized(fifo) {
                        while (fifo.size >= CHUNK) {
                            if (!isRunning.get()) Log.d(TAG, "STOP: draining chunk with overlap")
                            
                            val chunk = fifo.take(CHUNK)
                            Log.d(TAG, "FIFO: chunk extracted (${chunk.size} samples)")
                            
                            val text = nativeSession?.transcribe(chunk)?.trim().orEmpty()
                            appendNewText(text)

                            fifo.discard(STEP)
                            Log.d(TAG, "FIFO: discarding stepSizeSamples ($STEP)")
                            Log.d(TAG, "FIFO: retaining overlapSizeSamples ($OVERLAP)")
                        }
                    }

                    // STOP-tail logic
                    if (!isRunning.get() && audioQueue.isEmpty()) {
                        synchronized(fifo) {
                            if (fifo.size > 0) {
                                Log.d(TAG, "STOP: tail chunk with overlap applied")
                                val rawTail = fifo.takeAll()
                                val tail = if (rawTail.size < STEP) {
                                    Log.d(TAG, "STOP: padding tail (${rawTail.size} samples) to $STEP")
                                    ShortArray(STEP).apply {
                                        System.arraycopy(rawTail, 0, this, 0, rawTail.size)
                                    }
                                } else {
                                    rawTail
                                }
                                val text = nativeSession?.transcribe(tail)?.trim().orEmpty()
                                appendNewText(text)
                            }
                        }
                    }
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                } catch (t: Throwable) {
                    dispatchError(t)
                }
            }
            Log.d(TAG, "Inference worker finished")
        }
    }

    private fun appendNewText(newText: String) {
        synchronized(transcriptLock) {
            val existing = transcriptBuilder.toString().trim()
            if (existing.isEmpty()) {
                transcriptBuilder.append(newText.trim())
                return
            }

            // Normalise for semantic comparison
            fun norm(s: String) =
                s.lowercase()
                    .replace(".", "")
                    .replace(",", "")
                    .replace("!", "")
                    .replace("?", "")
                    .trim()

            val existingNorm = norm(existing)
            val newNorm = norm(newText)

            // If the new chunk ends with the same meaning as the existing transcript,
            // drop the repeated part entirely.
            if (existingNorm.endsWith(newNorm)) {
                return
            }

            // Word-level dedupe fallback
            val existingWords = existing.split(" ")
            val newWords = newText.trim().split(" ")

            val maxOverlap = minOf(8, newWords.size)
            var overlap = 0

            for (i in 1..maxOverlap) {
                val tail = existingWords.takeLast(i).joinToString(" ")
                val head = newWords.take(i).joinToString(" ")

                if (norm(tail).startsWith(norm(head).take(2)) ||
                    norm(head).startsWith(norm(tail).take(2))) {
                    overlap = i
                }
            }

            val deduped = newWords.drop(overlap).joinToString(" ")
            if (deduped.isNotBlank()) {
                transcriptBuilder.append(" ").append(deduped)
            }
        }
    }

    private fun stopInternal() {
        isRunning.set(false)
        audioCapture?.stop()
        audioCapture = null
        inferenceWorker?.shutdownNow()
        inferenceWorker = null
        val session = nativeSession
        nativeSession = null
        Thread { try { session?.close() } catch(_: Exception) {} }.start()
    }

    private fun dispatchError(t: Throwable) = onError?.invoke(t)

    private class NativeSession(private val debug: Boolean) {
        fun loadModel(path: String) {
            if (debug) Log.d("Whisper", "Loading: $path")
            WhisperBridge.loadModel(path)
        }
        fun transcribe(pcm: ShortArray): String = WhisperBridge.transcribe(pcm)
        fun close() {
            if (debug) Log.d("Whisper", "Unloading model")
            WhisperBridge.unloadModel()
        }
    }

    /**
     * Primitive FIFO buffer for high-performance audio processing.
     * Prevents boxing/unboxing overhead of MutableList<Short>.
     */
    private class PrimitiveFifo(capacity: Int) {
        private var buffer = ShortArray(capacity)
        private var head = 0
        private var count = 0

        val size: Int get() = count

        fun write(data: ShortArray) {
            if (count + data.size > buffer.size) {
                // Grow buffer
                val newBuffer = ShortArray(maxOf(buffer.size * 2, count + data.size))
                System.arraycopy(buffer, head, newBuffer, 0, count)
                buffer = newBuffer
                head = 0
            }
            System.arraycopy(data, 0, buffer, head + count, data.size)
            count += data.size
        }

        fun take(n: Int): ShortArray {
            val result = ShortArray(n)
            System.arraycopy(buffer, head, result, 0, n)
            return result
        }

        fun takeAll(): ShortArray {
            val result = ShortArray(count)
            System.arraycopy(buffer, head, result, 0, count)
            count = 0
            head = 0
            return result
        }

        fun discard(n: Int) {
            if (n >= count) {
                count = 0
                head = 0
            } else {
                head += n
                count -= n
                // Compact if head gets too far
                if (head > buffer.size / 2) {
                    System.arraycopy(buffer, head, buffer, 0, count)
                    head = 0
                }
            }
        }

        fun clear() {
            count = 0
            head = 0
        }
    }
}
