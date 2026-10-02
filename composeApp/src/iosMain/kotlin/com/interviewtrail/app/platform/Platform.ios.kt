package com.interviewtrail.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.*
import platform.Foundation.*
import platform.Speech.*
import platform.AVFAudio.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlinx.cinterop.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.darwin.NSObject
import platform.posix.memcpy

actual val defaultApiBaseUrl: String = "http://localhost:8080"   // simulator shares the host network

private class IosSpeechToText : SpeechToText {
    private val speechRecognizer = SFSpeechRecognizer(NSLocale(localeIdentifier = "en-IN"))
    private var audioEngine: AVAudioEngine? = null
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest? = null
    private var recognitionTask: SFSpeechRecognitionTask? = null

    override val isAvailable: Boolean
        get() = speechRecognizer?.isAvailable() == true

    @OptIn(ExperimentalForeignApi::class)
    override fun start(onResult: (String) -> Unit, onError: (String) -> Unit) {
        SFSpeechRecognizer.requestAuthorization { authStatus ->
            dispatch_async(dispatch_get_main_queue()) {
                if (authStatus == null || !authStatus.toString().contains("Authorized")) {
                    onError("Speech recognition not authorized.")
                    return@dispatch_async
                }

                stop()

                val engine = AVAudioEngine()
                audioEngine = engine
                val request = SFSpeechAudioBufferRecognitionRequest()
                recognitionRequest = request
                request.shouldReportPartialResults = false

                val node = engine.inputNode
                val recordingFormat = node.outputFormatForBus(0u)
                node.installTapOnBus(0u, 1024u, recordingFormat) { buffer, _ ->
                    if (buffer != null) {
                        request.appendAudioPCMBuffer(buffer)
                    }
                }

                runCatching {
                    engine.prepare()
                    engine.startAndReturnError(null)
                }.onFailure {
                    onError("Audio engine failed to start.")
                    stop()
                    return@dispatch_async
                }

                recognitionTask = speechRecognizer?.recognitionTaskWithRequest(request) { result, error ->
                    if (result != null) {
                        val text = result.bestTranscription.formattedString
                        if (text.isNotBlank()) {
                            onResult(text)
                            stop()
                        }
                    }
                    if (error != null) {
                        onError("Didn't catch that. Try again.")
                        stop()
                    }
                }
            }
        }
    }

    override fun stop() {
        runCatching {
            audioEngine?.stop()
            audioEngine?.inputNode?.removeTapOnBus(0u)
            recognitionRequest?.endAudio()
            recognitionTask?.cancel()
        }
        audioEngine = null
        recognitionRequest = null
        recognitionTask = null
    }
}

@Composable
actual fun rememberSpeechToText(): SpeechToText = remember { IosSpeechToText() }

@Composable
actual fun rememberShareSheet(): (String) -> Unit = remember {
    { text ->
        val sheet = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
        UIApplication.sharedApplication.keyWindow?.rootViewController
            ?.presentViewController(sheet, animated = true, completion = null)
    }
}

private class IosImagePickerDelegate(val onResult: (ByteArray?) -> Unit) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    @OptIn(ExperimentalForeignApi::class)
    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerEditedImage] as? UIImage
            ?: didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        
        picker.dismissViewControllerAnimated(true, null)
        
        if (image != null) {
            val data = UIImageJPEGRepresentation(image, 0.8)
            val bytes = data?.let { 
                val len = it.length.toInt()
                val byteArray = ByteArray(len)
                it.bytes?.let { ptr -> 
                    byteArray.usePinned { pinned ->
                        memcpy(pinned.addressOf(0), ptr, it.length)
                    }
                }
                byteArray
            }
            onResult(bytes)
        } else {
            onResult(null)
        }
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, null)
        onResult(null)
    }
}

@Composable
actual fun rememberImagePicker(): ImagePicker {
    return remember {
        object : ImagePicker {
            var delegate: IosImagePickerDelegate? = null
            
            override fun takePhoto(onResult: (ByteArray?) -> Unit) {
                if (UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)) {
                    val picker = UIImagePickerController()
                    picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                    delegate = IosImagePickerDelegate(onResult)
                    picker.delegate = delegate
                    UIApplication.sharedApplication.keyWindow?.rootViewController
                        ?.presentViewController(picker, animated = true, completion = null)
                } else {
                    onResult(null)
                }
            }

            override fun pickImage(onResult: (ByteArray?) -> Unit) {
                if (UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary)) {
                    val picker = UIImagePickerController()
                    picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
                    delegate = IosImagePickerDelegate(onResult)
                    picker.delegate = delegate
                    UIApplication.sharedApplication.keyWindow?.rootViewController
                        ?.presentViewController(picker, animated = true, completion = null)
                } else {
                    onResult(null)
                }
            }
        }
    }
}

@Composable
actual fun rememberGoogleSignIn(): (suspend () -> String?)? = null
