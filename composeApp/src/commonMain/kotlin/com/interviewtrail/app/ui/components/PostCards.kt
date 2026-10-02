package com.interviewtrail.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.interviewtrail.app.data.LearningPost
import com.interviewtrail.app.data.Post
import com.interviewtrail.app.data.PostType

@Composable
fun PostCard(post: Post, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium, // Sharper corners for LinkedIn feel
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp, // Subtle drop shadow instead of tonal elevation
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), // Spacing between cards like a feed
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Mimic LinkedIn profile header
                NetworkAvatar(
                    url = null, 
                    name = post.authorName.ifBlank { "User" }, 
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(post.authorName.ifBlank { "A member" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(post.domain ?: "Software Engineer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) // Dynamic subtitle
                    val detail = when (post.type) {
                        PostType.INTERVIEW_EXPERIENCE -> post.interview?.interviewDate
                        PostType.WORK_STATUS -> ""
                        PostType.SEMINAR_WORKSHOP -> post.seminar?.eventDate
                    }
                    if (detail != null) Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            val headline = when (post.type) {
                PostType.INTERVIEW_EXPERIENCE -> "Interview Experience at ${post.company ?: "Company"}"
                PostType.WORK_STATUS -> "Started a new position at ${post.company}"
                PostType.SEMINAR_WORKSHOP -> post.seminar?.eventName ?: "Event"
            }
            Text(headline, style = MaterialTheme.typography.bodyLarge)
            
            if (post.type == PostType.INTERVIEW_EXPERIENCE) {
                post.interview?.let {
                    val info = "${it.rounds.size} round${if (it.rounds.size == 1) "" else "s"}, ${it.rounds.sumOf { r -> r.questions.size }} questions"
                    Text(info, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            
            if (post.imageUrls.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                NetworkImage(
                    url = post.imageUrls.first(),
                    contentDescription = "Post image preview",
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.small), // Taller image like LinkedIn feed
                )
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StackTag(post.techStack)
                post.domain?.takeIf { it.isNotBlank() }?.let { StackTag(it) }
            }
        }
    }
}

@Composable
fun StackTag(text: String) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
fun LearningPostCard(
    post: LearningPost,
    isMine: Boolean,
    onEdit: (() -> Unit)? = null,
    onDelete: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NetworkAvatar(
                    url = null, 
                    name = post.authorName.ifBlank { "Member" }, 
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(post.authorName.ifBlank { "Member" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(post.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(post.topic, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(post.description, style = MaterialTheme.typography.bodyMedium)
            if (!post.imageUrl.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                NetworkImage(
                    url = post.imageUrl,
                    contentDescription = "Learning post attachment",
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.small),
                )
            }
            if (post.needsHelp) {
                Spacer(Modifier.height(10.dp))
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.small) {
                    Text("Stuck on this – looking for help", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            if (post.links.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("References", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                post.links.forEach { link ->
                    Text(link, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (isMine) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    onEdit?.let { TextButton(onClick = it) { Text("Edit") } }
                    TextButton(onClick = onDelete) { Text("Delete") }
                }
            }
        }
    }
}
