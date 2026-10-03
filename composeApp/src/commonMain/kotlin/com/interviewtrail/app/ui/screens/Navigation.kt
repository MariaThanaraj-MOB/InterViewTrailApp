package com.interviewtrail.app.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.interviewtrail.app.data.*

enum class Tab(val label: String, val icon: ImageVector) {
    COMMUNITIES("Communities", Icons.Default.Forum),
    GROUPS("Groups", Icons.Default.Groups),
    COMPANIES("Companies", Icons.Default.Business),
    LEARNING("Learning", Icons.Default.School),
    PROFILE("Profile", Icons.Default.Person),
}

sealed interface Screen {
    data class CommunityDetail(val community: Community) : Screen
    data class PostDetail(val postId: String) : Screen
    data class CreatePost(
        val communityId: String? = null, val techStack: String? = null,
        val groupId: String? = null, val company: String? = null,
        val editing: Post? = null,
    ) : Screen
    data class GroupDetail(val group: Group) : Screen
    data class CompanyDetail(val company: Company) : Screen
    data class LearningSpaceDetail(val space: Community) : Screen
    data class LearningGroupDetail(val group: Group) : Screen
    data class CreateLearningPost(val spaceId: String?, val groupId: String?, val placeName: String, val editing: LearningPost? = null) : Screen
    data object EditProfile : Screen
    data object Interviews : Screen
    data object Suggestions : Screen
}

/** One back stack per bottom tab, so switching tabs keeps your place. */
class Navigator {
    var tab by mutableStateOf(Tab.COMMUNITIES)
    private val stacks = mutableStateMapOf<Tab, List<Screen>>()

    val top: Screen? get() = stacks[tab]?.lastOrNull()

    fun push(s: Screen) { stacks[tab] = stacks[tab].orEmpty() + s }
    fun pop() { stacks[tab] = stacks[tab].orEmpty().dropLast(1) }
    fun popToRoot() { stacks[tab] = emptyList() }
    fun select(t: Tab) { if (t == tab) stacks[t] = emptyList() else tab = t }
    fun openIn(t: Tab, s: Screen) { tab = t; push(s) }
}

val LocalNav = staticCompositionLocalOf<Navigator> { error("Navigator not provided") }
val LocalMyUid = staticCompositionLocalOf { "" }
