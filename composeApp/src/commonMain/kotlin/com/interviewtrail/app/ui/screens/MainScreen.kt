package com.interviewtrail.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interviewtrail.app.LocalContainer
import com.interviewtrail.app.data.InterviewReminder
import com.interviewtrail.app.ui.components.Load
import com.interviewtrail.app.ui.components.NetworkAvatar
import com.interviewtrail.app.ui.components.rememberLoader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(myUid: String) {
    val c = LocalContainer.current
    val nav = remember { Navigator() }
    val api = c.api
    val meLoader = rememberLoader { api.me() }
    val me = (meLoader.state as? Load.Ready)?.value
    var showLogoutDialog by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalNav provides nav, LocalMyUid provides myUid) {
        Scaffold(
            topBar = {
                // Top App Bar with Official App Logo and User Info
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(end = 4.dp)
                        ) {
                            com.interviewtrail.app.ui.components.AppHeaderLogo(
                                size = 36.dp,
                                onClick = { nav.popToRoot() }
                            )
                            Spacer(Modifier.weight(1f))
                            if (me != null) {
                                Surface(
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.clickable { nav.push(Screen.EditProfile) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                "${me.firstName} ${me.lastName}".trim().ifBlank { "Me" },
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                me.designationId ?: me.currentCompany ?: me.domain ?: "Member",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        NetworkAvatar(
                                            url = me.photoUrl,
                                            name = "${me.firstName} ${me.lastName}".trim().ifBlank { "Me" },
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                }
                                IconButton(onClick = { showLogoutDialog = true }, modifier = Modifier.padding(start = 4.dp)) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Sign out",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                ) {
                    Tab.entries.filter { it != Tab.PROFILE }.forEach { t ->
                        val selected = nav.tab == t
                        NavigationBarItem(
                            selected = selected,
                            onClick = { nav.select(t) },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (val s = nav.top) {
                    null -> when (nav.tab) {
                        Tab.COMMUNITIES -> CommunitiesScreen()
                        Tab.GROUPS -> GroupsScreen()
                        Tab.COMPANIES -> CompaniesScreen()
                        Tab.LEARNING -> LearningScreen()
                        Tab.PROFILE -> ProfileScreen()
                    }
                    is Screen.CommunityDetail -> CommunityDetailScreen(s.community)
                    is Screen.PostDetail -> PostDetailScreen(s.postId)
                    is Screen.CreatePost -> CreatePostScreen(s)
                    is Screen.GroupDetail -> GroupDetailScreen(s.group)
                    is Screen.CompanyDetail -> CompanyDetailScreen(s.company)
                    is Screen.LearningSpaceDetail -> LearningSpaceScreen(s.space)
                    is Screen.LearningGroupDetail -> LearningGroupScreen(s.group)
                    is Screen.CreateLearningPost -> CreateLearningPostScreen(s)
                    Screen.EditProfile -> EditProfileScreen()
                    Screen.Interviews -> InterviewsScreen()
                    Screen.Suggestions -> SuggestionsScreen()
                }
            }
        }
        PostInterviewPrompt()
        
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Sign out") },
                text = { Text("Are you sure you want to sign out of InterviewTrail?") },
                confirmButton = {
                    TextButton(onClick = { 
                        showLogoutDialog = false
                        c.auth.signOut() 
                    }) { Text("Sign out", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

/** After a logged interview's time passes: a single yes/no question. "Yes" opens the post form. */
@Composable
private fun PostInterviewPrompt() {
    val api = LocalContainer.current.api
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    var queue by remember { mutableStateOf<List<InterviewReminder>>(emptyList()) }

    LaunchedEffect(Unit) { queue = runCatching { api.pendingPrompts() }.getOrDefault(emptyList()) }

    val current = queue.firstOrNull() ?: return
    fun answer(happened: Boolean) = scope.launch {
        runCatching { api.setOutcome(current.id, happened) }
        queue = queue.drop(1)
        if (happened) nav.openIn(Tab.COMMUNITIES, Screen.CreatePost(company = current.company))
    }

    AlertDialog(
        onDismissRequest = { queue = queue.drop(1) },
        title = { Text("Did your ${current.company} interview happen?") },
        text = { Text("If it did, sharing the rounds and questions helps the next person prepare.") },
        confirmButton = { TextButton(onClick = { answer(true) }) { Text("Yes, share it") } },
        dismissButton = { TextButton(onClick = { answer(false) }) { Text("No") } },
    )
}
