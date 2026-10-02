package com.interviewtrail.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.interviewtrail.app.LocalContainer
import com.interviewtrail.app.data.*
import com.interviewtrail.app.platform.rememberImagePicker
import com.interviewtrail.app.ui.components.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/** Day-by-day learning journals: tech Learning Spaces + invite-only Learning Groups. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningScreen() {
    val nav = LocalNav.current
    var showGroups by remember { mutableStateOf(false) }

    Column {
        TitleBar("Learning")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            SegmentedButton(!showGroups, { showGroups = false }, SegmentedButtonDefaults.itemShape(0, 2)) { Text("Tech spaces") }
            SegmentedButton(showGroups, { showGroups = true }, SegmentedButtonDefaults.itemShape(1, 2)) { Text("Learning groups") }
        }
        if (showGroups) {
            GroupList(GroupKind.LEARNING, "Learn alongside friends. Everyone logs what they studied each day.") {
                nav.push(Screen.LearningGroupDetail(it))
            }
        } else SpacesList()
    }
}

@Composable
private fun SpacesList() {
    val api = LocalContainer.current.api
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val loader = rememberLoader { api.learningSpaces() to api.me().subscribedLearningSpaces.toSet() }

    LoadContent(loader) { (spaces, subscribed) ->
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("Subscribe to a technology to post your daily progress and see others'.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(spaces, key = { it.id }) { s ->
                val on = s.id in subscribed
                RowItem(s.name, if (on) "Subscribed" else null, onClick = { nav.push(Screen.LearningSpaceDetail(s)) }) {
                    TextButton(onClick = {
                        scope.launch {
                            runCatching { if (on) api.unsubscribe(s.id) else api.subscribe(s.id) }
                            loader.reload()
                        }
                    }) { Text(if (on) "Unsubscribe" else "Subscribe") }
                }
            }
        }
    }
}

@Composable
fun LearningSpaceScreen(space: Community) {
    val api = LocalContainer.current.api
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    var subscribed by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(space.id) { subscribed = runCatching { space.id in api.me().subscribedLearningSpaces }.getOrNull() }

    LearningFeed(
        title = space.name,
        load = { needsHelp -> api.spacePosts(space.id, needsHelp) },
        keys = space.id,
        canPost = subscribed == true,
        onCompose = { nav.push(Screen.CreateLearningPost(space.id, null, space.name)) },
        actions = {
            if (subscribed == false) TextButton(onClick = {
                scope.launch { runCatching { api.subscribe(space.id) }.onSuccess { subscribed = true } }
            }) { Text("Subscribe") }
        },
    )
}

@Composable
fun LearningGroupScreen(group: Group) {
    val api = LocalContainer.current.api
    val nav = LocalNav.current
    val me = LocalMyUid.current
    var inviting by remember { mutableStateOf(false) }
    LearningFeed(
        title = group.name,
        load = { needsHelp -> api.learningGroupPosts(group.id, needsHelp) },
        keys = group.id,
        canPost = true,
        onCompose = { nav.push(Screen.CreateLearningPost(null, group.id, group.name)) },
        actions = {
            if (group.ownerId == me) IconButton(onClick = { inviting = true }) { Icon(Icons.Default.PersonAdd, "Invite") }
        },
    )
    if (inviting) InviteDialog(group) { inviting = false }
}

@Composable
private fun LearningFeed(
    title: String,
    load: suspend (needsHelp: Boolean) -> List<LearningPost>,
    keys: Any,
    canPost: Boolean,
    onCompose: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    val api = LocalContainer.current.api
    val nav = LocalNav.current
    val me = LocalMyUid.current
    val scope = rememberCoroutineScope()
    var needsHelp by remember { mutableStateOf(false) }
    val loader = rememberLoader(keys, needsHelp) { load(needsHelp) }

    Scaffold(
        topBar = { BackBar(title, nav::pop, actions) },
        floatingActionButton = {
            if (canPost) ExtendedFloatingActionButton(onClick = onCompose,
                icon = { Icon(Icons.Default.Add, null) }, text = { Text("Log today") })
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        Column(Modifier.padding(pad)) {
            Row(Modifier.padding(horizontal = 16.dp)) {
                FilterChip(needsHelp, { needsHelp = !needsHelp }, label = { Text("Only people who are stuck") })
            }
            LoadContent(loader) { posts ->
                if (posts.isEmpty()) EmptyState(if (needsHelp) "Nobody is stuck right now." else "No learning logs yet. Start the streak.")
                else LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(posts, key = { it.id }) { p ->
                        LearningPostCard(
                            post = p,
                            isMine = p.authorId == me,
                            onEdit = { nav.push(Screen.CreateLearningPost(p.spaceId, p.groupId, title, editing = p)) },
                            onDelete = {
                                scope.launch { runCatching { api.deleteLearningPost(p.id) }; loader.reload() }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateLearningPostScreen(args: Screen.CreateLearningPost) {
    val api = LocalContainer.current.api
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val edit = args.editing
    val imagePicker = rememberImagePicker()

    var date by remember { mutableStateOf(edit?.date ?: today()) }
    var topic by remember { mutableStateOf(edit?.topic.orEmpty()) }
    var description by remember { mutableStateOf(edit?.description.orEmpty()) }
    var needsHelp by remember { mutableStateOf(edit?.needsHelp ?: false) }
    var imageUrl by remember { mutableStateOf(edit?.imageUrl.orEmpty()) }
    var newLink by remember { mutableStateOf("") }
    val links = remember { mutableStateListOf<String>().apply { edit?.links?.let { addAll(it) } } }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var uploading by remember { mutableStateOf(false) }

    fun handleImage(bytes: ByteArray?) {
        if (bytes == null) return
        scope.launch {
            uploading = true
            error = null
            runCatching {
                api.firestore.uploadFile("learning/${Clock.System.now().toEpochMilliseconds()}.jpg", bytes)
            }.onSuccess { url -> imageUrl = url }.onFailure { error = "Failed to upload image: ${it.message}" }
            uploading = false
        }
    }

    Scaffold(topBar = { BackBar(if (edit != null) "Edit log" else "Log for ${args.placeName}", nav::pop) },
        containerColor = MaterialTheme.colorScheme.background) { pad ->
        Column(
            Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp).widthIn(max = 720.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(date, { date = it }, label = { Text("Date") }, supportingText = { Text("yyyy-mm-dd") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            VoiceTextField(topic, { topic = it }, "What did you learn today?", Modifier.fillMaxWidth(), singleLine = true,
                supportingText = "e.g. Kotlin coroutines – structured concurrency")
            VoiceTextField(description, { description = it }, "Details", Modifier.fillMaxWidth(), minLines = 5)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(needsHelp, { needsHelp = it })
                Column {
                    Text("I'm stuck on something")
                    Text("Flags your log so others can spot that you need help.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("Reference Links (optional)", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newLink,
                    onValueChange = { newLink = it },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        if (newLink.isNotBlank()) {
                            links.add(newLink.trim())
                            newLink = ""
                        }
                    },
                    enabled = newLink.isNotBlank(),
                ) { Text("Add Link") }
            }
            if (links.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    links.forEachIndexed { idx, link ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(link, modifier = Modifier.weight(1f), maxLines = 1, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { links.removeAt(idx) }) { Icon(Icons.Default.Close, "Remove link") }
                        }
                    }
                }
            }
            Text("Attach an image (optional)", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { imagePicker.takePhoto(::handleImage) },
                    enabled = !uploading,
                    modifier = Modifier.weight(1f)
                ) { Text("Take Photo") }
                OutlinedButton(
                    onClick = { imagePicker.pickImage(::handleImage) },
                    enabled = !uploading,
                    modifier = Modifier.weight(1f)
                ) { Text("Select from folder") }
            }
            if (uploading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (imageUrl.isNotBlank()) {
                NetworkImage(
                    url = imageUrl,
                    contentDescription = "Attachment preview",
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                )
            }
            ErrorText(error)
            Button(
                enabled = !busy && !uploading && topic.isNotBlank() && description.isNotBlank(),
                onClick = {
                    if (!isIsoDate(date)) { error = "Use the date format yyyy-mm-dd."; return@Button }
                    scope.launch {
                        busy = true
                        val req = CreateLearningPostRequest(
                            spaceId = edit?.spaceId ?: args.spaceId,
                            groupId = edit?.groupId ?: args.groupId,
                            date = date,
                            topic = topic,
                            description = description,
                            needsHelp = needsHelp,
                            imageUrl = imageUrl.ifBlank { null },
                            links = links.toList(),
                        )
                        runCatching {
                            if (edit != null) api.updateLearningPost(edit.id, req)
                            else api.createLearningPost(req)
                        }.onSuccess { nav.pop() }.onFailure { error = it.message }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text(if (edit != null) "Save changes" else "Post log") }
        }
    }
}
