package com.interviewtrail.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual val defaultApiBaseUrl: String = "http://localhost:8080"

// ---- Web Speech API (Chrome/Edge/Safari). English only. ----

private fun speechSupported(): Boolean =
    js("typeof window !== 'undefined' && !!(window.SpeechRecognition || window.webkitSpeechRecognition)")

private fun startRecognition(onText: (String) -> Unit, onErr: (String) -> Unit): JsAny = js("""(() => {
    const R = window.SpeechRecognition || window.webkitSpeechRecognition;
    const r = new R();
    r.lang = 'en-IN';
    r.interimResults = false;
    r.maxAlternatives = 1;
    r.onresult = (e) => onText(e.results[0][0].transcript);
    r.onerror = (e) => onErr(String(e.error));
    r.start();
    return r;
})()""")

private fun stopRecognition(r: JsAny): Unit = js("r.stop()")

private class WebSpeechToText : SpeechToText {
    private var current: JsAny? = null
    override val isAvailable: Boolean = speechSupported()

    override fun start(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onPartialResult: ((String) -> Unit)?,
    ) {
        stop()
        current = startRecognition(
            onText = { current = null; onResult(it) },
            onErr = { code ->
                current = null
                onError(when (code) {
                    "not-allowed", "service-not-allowed" -> "Allow microphone access in your browser to dictate."
                    "no-speech" -> "Didn't catch that. Try again."
                    else -> "Voice input stopped. Try again."
                })
            },
        )
    }

    override fun stop() {
        current?.let { stopRecognition(it) }
        current = null
    }
}

@Composable
actual fun rememberSpeechToText(): SpeechToText = remember { WebSpeechToText() }

// ---- Share: Web Share API, falling back to clipboard ----

private fun shareText(text: String): Unit = js("""(() => {
    if (navigator.share) { navigator.share({ text: text }).catch(() => {}); }
    else if (navigator.clipboard) { navigator.clipboard.writeText(text); window.alert('Invite link copied. Paste it into WhatsApp or email.'); }
})()""")

@Composable
actual fun rememberShareSheet(): (String) -> Unit = remember { { text -> shareText(text) } }

@Composable
actual fun rememberImagePicker(): ImagePicker {
    return remember {
        object : ImagePicker {
            override fun takePhoto(onResult: (ByteArray?) -> Unit) {
                onResult(null)
            }
            override fun pickImage(onResult: (ByteArray?) -> Unit) {
                onResult(null)
            }
        }
    }
}

/** TODO: Google Identity Services (accounts.google.com/gsi/client) → credential JWT. */
@Composable
actual fun rememberGoogleSignIn(): (suspend () -> String?)? = null
