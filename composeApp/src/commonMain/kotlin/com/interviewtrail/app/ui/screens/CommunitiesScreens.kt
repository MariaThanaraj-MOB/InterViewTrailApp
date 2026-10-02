package com.interviewtrail.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.interviewtrail.app.LocalContainer
import com.interviewtrail.app.data.Community
import com.interviewtrail.app.data.CommunityTab
import com.interviewtrail.app.ui.components.*
import kotlinx.coroutines.delay

@Composable
fun CommunitiesScreen() {
    val api = LocalContainer.current.api
    val nav = LocalNav.current
    val loader = rememberLoader { api.communities() }

    Column {
        TitleBar("Communities")
        LoadContent(loader) { list ->
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text("Pick your stack to see how interviews for it actually go.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp))
                }
                items(list, key = { it.id }) { c ->
                    RowItem(c.name, c.description.ifBlank { null }, onClick = { nav.push(Screen.CommunityDetail(c)) })
                }
            }
        }
    }
}

@Composable
fun CommunityDetailScreen(community: Community) {
    val api = LocalContainer.current.api
    val nav = LocalNav.current
    var tab by remember { mutableStateOf(CommunityTab.INTERVIEW) }
    var companyQuery by remember { mutableStateOf("") }
    var debounced by remember { mutableStateOf("") }
    LaunchedEffect(companyQuery) { delay(350); debounced = companyQuery }

    val loader = rememberLoader(tab, debounced) {
        api.communityPosts(community.id, tab, debounced.takeIf { tab == CommunityTab.INTERVIEW })
    }

    Scaffold(
        topBar = { BackBar(community.name, nav::pop) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.push(Screen.CreatePost(communityId = community.id, techStack = community.techStack)) },
                icon = { Icon(Icons.Default.Add, null) }, text = { Text("Share experience") },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        Column(Modifier.padding(pad)) {
            TabRow(selectedTabIndex = tab.ordinal, containerColor = MaterialTheme.colorScheme.background) {
                Tab(tab == CommunityTab.INTERVIEW, { tab = CommunityTab.INTERVIEW }, text = { Text("Interviews") })
                Tab(tab == CommunityTab.WORKSHOP_SEMINAR, { tab = CommunityTab.WORKSHOP_SEMINAR }, text = { Text("Workshops & seminars") })
            }
            if (tab == CommunityTab.INTERVIEW) {
                OutlinedTextField(
                    companyQuery, { companyQuery = it },
                    placeholder = { Text("Filter by company") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            LoadContent(loader) { posts ->
                if (posts.isEmpty()) {
                    EmptyState(
                        if (tab == CommunityTab.INTERVIEW) "No interview experiences here yet. Yours could be the first one someone reads before their big day."
                        else "No workshop or seminar notes yet.",
                    )
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(posts, key = { it.id }) { p -> PostCard(p) { nav.push(Screen.PostDetail(p.id)) } }
                    }
                }
            }
        }
    }
}
