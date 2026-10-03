package com.interviewtrail.app.platform

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

actual val defaultApiBaseUrl: String = "http://10.0.2.2:8080"   // emulator → host machine

private class AndroidSpeechToText(private val context: Context, private val askPermission: (() -> Unit) -> Unit) : SpeechToText {
    private var recognizer: SpeechRecognizer? = null

    override val isAvailable: Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    override fun start(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onPartialResult: ((String) -> Unit)?,
    ) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!granted) { askPermission { start(onResult, onError, onPartialResult) }; return }

        stop()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle) {
                    val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (text.isNullOrBlank()) onError("Didn't catch that. Try again.") else onResult(text)
                }
                override fun onError(error: Int) = onError(
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Didn't catch that. Try again."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Allow microphone access to dictate."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Voice input needs a connection."
                        else -> "Voice input stopped. Try again."
                    }
                )
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {
                    val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!text.isNullOrBlank()) {
                        onPartialResult?.invoke(text)
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")   // English only
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            })
        }
    }

    override fun stop() {
        recognizer?.destroy()
        recognizer = null
    }
}

@Composable
actual fun rememberSpeechToText(): SpeechToText {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pending?.invoke()
        pending = null
    }
    return remember(context) {
        AndroidSpeechToText(context) { retry ->
            pending = retry
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

@Composable
actual fun rememberShareSheet(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { text ->
            val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
            context.startActivity(Intent.createChooser(send, "Share invite").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

@Composable
actual fun rememberImagePicker(): ImagePicker {
    val context = LocalContext.current
    var onImageResult by remember { mutableStateOf<((ByteArray?) -> Unit)?>(null) }
    
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) {
            onImageResult?.invoke(null)
            return@rememberLauncherForActivityResult
        }
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        onImageResult?.invoke(bytes)
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap == null) {
            onImageResult?.invoke(null)
            return@rememberLauncherForActivityResult
        }
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        onImageResult?.invoke(stream.toByteArray())
    }

    return remember(context) {
        object : ImagePicker {
            override fun takePhoto(onResult: (ByteArray?) -> Unit) {
                onImageResult = onResult
                cameraLauncher.launch(null)
            }

            override fun pickImage(onResult: (ByteArray?) -> Unit) {
                onImageResult = onResult
                galleryLauncher.launch("image/*")
            }
        }
    }
}

/**
 * TODO: wire Credential Manager (androidx.credentials + googleid) with your Web client ID,
 * return GoogleIdTokenCredential.idToken. Returning null hides the Google button.
 */
@Composable
actual fun rememberGoogleSignIn(): (suspend () -> String?)? = null
