package dev.barrycade.voicecore.stt

import android.content.Context

class SpeechToText(
	private val context: Context,
	private val config: SttConfig = SttConfig()
) {
	private val stateLock = Any()
	private var onResult: ((String) -> Unit)? = null
	private var onError: ((Throwable) -> Unit)? = null

	@Volatile
	private var running = false

	private var audioCapture: AudioCapture? = null
	private var nativeSession: NativeSession? = null

	fun start() {
		val modelPath = config.modelPath
		if (modelPath.isNullOrBlank()) {
			dispatchError(IllegalArgumentException("SttConfig.modelPath is required"))
			return
		}

		synchronized(stateLock) {
			if (running) return

			try {
				val session = NativeSession()
				session.loadModel(modelPath)

				val capture = AudioCapture(
					sampleRate = config.sampleRate,
					bufferSize = config.bufferSize
				)
				capture.setOnAudioFrameListener { frame ->
					try {
						val text = session.transcribe(frame).trim()
						if (text.isNotEmpty()) {
							onResult?.invoke(text)
						}
					} catch (t: Throwable) {
						dispatchError(t)
					}
				}

				nativeSession = session
				audioCapture = capture
				running = true
				capture.start()
			} catch (t: Throwable) {
				running = false
				try {
					audioCapture?.stop()
				} catch (_: Throwable) {
				}
				audioCapture = null
				try {
					nativeSession?.close()
				} catch (_: Throwable) {
				}
				nativeSession = null
				dispatchError(t)
			}
		}
	}

	fun stop() {
		synchronized(stateLock) {
			if (!running && audioCapture == null && nativeSession == null) {
				return
			}

			running = false

			try {
				audioCapture?.stop()
			} finally {
				audioCapture = null
			}

			try {
				nativeSession?.close()
			} finally {
				nativeSession = null
			}
		}
	}

	fun setOnResultListener(listener: (String) -> Unit) {
		onResult = listener
	}

	fun setOnErrorListener(listener: (Throwable) -> Unit) {
		onError = listener
	}

	private fun dispatchError(t: Throwable) {
		onError?.invoke(t)
	}

	private class NativeSession {
		fun loadModel(modelPath: String) {
			WhisperBridge.loadModel(modelPath)
		}

		fun transcribe(samples: ShortArray): String {
			return WhisperBridge.transcribe(samples)
		}

		fun close() {
			WhisperBridge.unloadModel()
		}
	}
}
