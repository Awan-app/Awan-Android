package com.awan.feature.addtask.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.awan.feature.addtask.R
import java.util.Locale

class SpeechRecognizerState internal constructor(
    val isListening: Boolean,
    val errorMessage: String?,
    private val startListeningAction: () -> Unit,
    private val stopListeningAction: () -> Unit,
) {
    fun startListening() = startListeningAction()
    fun stopListening() = stopListeningAction()
}

@Composable
fun rememberSpeechRecognizer(
    onTranscript: (String) -> Unit,
): SpeechRecognizerState {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    fun stopInternal() {
        recognizer?.apply {
            stopListening()
            cancel()
        }
        isListening = false
    }

    fun startListeningNow() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            errorMessage = context.getString(R.string.add_task_goal_speech_unavailable)
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        var activeRecognizer = recognizer
        if (activeRecognizer == null) {
            activeRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer = activeRecognizer
        }

        activeRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                errorMessage = null
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                isListening = false
                when (error) {
                    SpeechRecognizer.ERROR_CLIENT -> {
                        // Silent when cancelled by user/app
                    }
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        errorMessage = context.getString(R.string.add_task_goal_speech_no_match)
                    }
                    else -> {
                        errorMessage = context.getString(R.string.add_task_goal_speech_error)
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onTranscript(matches[0])
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onTranscript(matches[0])
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        isListening = true
        try {
            activeRecognizer.startListening(intent)
        } catch (e: RuntimeException) {
            isListening = false
            errorMessage = context.getString(R.string.add_task_goal_speech_error)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            startListeningNow()
        } else {
            errorMessage = context.getString(R.string.add_task_goal_speech_permission_denied)
        }
    }

    DisposableEffect(context) {
        onDispose {
            recognizer?.apply {
                stopListening()
                cancel()
                destroy()
            }
            recognizer = null
        }
    }

    return remember(isListening, errorMessage) {
        SpeechRecognizerState(
            isListening = isListening,
            errorMessage = errorMessage,
            startListeningAction = {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    startListeningNow()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            stopListeningAction = { stopInternal() },
        )
    }
}
