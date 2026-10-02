package com.interviewtrail.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import com.interviewtrail.app.platform.rememberSpeechToText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    placeholder: String? = null,
    supportingText: String? = null,
    voice: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val speech = rememberSpeechToText()
    var listening by remember { mutableStateOf(false) }
    var voiceError by remember { mutableStateOf<String?>(null) }
    val current by rememberUpdatedState(value)

    DisposableEffect(Unit) { onDispose { speech.stop() } }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        modifier = modifier,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        isError = voiceError != null,
        supportingText = (voiceError ?: supportingText)?.let { { Text(it) } },
        trailingIcon = if (voice && speech.isAvailable) {
            {
                IconButton(onClick = {
                    voiceError = null
                    listening = true
                    speech.start(
                        onResult = { spoken ->
                            listening = false
                            val sep = if (current.isBlank() || current.endsWith(" ")) "" else " "
                            onValueChange(current + sep + spoken)
                        },
                        onError = { listening = false; voiceError = it },
                    )
                }) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Dictate $label",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        } else null,
    )

    if (listening) {
        ModalBottomSheet(
            onDismissRequest = {
                speech.stop()
                listening = false
            },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Listening...",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Pulsing animation mimicking Gemini voice
                val infiniteTransition = rememberInfiniteTransition()
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size((60 * scale).dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Listening",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        speech.stop()
                        listening = false
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.padding(end = 8.dp))
                    Text("Stop")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
