package com.interviewtrail.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interviewtrail.app.data.LearningPost
import com.interviewtrail.app.data.Post
import com.interviewtrail.app.data.PostType

@Composable
fun PostCard(post: Post, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NetworkAvatar(
                    url = null, 
                    name = post.authorName.ifBlank { "User" }, 
                    modifier = Modifier.size(44.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        post.authorName.ifBlank { "A member" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        post.domain ?: "Software Engineer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val detail = when (post.type) {
                        PostType.INTERVIEW_EXPERIENCE -> post.interview?.interviewDate
                        PostType.WORK_STATUS -> ""
                        PostType.SEMINAR_WORKSHOP -> post.seminar?.eventDate
                    }
                    if (!detail.isNullOrBlank()) {
                        Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            val headline = when (post.type) {
                PostType.INTERVIEW_EXPERIENCE -> "Interview Experience at ${post.company ?: "Company"}"
                PostType.WORK_STATUS -> "Started a new position at ${post.company}"
                PostType.SEMINAR_WORKSHOP -> post.seminar?.eventName ?: "Event"
            }
            Text(
                headline,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (post.type == PostType.INTERVIEW_EXPERIENCE) {
                post.interview?.let {
                    val info = "${it.rounds.size} round${if (it.rounds.size == 1) "" else "s"}, ${it.rounds.sumOf { r -> r.questions.size }} questions"
                    Spacer(Modifier.height(2.dp))
                    Text(info, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            
            if (post.imageUrls.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                NetworkImage(
                    url = post.imageUrls.first(),
                    contentDescription = "Post image preview",
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
                )
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StackTag(post.techStack)
                post.domain?.takeIf { it.isNotBlank() }?.let { StackTag(it) }
            }
        }
    }
}

@Composable
fun StackTag(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
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
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NetworkAvatar(
                    url = null, 
                    name = post.authorName.ifBlank { "Member" }, 
                    modifier = Modifier.size(44.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(post.authorName.ifBlank { "Member" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(post.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(post.topic, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(4.dp))
            Text(post.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!post.imageUrl.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                NetworkImage(
                    url = post.imageUrl,
                    contentDescription = "Learning post attachment",
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
                )
            }
            if (post.needsHelp) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        "Stuck on this – looking for help",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            if (post.links.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("References", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                post.links.forEach { link ->
                    Text(link, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                }
            }
            if (isMine) {
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    onEdit?.let { TextButton(onClick = it) { Text("Edit") } }
                    TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}
