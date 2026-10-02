package com.interviewtrail.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.interviewtrail.app.LocalContainer
import com.interviewtrail.app.data.*
import com.interviewtrail.app.ui.components.*
import kotlinx.coroutines.launch
import com.interviewtrail.app.platform.rememberImagePicker
import kotlinx.datetime.*

@Composable
fun ProfileScreen() {
    val c = LocalContainer.current
    val nav = LocalNav.current
    val loader = rememberLoader { c.api.me() to c.api.myPosts() }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LoadContent(loader) { (me, posts) ->
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    // Profile Header Card
                    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                NetworkAvatar(me.photoUrl, "${me.firstName} ${me.lastName}".ifBlank { me.email }, Modifier.size(88.dp))
                                Column {
                                    Text("${me.firstName} ${me.lastName}".ifBlank { me.email }, style = MaterialTheme.typography.titleLarge)
                                    val companyLine = listOfNotNull(me.currentCompany, me.experienceLevel?.label, me.domain).joinToString(" · ")
                                    if (companyLine.isNotBlank()) Text(companyLine, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                                    val loc = listOfNotNull(me.location.city, me.location.state, me.location.country).filter { it.isNotBlank() }.joinToString(", ")
                                    if (loc.isNotBlank()) Text(loc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            if (me.techStacks.isNotEmpty()) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    me.techStacks.forEach { StackTag(it) }
                                }
                                Spacer(Modifier.height(16.dp))
                            }
                            Button(onClick = { nav.push(Screen.EditProfile) }, modifier = Modifier.fillMaxWidth().height(44.dp)) {
                                Text("Edit profile")
                            }
                        }
                    }
                }
                
                item { Spacer(Modifier.height(4.dp)) }
                
                item { 
                    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
                        Column {
                            RowItem("My interviews", "Log upcoming interviews and get a reminder", onClick = { nav.push(Screen.Interviews) })
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            RowItem("Suggest something", "Missing a tech stack or company? Tell us", onClick = { nav.push(Screen.Suggestions) }) 
                        }
                    }
                }
                
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Your posts", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
                }
                if (posts.isEmpty()) item { EmptyState("You haven't shared anything yet.") }
                items(posts, key = { it.id }) { p -> 
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        PostCard(p) { nav.push(Screen.PostDetail(p.id)) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditProfileScreen() {
    val api = LocalContainer.current.api
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val loader = rememberLoader { api.me() to api.communities() }
    val imagePicker = rememberImagePicker()

    Scaffold(topBar = { BackBar("Edit profile", nav::pop) }, containerColor = MaterialTheme.colorScheme.background) { pad ->
        Box(Modifier.padding(pad)) {
            LoadContent(loader) { (me, communities) ->
                var first by remember { mutableStateOf(me.firstName) }
                var last by remember { mutableStateOf(me.lastName) }
                var company by remember { mutableStateOf(me.currentCompany.orEmpty()) }
                var level by remember { mutableStateOf(me.experienceLevel) }
                var domain by remember { mutableStateOf(me.domain.orEmpty()) }
                var state by remember { mutableStateOf(me.location.state.orEmpty()) }
                var city by remember { mutableStateOf(me.location.city.orEmpty()) }
                var photoUrl by remember { mutableStateOf(me.photoUrl.orEmpty()) }
                val stacks = remember { me.techStacks.toMutableStateList() }
                var error by remember { mutableStateOf<String?>(null) }
                var uploading by remember { mutableStateOf(false) }

                fun handleImage(bytes: ByteArray?) {
                    if (bytes == null) return
                    scope.launch {
                        uploading = true
                        error = null
                        runCatching {
                            api.firestore.uploadFile("avatars/${me.id}_${Clock.System.now().toEpochMilliseconds()}.jpg", bytes)
                        }.onSuccess { url -> photoUrl = url }.onFailure { error = "Failed to upload image: ${it.message}" }
                        uploading = false
                    }
                }

                Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp).widthIn(max = 720.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        NetworkAvatar(photoUrl.ifBlank { null }, "$first $last".ifBlank { me.email }, Modifier.size(64.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Profile Photo", style = MaterialTheme.typography.labelMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                                OutlinedButton(
                                    onClick = { imagePicker.takePhoto(::handleImage) },
                                    enabled = !uploading
                                ) { Text("Camera") }
                                OutlinedButton(
                                    onClick = { imagePicker.pickImage(::handleImage) },
                                    enabled = !uploading
                                ) { Text("Gallery") }
                            }
                            if (uploading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(first, { first = it }, label = { Text("First name") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(last, { last = it }, label = { Text("Last name") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Column(Modifier.fillMaxWidth()) {
                        Text("Current Company", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        MasterDropdownWithOthers(
                            label = "Current Company",
                            selectedValue = company,
                            options = ApiClient.MasterData.defaultCompanies.map { it.name },
                            onValueChange = { company = it },
                            placeholder = "Select Company",
                        )
                    }
                    Picker("Experience level", level?.label, ExperienceLevel.entries.map { it.label }) { level = ExperienceLevel.entries.getOrNull(it) ?: level }
                    Column(Modifier.fillMaxWidth()) {
                        Text("Domain", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        MasterDropdownWithOthers(
                            label = "Domain",
                            selectedValue = domain,
                            options = ApiClient.MasterData.domains,
                            onValueChange = { domain = it },
                            placeholder = "Select Domain",
                        )
                    }
                    Text("Technology stack", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        communities.forEach { cm ->
                            val on = cm.techStack in stacks
                            FilterChip(on, { if (on) stacks.remove(cm.techStack) else stacks.add(cm.techStack) }, label = { Text(cm.name) })
                        }
                    }
                    Text("Location (India)", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("State", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(4.dp))
                            MasterDropdownWithOthers(
                                label = "State",
                                selectedValue = state,
                                options = ApiClient.MasterData.states,
                                onValueChange = { state = it },
                                placeholder = "Select State",
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text("City", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(4.dp))
                            MasterDropdownWithOthers(
                                label = "City",
                                selectedValue = city,
                                options = ApiClient.MasterData.cities,
                                onValueChange = { city = it },
                                placeholder = "Select City",
                            )
                        }
                    }
                    ErrorText(error)
                    Button(onClick = {
                        scope.launch {
                            runCatching {
                                api.updateMe(UpdateProfileRequest(first, last, company.ifBlank { null }, level, stacks.toList(),
                                    domain.ifBlank { null }, photoUrl.ifBlank { null }, Location(state = state.ifBlank { null }, city = city.ifBlank { null })))
                            }.onSuccess { nav.pop() }.onFailure { error = it.message }
                        }
                    }, enabled = first.isNotBlank(), modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Save profile") }
                }
            }
        }
    }
}

@Composable
fun InterviewsScreen() {
    val api = LocalContainer.current.api
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val loader = rememberLoader { api.interviews() }
    var company by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(today()) }
    var time by remember { mutableStateOf("10:00") }
    var remind by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val tz = TimeZone.currentSystemDefault()

    Scaffold(topBar = { BackBar("My interviews", nav::pop) }, containerColor = MaterialTheme.colorScheme.background) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Log an upcoming interview", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(company, { company = it }, label = { Text("Company") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(venue, { venue = it }, label = { Text("Venue or meeting link") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(date, { date = it }, label = { Text("Date") }, supportingText = { Text("yyyy-mm-dd") },
                                singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(time, { time = it }, label = { Text("Time") }, supportingText = { Text("24h, hh:mm") },
                                singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Remind me 12 hours before", Modifier.weight(1f))
                            Switch(remind, { remind = it })
                        }
                        ErrorText(error)
                        Button(enabled = company.isNotBlank(), onClick = {
                            error = null
                            val at = runCatching { LocalDateTime.parse("${date}T$time").toInstant(tz).toEpochMilliseconds() }.getOrNull()
                            if (at == null) { error = "Check the date (yyyy-mm-dd) and time (hh:mm)."; return@Button }
                            scope.launch {
                                runCatching { api.logInterview(CreateReminderRequest(company, venue, at, remind)) }
                                    .onSuccess { company = ""; venue = ""; loader.reload() }
                                    .onFailure { error = it.message }
                            }
                        }) { Text("Save interview") }
                    }
                }
            }
            item { LoadContent(loader) { } }
            val list = (loader.state as? Load.Ready)?.value.orEmpty()
            items(list, key = { it.id }) { r ->
                val dt = Instant.fromEpochMilliseconds(r.scheduledAt).toLocalDateTime(tz)
                val past = r.outcome != ReminderOutcome.PENDING || r.scheduledAt < Clock.System.now().toEpochMilliseconds()
                RowItem(
                    r.company,
                    "${dt.date} at ${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}" +
                        (if (r.venue.isNotBlank()) " · ${r.venue}" else ""),
                    onClick = {},
                ) {
                    if (!past) Switch(r.remindEnabled, { on ->
                        scope.launch { runCatching { api.toggleReminder(r.id, on) }; loader.reload() }
                    })
                    TextButton(onClick = { scope.launch { runCatching { api.deleteInterview(r.id) }; loader.reload() } }) { Text("Remove") }
                }
            }
        }
    }
}

@Composable
fun SuggestionsScreen() {
    val api = LocalContainer.current.api
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val loader = rememberLoader { api.suggestions() }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var sent by remember { mutableStateOf(false) }

    Scaffold(topBar = { BackBar("Suggest something", nav::pop) }, containerColor = MaterialTheme.colorScheme.background) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(title, { title = it; sent = false }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        supportingText = { Text("e.g. Add a Golang community") })
                    OutlinedTextField(description, { description = it; sent = false }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    ErrorText(error)
                    if (sent) Text("Sent. You can track its status below.", color = MaterialTheme.colorScheme.primary)
                    Button(enabled = title.isNotBlank() && description.isNotBlank(), onClick = {
                        scope.launch {
                            runCatching { api.suggest(CreateSuggestionRequest(title, description)) }
                                .onSuccess { title = ""; description = ""; sent = true; loader.reload() }
                                .onFailure { error = it.message }
                        }
                    }) { Text("Send suggestion") }
                    Spacer(Modifier.height(8.dp))
                    Text("Your suggestions", style = MaterialTheme.typography.titleMedium)
                }
            }
            val list = (loader.state as? Load.Ready)?.value.orEmpty()
            items(list, key = { it.id }) { s ->
                RowItem(s.title, s.description, onClick = {}) {
                    StackTag(s.status.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() })
                }
            }
        }
    }
}
