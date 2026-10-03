package com.interviewtrail.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.interviewtrail.app.LocalContainer
import com.interviewtrail.app.data.*
import com.interviewtrail.app.ui.components.*
import kotlinx.coroutines.launch

import com.interviewtrail.app.platform.rememberImagePicker
import kotlinx.datetime.Clock

// ---------------- Create / edit ----------------

private class QAState(q: String = "", a: String = "") {
    var question by mutableStateOf(q); var answer by mutableStateOf(a)
}
private class RoundState(name: String = "", qs: List<QAState> = listOf(QAState())) {
    var name by mutableStateOf(name)
    val questions: SnapshotStateList<QAState> = mutableStateListOf(*qs.toTypedArray())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(args: Screen.CreatePost) {
    val api = LocalContainer.current.api
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val edit = args.editing

    var type by remember { mutableStateOf(edit?.type ?: PostType.INTERVIEW_EXPERIENCE) }
    var community by remember { mutableStateOf<Community?>(null) }
    val needsCommunityPick = args.communityId == null && args.groupId == null && edit == null
    val communities = rememberLoader { if (needsCommunityPick) api.communities() else emptyList() }

    var company by remember { mutableStateOf(edit?.company ?: args.company.orEmpty()) }
    var domain by remember { mutableStateOf(edit?.domain.orEmpty()) }
    var interviewDate by remember { mutableStateOf(edit?.interview?.interviewDate ?: today()) }
    var notes by remember { mutableStateOf(edit?.interview?.notes.orEmpty()) }
    val rounds = remember {
        mutableStateListOf<RoundState>().apply {
            edit?.interview?.rounds?.forEach { r -> add(RoundState(r.roundName, r.questions.map { QAState(it.question, it.answer) })) }
            if (isEmpty()) add(RoundState("Technical Round 1"))
        }
    }

    val companiesLoader = rememberLoader { api.companies().map { it.name } }
    val companyMasterList = remember(companiesLoader.state) {
        val list = (companiesLoader.state as? Load.Ready)?.value.orEmpty().toMutableList()
        ApiClient.MasterData.defaultCompanies.map { it.name }.forEach { if (it !in list) list.add(it) }
        list
    }
    var eventName by remember { mutableStateOf(edit?.seminar?.eventName.orEmpty()) }
    var eventDate by remember { mutableStateOf(edit?.seminar?.eventDate ?: today()) }
    var topic by remember { mutableStateOf(edit?.seminar?.topic.orEmpty()) }
    var takeaways by remember { mutableStateOf(edit?.seminar?.takeaways.orEmpty()) }
    val imageUrls = remember { mutableStateListOf<String>().apply { edit?.imageUrls?.let { addAll(it) } } }
    
    var uploading by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val imagePicker = rememberImagePicker()
    
    fun handleImage(bytes: ByteArray?) {
        if (bytes == null) return
        scope.launch {
            uploading = true
            error = null
            runCatching {
                api.firestore.uploadFile("posts/${Clock.System.now().toEpochMilliseconds()}.jpg", bytes)
            }.onSuccess { url -> imageUrls.add(url) }.onFailure { error = "Failed to upload image: ${it.message}" }
            uploading = false
        }
    }

    fun build(): CreatePostRequest? {
        val communityId = edit?.communityId ?: args.communityId ?: community?.id
        val stack = edit?.techStack ?: args.techStack ?: community?.techStack
        if (communityId == null && args.groupId == null) { error = "Choose a community for this post."; return null }
        if (stack == null) { error = "Choose a community for this post."; return null }
        val finalImageUrls = imageUrls.toList()
        return when (type) {
            PostType.INTERVIEW_EXPERIENCE -> {
                if (company.isBlank()) { error = "Add the company name."; return null }
                if (!isIsoDate(interviewDate)) { error = "Use the date format yyyy-mm-dd."; return null }
                val rs = rounds.map { r ->
                    InterviewRound(r.name.trim(), r.questions.filter { it.question.isNotBlank() }
                        .map { QuestionAnswer(it.question.trim(), it.answer.trim()) })
                }
                if (rs.any { it.roundName.isBlank() }) { error = "Give every round a name, e.g. \"Technical 1\"."; return null }
                CreatePostRequest(type, communityId, company, stack, domain.ifBlank { null },
                    interview = InterviewDetails(interviewDate, rs, notes.ifBlank { null }), imageUrls = finalImageUrls, groupId = args.groupId)
            }
            PostType.WORK_STATUS -> {
                if (company.isBlank()) { error = "Add the company name."; return null }
                CreatePostRequest(type, communityId, company, stack, domain.ifBlank { null }, imageUrls = finalImageUrls, groupId = args.groupId)
            }
            PostType.SEMINAR_WORKSHOP -> {
                if (eventName.isBlank() || topic.isBlank()) { error = "Add the event name and topic."; return null }
                if (!isIsoDate(eventDate)) { error = "Use the date format yyyy-mm-dd."; return null }
                CreatePostRequest(type, communityId, null, stack, domain.ifBlank { null },
                    seminar = SeminarDetails(eventName, eventDate, topic, takeaways), imageUrls = finalImageUrls, groupId = args.groupId)
            }
        }
    }

    Scaffold(topBar = { BackBar(if (edit != null) "Edit post" else "Share experience", nav::pop) },
        containerColor = MaterialTheme.colorScheme.background) { pad ->
        Column(
            Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp).widthIn(max = 720.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (edit == null) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    PostType.entries.forEachIndexed { i, t ->
                        SegmentedButton(type == t, { type = t }, SegmentedButtonDefaults.itemShape(i, PostType.entries.size)) {
                            Text(when (t) { PostType.INTERVIEW_EXPERIENCE -> "Interview"; PostType.WORK_STATUS -> "Work status"; PostType.SEMINAR_WORKSHOP -> "Event" }, maxLines = 1)
                        }
                    }
                }
            }

            if (needsCommunityPick) {
                Surface(shape = MaterialTheme.shapes.medium, shadowElevation = 1.dp, color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(16.dp).fillMaxWidth()) {
                        Text("Community", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        LoadContent(communities) { list ->
                            Picker("Community (tech stack)", community?.name, list.map { it.name }) { community = list[it] }
                        }
                    }
                }
            }

            when (type) {
                PostType.INTERVIEW_EXPERIENCE -> {
                    Surface(shape = MaterialTheme.shapes.medium, shadowElevation = 1.dp, color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.padding(16.dp).fillMaxWidth()) {
                            Text("Interview details", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(16.dp))
                            Text("Company Name", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(4.dp))
                            MasterDropdownWithOthers(
                                label = "Company Name",
                                selectedValue = company,
                                options = companyMasterList,
                                onValueChange = { company = it },
                                placeholder = "Select Company Name",
                                onAddNewOption = { newComp ->
                                    scope.launch { runCatching { api.companies(newComp) } }
                                }
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text("Interview date", style = MaterialTheme.typography.labelMedium)
                                    Spacer(Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = interviewDate,
                                        onValueChange = { interviewDate = it },
                                        placeholder = { Text("yyyy-mm-dd") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("Domain Name", style = MaterialTheme.typography.labelMedium)
                                    Spacer(Modifier.height(4.dp))
                                    MasterDropdownWithOthers(
                                        label = "Domain Name",
                                        selectedValue = domain,
                                        options = ApiClient.MasterData.domains,
                                        onValueChange = { domain = it },
                                        placeholder = "Select Domain Name",
                                    )
                                }
                            }
                        }
                    }
                    
                    rounds.forEachIndexed { ri, round ->
                        RoundEditor(round, canRemove = rounds.size > 1, onRemove = { rounds.removeAt(ri) })
                    }
                    OutlinedButton(onClick = { rounds.add(RoundState("Technical Round ${rounds.size + 1}")) }, modifier = Modifier.fillMaxWidth()) { Text("+ Add round") }
                    
                    Surface(shape = MaterialTheme.shapes.medium, shadowElevation = 1.dp, color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.padding(16.dp).fillMaxWidth()) {
                            Text("Additional notes", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            VoiceTextField(notes, { notes = it }, "Notes (optional)", Modifier.fillMaxWidth(), minLines = 3,
                                supportingText = "Tips, what you'd prepare differently, result")
                        }
                    }
                }
                PostType.WORK_STATUS -> {
                    Surface(shape = MaterialTheme.shapes.medium, shadowElevation = 1.dp, color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.padding(16.dp).fillMaxWidth()) {
                            Text("Company Name", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(4.dp))
                            MasterDropdownWithOthers(
                                label = "Company Name",
                                selectedValue = company,
                                options = companyMasterList,
                                onValueChange = { company = it },
                                placeholder = "Select Company Name",
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("Domain Name", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(4.dp))
                            MasterDropdownWithOthers(
                                label = "Domain Name",
                                selectedValue = domain,
                                options = ApiClient.MasterData.domains,
                                onValueChange = { domain = it },
                                placeholder = "Select Domain Name",
                            )
                        }
                    }
                }
                PostType.SEMINAR_WORKSHOP -> {
                    Surface(shape = MaterialTheme.shapes.medium, shadowElevation = 1.dp, color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.padding(16.dp).fillMaxWidth()) {
                            VoiceTextField(eventName, { eventName = it }, "Event name", Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(eventDate, { eventDate = it }, label = { Text("Event date") },
                                supportingText = { Text("yyyy-mm-dd") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(12.dp))
                            VoiceTextField(topic, { topic = it }, "Topic", Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(Modifier.height(12.dp))
                            VoiceTextField(takeaways, { takeaways = it }, "Key takeaways", Modifier.fillMaxWidth(), minLines = 4)
                        }
                    }
                }
            }

            Surface(shape = MaterialTheme.shapes.medium, shadowElevation = 1.dp, color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("Attach images (optional)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
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
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    if (imageUrls.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 16.dp)) {
                            imageUrls.forEachIndexed { idx, url ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    NetworkImage(url = url, contentDescription = "Preview", modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.small))
                                    Spacer(Modifier.width(12.dp))
                                    Text("Image ${idx + 1}", modifier = Modifier.weight(1f), maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                                    IconButton(onClick = { imageUrls.removeAt(idx) }) { Icon(Icons.Default.Close, "Remove image") }
                                }
                            }
                        }
                    }
                }
            }
            ErrorText(error)
            Button(
                onClick = {
                    error = null
                    val req = build() ?: return@Button
                    scope.launch {
                        busy = true
                        runCatching { if (edit != null) api.updatePost(edit.id, req) else api.createPost(req) }
                            .onSuccess { nav.pop(); if (edit == null) nav.push(Screen.PostDetail(it.id)) }
                            .onFailure { error = it.message }
                        busy = false
                    }
                },
                enabled = !busy && !uploading, modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text(if (edit != null) "Save changes" else "Publish") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RoundEditor(round: RoundState, canRemove: Boolean, onRemove: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.medium, shadowElevation = 1.dp, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Round Name", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(8.dp))
                MasterDropdownWithOthers(
                    label = "Round Name",
                    selectedValue = round.name,
                    options = ApiClient.MasterData.interviewRoundTypes,
                    onValueChange = { round.name = it },
                    placeholder = "Select Round Name",
                    modifier = Modifier.weight(1f),
                )
                if (canRemove) {
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onRemove) { Icon(Icons.Default.Close, "Remove round", tint = MaterialTheme.colorScheme.error) }
                }
            }
            round.questions.forEachIndexed { qi, qa ->
                VoiceTextField(qa.question, { qa.question = it }, "Question ${qi + 1}", Modifier.fillMaxWidth())
                VoiceTextField(qa.answer, { qa.answer = it }, "Your answer (optional)", Modifier.fillMaxWidth(), minLines = 2)
            }
            TextButton(onClick = { round.questions.add(QAState()) }) { Text("+ Add question") }
        }
    }
}

@Composable
fun Picker(label: String, selected: String?, options: List<String>, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected ?: label, modifier = Modifier.weight(1f))
        }
        DropdownMenu(open, { open = false }) {
            options.forEachIndexed { i, o -> DropdownMenuItem(text = { Text(o) }, onClick = { onPick(i); open = false }) }
        }
    }
}

// ---------------- Detail ----------------

@Composable
fun PostDetailScreen(postId: String) {
    val api = LocalContainer.current.api
    val nav = LocalNav.current
    val me = LocalMyUid.current
    val scope = rememberCoroutineScope()
    val loader = rememberLoader(postId) { api.post(postId) }
    var confirmDelete by remember { mutableStateOf(false) }
    var sharing by remember { mutableStateOf(false) }

    val post = (loader.state as? Load.Ready)?.value
    val mine = post?.authorId == me

    Scaffold(
        topBar = {
            BackBar(post?.company ?: post?.seminar?.eventName ?: "Post", nav::pop) {
                if (mine && post != null) {
                    // Cross-posting: only the original poster can share into their own groups.
                    IconButton(onClick = { sharing = true }) { Icon(Icons.Default.Share, "Share to my groups") }
                    IconButton(onClick = { nav.push(Screen.CreatePost(editing = post)) }) { Icon(Icons.Default.Edit, "Edit") }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Delete") }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        Box(Modifier.padding(pad)) {
            LoadContent(loader) { p -> PostBody(p) }
        }
    }

    if (confirmDelete && post != null) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete this post?") },
        text = { Text("It will be removed from the community and any groups it was shared to.") },
        confirmButton = { TextButton(onClick = { scope.launch { runCatching { api.deletePost(post.id) }; nav.pop() } }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } },
    )
    if (sharing && post != null) ShareToGroupsDialog(post, onDone = { sharing = false; loader.reload() })
}

@Composable
private fun PostBody(p: Post) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp).widthIn(max = 720.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StackTag(p.techStack); p.domain?.let { StackTag(it) }
        }
        Text("Shared by ${p.authorName.ifBlank { "a member" }}", style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        when (p.type) {
            PostType.INTERVIEW_EXPERIENCE -> p.interview?.let { iv ->
                Text("Interviewed on ${iv.interviewDate}", style = MaterialTheme.typography.bodyMedium)
                
                val allQuestions = iv.rounds.flatMap { r -> 
                    r.questions.map { q -> r.roundName to q }
                }
                
                if (allQuestions.isNotEmpty()) {
                    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { allQuestions.size })
                    val scope = rememberCoroutineScope()
                    
                    Text("Questions (${pagerState.currentPage + 1}/${allQuestions.size})", style = MaterialTheme.typography.titleMedium)
                    
                    androidx.compose.foundation.pager.HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        val (roundName, qa) = allQuestions[page]
                        Surface(
                            shape = MaterialTheme.shapes.medium, 
                            shadowElevation = 2.dp, 
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = roundName, 
                                    style = MaterialTheme.typography.labelMedium, 
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Q: ${qa.question}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Text(
                                    text = if (qa.answer.isNotBlank()) "A: ${qa.answer}" else "A: Answer not mentioned.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (qa.answer.isNotBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                            enabled = pagerState.currentPage > 0
                        ) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous")
                            Spacer(Modifier.width(4.dp))
                            Text("Previous")
                        }
                        
                        TextButton(
                            onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                            enabled = pagerState.currentPage < allQuestions.size - 1
                        ) {
                            Text("Next")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next")
                        }
                    }
                }

                iv.notes?.let {
                    Spacer(Modifier.height(12.dp))
                    Text("Notes", style = MaterialTheme.typography.titleMedium)
                    Text(it, style = MaterialTheme.typography.bodyLarge)
                }
            }
            PostType.WORK_STATUS -> Text("Currently working at ${p.company} on ${p.techStack}.", style = MaterialTheme.typography.bodyLarge)
            PostType.SEMINAR_WORKSHOP -> p.seminar?.let { s ->
                Text("${s.topic} · ${s.eventDate}", style = MaterialTheme.typography.bodyMedium)
                Text("Takeaways", style = MaterialTheme.typography.titleMedium)
                Text(s.takeaways, style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (p.imageUrls.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Attached images", style = MaterialTheme.typography.titleMedium)
            p.imageUrls.forEach { url ->
                NetworkImage(
                    url = url,
                    contentDescription = "Attached image",
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                )
            }
        }
    }
}

@Composable
private fun ShareToGroupsDialog(post: Post, onDone: () -> Unit) {
    val api = LocalContainer.current.api
    val scope = rememberCoroutineScope()
    val groups = rememberLoader { api.groups(GroupKind.PRIVATE) }
    val picked = remember { mutableStateListOf<String>() }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDone,
        title = { Text("Share to your groups") },
        text = {
            LoadContent(groups) { list ->
                val available = list.filter { it.id !in post.sharedToGroupIds }
                Column {
                    if (available.isEmpty()) Text("You're not in any other private group yet.")
                    available.forEach { g ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(g.id in picked, { if (it) picked.add(g.id) else picked.remove(g.id) })
                            Text(g.name)
                        }
                    }
                    ErrorText(error)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = picked.isNotEmpty(), onClick = {
                scope.launch {
                    runCatching { api.sharePost(post.id, picked.toList()) }
                        .onSuccess { onDone() }.onFailure { error = it.message }
                }
            }) { Text("Share") }
        },
        dismissButton = { TextButton(onClick = onDone) { Text("Cancel") } },
    )
}
