package com.example.facenav.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Handles voice recognition for emergency kill switch
 */
class VoiceCommandHandler(
    private val context: Context,
    private val killSwitch: EmergencyKillSwitch
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    companion object {
        private const val TAG = "VoiceCommandHandler"
    }

    /**
     * Start listening for voice commands
     */
    fun startListening() {
        if (isListening) {
            Log.d(TAG, "Already listening")
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
                isListening = true
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Speech started")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Audio level changed
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Buffer received
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "Speech ended")
                isListening = false
            }

            override fun onError(error: Int) {
                Log.e(TAG, "Speech recognition error: $error")
                isListening = false

                // Only restart on transient errors where the recogniser is
                // still usable. ERROR_SPEECH_TIMEOUT and ERROR_NO_MATCH are
                // normal "no speech detected" conditions and must NOT trigger
                // an immediate restart loop (Issue 16).
                val restartable = error == SpeechRecognizer.ERROR_NETWORK ||
                        error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT ||
                        error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                        error == SpeechRecognizer.ERROR_SERVER

                if (restartable) {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        startListening()
                    }, 2000)
                } else if (error != SpeechRecognizer.ERROR_CLIENT) {
                    // For no-speech / no-match, wait longer before retrying
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        startListening()
                    }, 5000)
                }
            }

            override fun onResults(results: Bundle?) {
                handleResults(results)
                // Restart listening
                startListening()
            }

            override fun onPartialResults(results: Bundle?) {
                // Handle partial results for faster response
                handleResults(results)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Event occurred
            }

            private fun handleResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.forEach { text ->
                        val safe = text.replace("\n", " ").replace("\r", " ")
                        Log.d(TAG, "Recognized: $safe")
                        if (killSwitch.checkVoiceCommand(text)) {
                            Log.d(TAG, "Kill switch triggered by voice!")
                        }
                    }
            }
        })

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            isListening = false
        }
    }

    /**
     * Stop listening for voice commands
     */
    fun stopListening() {
        isListening = false
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        Log.d(TAG, "Stopped listening")
    }

    /**
     * Check if currently listening
     */
    fun isListening(): Boolean = isListening
}