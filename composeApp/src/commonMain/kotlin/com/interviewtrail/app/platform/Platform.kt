package com.interviewtrail.app.platform

import androidx.compose.runtime.Composable

/** Backend URL default per platform (Android emulator can't reach "localhost"). */
expect val defaultApiBaseUrl: String

/**
 * English-only voice-to-text, used by every text/description field.
 * The user speaks, text is inserted, they review before posting.
 */
interface SpeechToText {
    val isAvailable: Boolean
    fun start(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onPartialResult: ((String) -> Unit)? = null,
    )
    fun stop()
}

@Composable
expect fun rememberSpeechToText(): SpeechToText

/** Opens the native share sheet (WhatsApp, Mail, …) with the given text, e.g. an invite link. */
@Composable
expect fun rememberShareSheet(): (text: String) -> Unit

/**
 * Image picker providing two options: take a photo with the camera, or pick from gallery/folder.
 * Returns the image as a ByteArray (JPEG encoded).
 */
interface ImagePicker {
    fun takePhoto(onResult: (ByteArray?) -> Unit)
    fun pickImage(onResult: (ByteArray?) -> Unit)
}

@Composable
expect fun rememberImagePicker(): ImagePicker

/**
 * Launches the platform Google sign-in and returns a Google ID token,
 * or null when not wired up yet on this platform.
 */
@Composable
expect fun rememberGoogleSignIn(): (suspend () -> String?)?
