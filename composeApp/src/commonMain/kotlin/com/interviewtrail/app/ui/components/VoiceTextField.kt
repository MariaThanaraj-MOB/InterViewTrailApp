package com.interviewtrail.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var liveSpokenText by remember { mutableStateOf("") }
    val current by rememberUpdatedState(value)

    fun commitText(spoken: String) {
        val trimmed = spoken.trim()
        if (trimmed.isNotBlank()) {
            val sep = if (current.isBlank() || current.endsWith(" ")) "" else " "
            onValueChange(current + sep + trimmed)
        }
        speech.stop()
        listening = false
        liveSpokenText = ""
    }

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
                    liveSpokenText = ""
                    listening = true
                    speech.start(
                        onResult = { spoken ->
                            commitText(spoken)
                        },
                        onError = {
                            listening = false
                            voiceError = it
                        },
                        onPartialResult = { partial ->
                            liveSpokenText = partial
                        }
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
                if (liveSpokenText.isNotBlank()) {
                    commitText(liveSpokenText)
                } else {
                    speech.stop()
                    listening = false
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            val infiniteTransition = rememberInfiniteTransition()

            // Radar pulse animations
            val radarScale1 by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.35f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
            val radarAlpha1 by infiniteTransition.animateFloat(
                initialValue = 0.45f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                )
            )

            val radarScale2 by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1600, delayMillis = 400, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
            val radarAlpha2 by infiniteTransition.animateFloat(
                initialValue = 0.35f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1600, delayMillis = 400, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                )
            )

            // Dynamic equalizer bars
            val bar1 by infiniteTransition.animateFloat(
                initialValue = 8f, targetValue = 30f,
                animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse)
            )
            val bar2 by infiniteTransition.animateFloat(
                initialValue = 14f, targetValue = 42f,
                animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse)
            )
            val bar3 by infiniteTransition.animateFloat(
                initialValue = 6f, targetValue = 36f,
                animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse)
            )
            val bar4 by infiniteTransition.animateFloat(
                initialValue = 18f, targetValue = 48f,
                animationSpec = infiniteRepeatable(tween(720, easing = FastOutSlowInEasing), RepeatMode.Reverse)
            )
            val bar5 by infiniteTransition.animateFloat(
                initialValue = 10f, targetValue = 34f,
                animationSpec = infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse)
            )
            val bar6 by infiniteTransition.animateFloat(
                initialValue = 12f, targetValue = 40f,
                animationSpec = infiniteRepeatable(tween(540, easing = FastOutSlowInEasing), RepeatMode.Reverse)
            )
            val bar7 by infiniteTransition.animateFloat(
                initialValue = 8f, targetValue = 28f,
                animationSpec = infiniteRepeatable(tween(390, easing = FastOutSlowInEasing), RepeatMode.Reverse)
            )

            val cursorAlpha by infiniteTransition.animateFloat(
                initialValue = 1f, targetValue = 0f,
                animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse)
            )

            val dotAlpha by infiniteTransition.animateFloat(
                initialValue = 0.35f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp, top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // User Voice Identification Header Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E7D32).copy(alpha = dotAlpha))
                        )
                        Text(
                            text = "VOICE IDENTIFICATION ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Title & Target Field
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (liveSpokenText.isNotBlank()) "Recognizing Voice..." else "Listening...",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Dictating into: $label",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Voice Listening Loading Visualizer: Radar Circles + Mic Icon
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size((75 * radarScale2).dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = radarAlpha2))
                    )
                    Box(
                        modifier = Modifier
                            .size((65 * radarScale1).dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = radarAlpha1))
                    )
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 6.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Listening",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Dynamic Audio Waveform Equalizer (7 vertical animated wave bars)
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val barHeights = listOf(bar1, bar2, bar3, bar4, bar5, bar6, bar7)
                    barHeights.forEachIndexed { index, h ->
                        val barColor = if (index % 2 == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .height(h.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(barColor)
                        )
                    }
                }

                // Live Transcribed Speech Preview Box ("voice-to-text during voice speech")
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 88.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (liveSpokenText.isNotBlank()) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (liveSpokenText.isNotBlank()) "Live Speech-to-Text" else "Voice Input Status",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (liveSpokenText.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = liveSpokenText,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 22.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = " |",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = cursorAlpha)
                                    )
                                )
                            }
                        } else {
                            Text(
                                text = "Listening to your voice... Speak clearly, and your words will appear here in real time.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = FontStyle.Italic
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                // Action Buttons: Done/Insert and Stop/Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            speech.stop()
                            listening = false
                            liveSpokenText = ""
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            commitText(liveSpokenText)
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            if (liveSpokenText.isNotBlank()) Icons.Default.Check else Icons.Default.Stop,
                            contentDescription = "Done",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (liveSpokenText.isNotBlank()) "Insert Text" else "Stop")
                    }
                }
            }
        }
    }
}
