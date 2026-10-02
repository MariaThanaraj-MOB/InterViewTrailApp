package com.interviewtrail.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.interviewtrail.app.LocalContainer
import com.interviewtrail.app.data.Company
import com.interviewtrail.app.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CompaniesScreen() {
    val api = LocalContainer.current.api
    val nav = LocalNav.current
    var query by remember { mutableStateOf("") }
    var debounced by remember { mutableStateOf("") }
    LaunchedEffect(query) { delay(300); debounced = query }
    val loader = rememberLoader(debounced) { api.companies(debounced) to api.followedCompanies().toSet() }

    Column {
        TitleBar("Companies")
        OutlinedTextField(query, { query = it }, placeholder = { Text("Search companies") },
            leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        LoadContent(loader) { (companies, followed) ->
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (companies.isEmpty()) item {
                    EmptyState("No company matches \"$debounced\". Suggest it from your profile and we'll add it.")
                }
                items(companies, key = { it.id }) { c ->
                    RowItem(c.name, if (c.id in followed) "Following" else null, onClick = { nav.push(Screen.CompanyDetail(c)) })
                }
            }
        }
    }
}

@Composable
fun CompanyDetailScreen(company: Company) {
    val api = LocalContainer.current.api
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    var stack by remember { mutableStateOf<String?>(null) }
    var following by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(company.id) { following = runCatching { company.id in api.followedCompanies() }.getOrNull() }
    val stacks = rememberLoader(company.id) { api.companyStacks(company.id) }
    val posts = rememberLoader(company.id, stack) { api.companyPosts(company.id, stack) }

    Scaffold(
        topBar = {
            BackBar(company.name, nav::pop) {
                following?.let { f ->
                    // Followers get a push when someone posts about this company.
                    FilledTonalButton(onClick = {
                        scope.launch {
                            runCatching { if (f) api.unfollow(company.id) else api.follow(company.id) }
                                .onSuccess { following = !f }
                        }
                    }, modifier = Modifier.padding(end = 8.dp)) { Text(if (f) "Following" else "Follow") }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        Column(Modifier.padding(pad)) {
            (stacks.state as? Load.Ready)?.value?.takeIf { it.isNotEmpty() }?.let { list ->
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(stack == null, { stack = null }, label = { Text("All stacks") }) }
                    items(list) { s -> FilterChip(stack == s, { stack = s }, label = { Text(s) }) }
                }
            }
            LoadContent(posts) { list ->
                if (list.isEmpty()) EmptyState("No posts about ${company.name} yet. Follow to hear when the first one lands.")
                else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(list, key = { it.id }) { p -> PostCard(p) { nav.push(Screen.PostDetail(p.id)) } }
                }
            }
        }
    }
}
