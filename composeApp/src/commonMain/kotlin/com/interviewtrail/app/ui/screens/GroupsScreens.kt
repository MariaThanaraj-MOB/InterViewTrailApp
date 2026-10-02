package com.interviewtrail.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.interviewtrail.app.LocalContainer
import com.interviewtrail.app.data.*
import com.interviewtrail.app.platform.rememberShareSheet
import com.interviewtrail.app.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun GroupsScreen() {
    val nav = LocalNav.current
    Column {
        TitleBar("Groups")
        GroupList(GroupKind.PRIVATE, "Invite-only circles – your batchmates, your team – to share experiences privately.") {
            nav.push(Screen.GroupDetail(it))
        }
    }
}

/** Reused by private Groups and Learning Groups. */
@Composable
fun GroupList(kind: GroupKind, intro: String, open: (Group) -> Unit) {
    val api = LocalContainer.current.api
    val loader = rememberLoader(kind) { api.groups(kind) }
    var creating by remember { mutableStateOf(false) }
    var joining by remember { mutableStateOf(false) }

    LoadContent(loader) { groups ->
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text(intro, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { creating = true }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("New group") }
                    OutlinedButton(onClick = { joining = true }) { Text("Join with invite link") }
                }
                Spacer(Modifier.height(8.dp))
            }
            if (groups.isEmpty()) item { EmptyState("You're not in any group yet. Create one and invite people by email.") }
            items(groups, key = { it.id }) { g ->
                RowItem(g.name, "${g.memberIds.size} member${if (g.memberIds.size == 1) "" else "s"}", onClick = { open(g) })
            }
        }
    }
    if (creating) CreateGroupDialog(kind, onDone = { creating = false; loader.reload() })
    if (joining) JoinGroupDialog(onDone = { joining = false; loader.reload() })
}

@Composable
private fun CreateGroupDialog(kind: GroupKind, onDone: () -> Unit) {
    val api = LocalContainer.current.api
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(if (kind == GroupKind.LEARNING) "New learning group" else "New group") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VoiceTextField(name, { name = it }, "Group name", singleLine = true)
                VoiceTextField(description, { description = it }, "What's it for? (optional)", minLines = 2)
                ErrorText(error)
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                scope.launch {
                    runCatching { api.createGroup(CreateGroupRequest(kind, name, description)) }
                        .onSuccess { onDone() }.onFailure { error = it.message }
                }
            }) { Text("Create group") }
        },
        dismissButton = { TextButton(onClick = onDone) { Text("Cancel") } },
    )
}

@Composable
private fun JoinGroupDialog(onDone: () -> Unit) {
    val api = LocalContainer.current.api
    val scope = rememberCoroutineScope()
    var link by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text("Join with invite link") },
        text = {
            Column {
                Text("Paste the link you received. It only works for the email it was sent to.")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(link, { link = it.trim() }, label = { Text("Invite link") }, singleLine = true)
                ErrorText(error)
            }
        },
        confirmButton = {
            TextButton(enabled = link.isNotBlank(), onClick = {
                scope.launch {
                    runCatching { api.acceptInvite(link.substringAfterLast('/')) }
                        .onSuccess { onDone() }.onFailure { error = it.message }
                }
            }) { Text("Join") }
        },
        dismissButton = { TextButton(onClick = onDone) { Text("Cancel") } },
    )
}

/** Invite by email → server returns a link → native share sheet (WhatsApp, Mail…). */
@Composable
fun InviteDialog(group: Group, onDone: () -> Unit) {
    val api = LocalContainer.current.api
    val share = rememberShareSheet()
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text("Invite to ${group.name}") },
        text = {
            Column {
                Text("The link will only work for this email address.")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(email, { email = it.trim() }, label = { Text("Their email") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                ErrorText(error)
            }
        },
        confirmButton = {
            TextButton(enabled = email.contains("@"), onClick = {
                scope.launch {
                    runCatching { api.invite(group.id, email) }
                        .onSuccess {
                            share("Join my group \"${group.name}\" on InterviewTrail (sign in with $email): ${it.link}")
                            onDone()
                        }
                        .onFailure { error = it.message }
                }
            }) { Text("Create & share link") }
        },
        dismissButton = { TextButton(onClick = onDone) { Text("Cancel") } },
    )
}

@Composable
fun GroupDetailScreen(group: Group) {
    val api = LocalContainer.current.api
    val nav = LocalNav.current
    val me = LocalMyUid.current
    val scope = rememberCoroutineScope()
    val loader = rememberLoader(group.id) { api.groupPosts(group.id) }
    var inviting by remember { mutableStateOf(false) }
    val isOwner = group.ownerId == me

    Scaffold(
        topBar = {
            BackBar(group.name, nav::pop) {
                if (isOwner) IconButton(onClick = { inviting = true }) { Icon(Icons.Default.PersonAdd, "Invite") }
                else TextButton(onClick = { scope.launch { runCatching { api.leaveGroup(group.id) }; nav.pop() } }) { Text("Leave") }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { nav.push(Screen.CreatePost(groupId = group.id)) },
                icon = { Icon(Icons.Default.Add, null) }, text = { Text("Post here") })
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        Box(Modifier.padding(pad)) {
            LoadContent(loader) { posts ->
                if (posts.isEmpty()) EmptyState("Nothing shared here yet. Share one of your community posts, or post directly.")
                else LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(posts, key = { it.id }) { p -> PostCard(p) { nav.push(Screen.PostDetail(p.id)) } }
                }
            }
        }
    }
    if (inviting) InviteDialog(group) { inviting = false }
}
