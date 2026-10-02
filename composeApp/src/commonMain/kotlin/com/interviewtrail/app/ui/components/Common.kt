package com.interviewtrail.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.interviewtrail.app.LocalContainer
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ready<T>(val value: T) : Load<T>
    data class Failed(val message: String) : Load<Nothing>
}

/** Loads data for a screen; call `reload()` after mutations. */
class Loader<T>(private val scope: kotlinx.coroutines.CoroutineScope, private val block: suspend () -> T) {
    var state by mutableStateOf<Load<T>>(Load.Loading)
        private set

    fun reload() {
        scope.launch {
            if (state !is Load.Ready) state = Load.Loading
            state = runCatching { Load.Ready(block()) }
                .getOrElse { Load.Failed(it.message ?: "Couldn't load. Check your connection.") }
        }
    }
}

@Composable
fun <T> rememberLoader(vararg keys: Any?, block: suspend () -> T): Loader<T> {
    val scope = rememberCoroutineScope()
    val loader = remember(*keys) { Loader(scope, block) }
    LaunchedEffect(loader) { loader.reload() }
    return loader
}

@Composable
fun <T> LoadContent(loader: Loader<T>, content: @Composable (T) -> Unit) {
    when (val s = loader.state) {
        Load.Loading -> Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) {
            CircularProgressIndicator(strokeWidth = 2.dp)
        }
        is Load.Failed -> Column(
            Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(s.message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = loader::reload) { Text("Try again") }
        }
        is Load.Ready -> content(s.value)
    }
}

@Composable
fun EmptyState(message: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = onAction) { Text(action) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title, maxLines = 1) },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleBar(title: String, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
fun ErrorText(message: String?) {
    if (message != null) Text(message, color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))
}

fun today(): String = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

private val DATE = Regex("^\\d{4}-\\d{2}-\\d{2}$")
fun isIsoDate(s: String) = DATE.matches(s)

@Composable
fun NetworkImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    if (url.isBlank()) return
    val http = LocalContainer.current.http
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        runCatching {
            val bytes = http.get(url).body<ByteArray>()
            bitmap = bytes.decodeToImageBitmap()
        }.onFailure {
            failed = true
        }
    }

    when {
        bitmap != null -> Image(
            bitmap = bitmap!!,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
        !failed -> Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        }
        else -> Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Image, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun NetworkAvatar(
    url: String?,
    name: String,
    modifier: Modifier = Modifier.size(48.dp),
) {
    if (!url.isNullOrBlank()) {
        NetworkImage(
            url = url,
            contentDescription = name,
            modifier = modifier.clip(CircleShape),
        )
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.take(1).uppercase().ifBlank { "?" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

/**
 * Standard, reusable dropdown component for all master lists across the application.
 * Always includes an "Others" option.
 * Selecting "Others" opens an AlertDialog prompting the user to enter a custom value.
 * Submitting the custom value dynamically appends it to the master list, immediately
 * updates the dropdown UI, and sets it as the user's selected value.
 */
@Composable
fun MasterDropdownWithOthers(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select $label",
    onAddNewOption: ((String) -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var customInput by remember { mutableStateOf("") }
    val masterList = remember(options) { mutableStateListOf<String>().apply { addAll(options) } }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (selectedValue.isNotBlank()) selectedValue else placeholder,
                    color = if (selectedValue.isNotBlank()) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text("▼", style = MaterialTheme.typography.labelSmall)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f),
        ) {
            val allItems = remember(masterList.toList()) {
                val list = masterList.toList().toMutableList()
                if ("Others" !in list) list.add("Others")
                list
            }
            allItems.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = item,
                            fontWeight = if (item == "Others") androidx.compose.ui.text.font.FontWeight.Bold else null,
                            color = if (item == "Others") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = {
                        expanded = false
                        if (item == "Others") {
                            customInput = ""
                            showDialog = true
                        } else {
                            onValueChange(item)
                        }
                    },
                )
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add Custom $label") },
            text = {
                Column {
                    Text("Enter custom $label to add to the master list:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customInput,
                        onValueChange = { customInput = it },
                        label = { Text("New $label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = customInput.isNotBlank(),
                    onClick = {
                        val newValue = customInput.trim()
                        if (newValue.isNotBlank()) {
                            if (newValue !in masterList) {
                                masterList.add(newValue)
                            }
                            onAddNewOption?.invoke(newValue)
                            onValueChange(newValue)
                        }
                        showDialog = false
                    },
                ) { Text("Add & Select") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            },
        )
    }
}
