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

@OptIn(ExperimentalForeignApi::class)
private class IosSpeechToText : SpeechToText {
    private val speechRecognizer: SFSpeechRecognizer? =
        SFSpeechRecognizer(NSLocale(localeIdentifier = "en-IN"))
            ?: SFSpeechRecognizer(NSLocale(localeIdentifier = "en-US"))
            ?: SFSpeechRecognizer(NSLocale.currentLocale)

    private var audioEngine: AVAudioEngine? = null
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest? = null
    private var recognitionTask: SFSpeechRecognitionTask? = null

    override val isAvailable: Boolean
        get() = speechRecognizer?.isAvailable() == true

    @OptIn(ExperimentalForeignApi::class)
    override fun start(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onPartialResult: ((String) -> Unit)?,
    ) {
        val recognizer = speechRecognizer
        if (recognizer == null || !recognizer.isAvailable()) {
            onError("Speech recognition service is not available.")
            return
        }

        SFSpeechRecognizer.requestAuthorization { authStatus ->
            dispatch_async(dispatch_get_main_queue()) {
                if (authStatus != SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized) {
                    onError("Speech recognition permission denied.")
                    return@dispatch_async
                }

                val audioSession = AVAudioSession.sharedInstance()
                audioSession.requestRecordPermission { micGranted ->
                    dispatch_async(dispatch_get_main_queue()) {
                        if (!micGranted) {
                            onError("Microphone permission denied.")
                            return@dispatch_async
                        }

                        stop()

                        val setupSuccess = runCatching {
                            // Configure audio session for recording
                            audioSession.setCategory(
                                category = AVAudioSessionCategoryPlayAndRecord,
                                mode = AVAudioSessionModeMeasurement,
                                options = AVAudioSessionCategoryOptionDefaultToSpeaker,
                                error = null
                            )
                            audioSession.setActive(
                                true,
                                withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation,
                                error = null
                            )

                            val engine = AVAudioEngine()
                            audioEngine = engine
                            val node = engine.inputNode
                            node.removeTapOnBus(0u)

                            val recordingFormat = node.outputFormatForBus(0u)
                            if (recordingFormat.sampleRate <= 0.0 || recordingFormat.channelCount == 0u) {
                                onError("Microphone audio is unavailable on simulator. Please test voice on a physical device.")
                                stop()
                                return@dispatch_async
                            }

                            val request = SFSpeechAudioBufferRecognitionRequest()
                            recognitionRequest = request
                            request.shouldReportPartialResults = true

                            node.installTapOnBus(0u, 1024u, recordingFormat) { buffer, _ ->
                                if (buffer != null) {
                                    request.appendAudioPCMBuffer(buffer)
                                }
                            }

                            engine.prepare()
                            engine.startAndReturnError(null)

                            recognitionTask = recognizer.recognitionTaskWithRequest(request) { result, error ->
                                if (result != null) {
                                    val text = result.bestTranscription.formattedString
                                    if (text.isNotBlank()) {
                                        dispatch_async(dispatch_get_main_queue()) {
                                            onPartialResult?.invoke(text)
                                            if (result.isFinal()) {
                                                onResult(text)
                                                stop()
                                            }
                                        }
                                    }
                                }
                                if (error != null) {
                                    dispatch_async(dispatch_get_main_queue()) {
                                        onError("Didn't catch that. Try again.")
                                        stop()
                                    }
                                }
                            }
                        }.isSuccess

                        if (!setupSuccess) {
                            onError("Microphone audio is unavailable. Please verify microphone permissions or test on device.")
                            stop()
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun stop() {
        runCatching {
            audioEngine?.stop()
            audioEngine?.inputNode?.removeTapOnBus(0u)
            recognitionRequest?.endAudio()
            recognitionTask?.cancel()
            AVAudioSession.sharedInstance().setActive(
                false,
                withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation,
                error = null
            )
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
