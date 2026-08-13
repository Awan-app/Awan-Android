package com.awan.app.core.designsystem

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class SpeechRecognizerState internal constructor(
    val isListening: Boolean,
    val errorMessage: String?,
    val isPermissionError: Boolean,
    private val startListeningAction: () -> Unit,
    private val stopListeningAction: () -> Unit,
    private val clearErrorAction: () -> Unit,
) {
    fun startListening() = startListeningAction()
    fun stopListening() = stopListeningAction()
    fun clearError() = clearErrorAction()
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Dictation for any text field, paired with [AwanMicButton].
 *
 * [currentText] is read when listening starts and the transcript is appended to it, so speaking
 * after typing extends the sentence instead of replacing it. Pass the field's current value.
 *
 * The permission is never requested cold: the first tap opens an in-app explanation, and only
 * accepting that launches the system prompt. That ordering is what makes the settings-screen
 * fallback after a permanent denial read as a consequence of a choice the user made.
 */
@Composable
fun rememberSpeechRecognizer(
    onTranscript: (String) -> Unit,
    currentText: () -> String = { "" },
    hasRequestedMicPermission: Boolean = false,
    onSetMicPermissionRequested: (Boolean) -> Unit = {},
): SpeechRecognizerState {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPermissionError by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showRationaleDialog by remember { mutableStateOf(false) }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    /**
     * The app's language, not the device's. MainActivity overrides [LocalConfiguration] from the
     * stored locale preference; `Locale.getDefault()` only ever reports the system locale, which is
     * why an English phone running the app in Arabic used to be transcribed as English.
     */
    val languageTag = LocalConfiguration.current.locales[0].toLanguageTag()
    val latestText by rememberUpdatedState(currentText)

    fun stopInternal() {
        recognizer?.apply {
            stopListening()
            cancel()
        }
        isListening = false
    }

    fun startListeningNow() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            errorMessage = context.resources.getString(R.string.ds_speech_unavailable)
            isPermissionError = false
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            // Without this the engine may fall back to its own preferred language rather than ours.
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        var activeRecognizer = recognizer
        if (activeRecognizer == null) {
            activeRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer = activeRecognizer
        }

        // Frozen for this utterance so partial results keep rewriting the same tail rather than
        // stacking every interim guess onto the previous one.
        val base = latestText().trimEnd()
        val prefix = if (base.isEmpty()) "" else "$base "

        activeRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                errorMessage = null
                isPermissionError = false
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
                        errorMessage = context.resources.getString(R.string.ds_speech_no_match)
                    }
                    else -> {
                        errorMessage = if (isLanguageError(error)) {
                            context.resources.getString(R.string.ds_speech_language_unsupported)
                        } else {
                            context.resources.getString(R.string.ds_speech_error)
                        }
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onTranscript(prefix + matches[0])
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onTranscript(prefix + matches[0])
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        isListening = true
        try {
            activeRecognizer.startListening(intent)
        } catch (e: RuntimeException) {
            isListening = false
            errorMessage = context.resources.getString(R.string.ds_speech_error)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            onSetMicPermissionRequested(false)
            isPermissionError = false
            errorMessage = null
            startListeningNow()
        } else {
            val activity = context.findActivity()
            val shouldShowRationale = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.RECORD_AUDIO,
            )
            if (!shouldShowRationale) {
                showSettingsDialog = true
                isPermissionError = false
                errorMessage = null
            } else {
                isPermissionError = true
                errorMessage = context.resources.getString(R.string.ds_speech_permission_denied)
            }
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

    if (showRationaleDialog) {
        AwanConfirmDialog(
            title = stringResource(R.string.ds_speech_rationale_title),
            body = stringResource(R.string.ds_speech_rationale_body),
            confirmLabel = stringResource(R.string.ds_speech_rationale_confirm),
            onConfirm = {
                showRationaleDialog = false
                onSetMicPermissionRequested(true)
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            dismissLabel = stringResource(R.string.ds_speech_rationale_cancel),
            onDismiss = { showRationaleDialog = false },
        )
    }

    if (showSettingsDialog) {
        AwanConfirmDialog(
            title = stringResource(R.string.ds_speech_settings_dialog_title),
            body = stringResource(R.string.ds_speech_settings_dialog_body),
            confirmLabel = stringResource(R.string.ds_speech_settings_dialog_confirm),
            onConfirm = {
                showSettingsDialog = false
                isPermissionError = false
                errorMessage = null
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            dismissLabel = stringResource(R.string.ds_speech_settings_dialog_cancel),
            onDismiss = {
                showSettingsDialog = false
                isPermissionError = true
                errorMessage = context.resources.getString(R.string.ds_speech_permission_denied)
            },
        )
    }

    return remember(isListening, errorMessage, isPermissionError, hasRequestedMicPermission) {
        SpeechRecognizerState(
            isListening = isListening,
            errorMessage = errorMessage,
            isPermissionError = isPermissionError,
            startListeningAction = {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    if (hasRequestedMicPermission) {
                        onSetMicPermissionRequested(false)
                    }
                    isPermissionError = false
                    errorMessage = null
                    startListeningNow()
                } else {
                    val activity = context.findActivity()
                    val shouldShowRationale = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.RECORD_AUDIO,
                    )
                    val isFirstRequest = !hasRequestedMicPermission

                    // No error is shown yet — nothing has been denied. Reddening the field here
                    // marks the request as failed while the system prompt is still unanswered.
                    isPermissionError = false
                    errorMessage = null

                    if (isFirstRequest || shouldShowRationale) {
                        showRationaleDialog = true
                    } else {
                        showSettingsDialog = true
                    }
                }
            },
            stopListeningAction = { stopInternal() },
            clearErrorAction = {
                errorMessage = null
                isPermissionError = false
            },
        )
    }
}

/** Both codes are API 33+; on older devices the engine reports a generic error instead. */
private fun isLanguageError(error: Int): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        (error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED || error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)
