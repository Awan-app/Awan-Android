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
import java.util.concurrent.atomic.AtomicReference

class SpeechRecognizerState internal constructor(
    val isListening: Boolean,
    val errorMessage: String?,
    val isPermissionError: Boolean,
    /**
     * How loud the microphone is hearing you, 0..1, updated many times a second while listening.
     *
     * Deliberately a lambda rather than a value: read it inside `graphicsLayer`/`drawBehind` so the
     * level drives a frame without recomposing anything. Reading it in a composable body instead
     * would recompose the caller on every audio frame.
     */
    val amplitude: () -> Float,
    private val startListeningAction: () -> Unit,
    private val stopListeningAction: () -> Unit,
    private val clearErrorAction: () -> Unit,
) {
    fun startListening() = startListeningAction()
    fun stopListening() = stopListeningAction()
    fun clearError() = clearErrorAction()
}

/**
 * `onRmsChanged` reports roughly -2 dB (silence) to 10 dB (loud) — the range is not documented as a
 * contract, so it is clamped rather than trusted.
 */
private const val RMS_FLOOR_DB = -2f
private const val RMS_CEILING_DB = 10f

private fun normalizeRms(rmsdB: Float): Float =
    ((rmsdB - RMS_FLOOR_DB) / (RMS_CEILING_DB - RMS_FLOOR_DB)).coerceIn(0f, 1f)

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
    // Not State: the level changes on every audio frame, and nothing should recompose for it.
    val amplitude = remember { AtomicReference(0f) }

    /**
     * The app's language, not the device's. MainActivity overrides [LocalConfiguration] from the
     * stored locale preference; `Locale.getDefault()` only ever reports the system locale, which is
     * why an English phone running the app in Arabic used to be transcribed as English.
     */
    val languageTag = LocalConfiguration.current.locales[0].toLanguageTag()
    val latestText by rememberUpdatedState(currentText)
    val currentOnTranscript by rememberUpdatedState(onTranscript)
    val currentOnSetMicPermissionRequested by rememberUpdatedState(onSetMicPermissionRequested)

    // Resolved here rather than inside the listener: the recognizer's callbacks are not composable,
    // and the activity's resources are not guaranteed to carry the app locale the composition does.
    val unavailableText = stringResource(R.string.ds_speech_unavailable)
    val noMatchText = stringResource(R.string.ds_speech_no_match)
    val errorText = stringResource(R.string.ds_speech_error)
    val languageUnsupportedText = stringResource(R.string.ds_speech_language_unsupported)
    val permissionDeniedText = stringResource(R.string.ds_speech_permission_denied)

    fun stopInternal() {
        recognizer?.stopListening()
        amplitude.set(0f)
        isListening = false
    }

    fun startListeningNow() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            errorMessage = unavailableText
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
            override fun onRmsChanged(rmsdB: Float) = amplitude.set(normalizeRms(rmsdB))
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() = amplitude.set(0f)

            override fun onError(error: Int) {
                amplitude.set(0f)
                isListening = false
                when (error) {
                    SpeechRecognizer.ERROR_CLIENT -> {
                        // Silent when cancelled by user/app
                    }
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        isPermissionError = true
                        errorMessage = permissionDeniedText
                    }
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        errorMessage = noMatchText
                    }
                    else -> {
                        errorMessage = if (isLanguageError(error)) languageUnsupportedText else errorText
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                amplitude.set(0f)
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    currentOnTranscript(prefix + matches[0])
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    currentOnTranscript(prefix + matches[0])
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        isListening = true
        try {
            activeRecognizer.startListening(intent)
        } catch (e: RuntimeException) {
            isListening = false
            errorMessage = errorText
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            currentOnSetMicPermissionRequested(false)
            isPermissionError = false
            errorMessage = null
            startListeningNow()
        } else {
            currentOnSetMicPermissionRequested(true)
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
                errorMessage = permissionDeniedText
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
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            },
            dismissLabel = stringResource(R.string.ds_speech_settings_dialog_cancel),
            onDismiss = {
                showSettingsDialog = false
                isPermissionError = true
                errorMessage = permissionDeniedText
            },
        )
    }

    return remember(isListening, errorMessage, isPermissionError, hasRequestedMicPermission) {
        SpeechRecognizerState(
            isListening = isListening,
            errorMessage = errorMessage,
            isPermissionError = isPermissionError,
            amplitude = amplitude::get,
            startListeningAction = {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    if (hasRequestedMicPermission) {
                        currentOnSetMicPermissionRequested(false)
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
