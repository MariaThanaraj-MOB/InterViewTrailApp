package com.interviewtrail.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Mirrors backend & Firebase Firestore schema. Keep in sync with InterviewTrail_Database_Document.md.

@Serializable
data class Location(val country: String = "India", val state: String? = null, val city: String? = null)

@Serializable
enum class ExperienceLevel(val label: String) {
    FRESHER("Fresher"), JUNIOR_0_2("0–2 years"), MID_2_5("2–5 years"),
    SENIOR_5_10("5–10 years"), LEAD_10_PLUS("10+ years")
}

@Serializable
data class User(
    @SerialName("_id") val id: String,
    val email: String,
    val firstName: String = "",
    val lastName: String = "",
    val currentCompany: String? = null,
    val designationId: String? = null,
    val experienceLevel: ExperienceLevel? = null,
    val techStacks: List<String> = emptyList(),
    val domain: String? = null,
    val photoUrl: String? = null,
    val location: Location = Location(),
    val followedCompanyIds: List<String> = emptyList(),
    val subscribedLearningSpaces: List<String> = emptyList(),
    val isActive: Boolean = true,
    val createdAt: Long = 0,
)

@Serializable
data class UpdateProfileRequest(
    val firstName: String, val lastName: String, val currentCompany: String?,
    val experienceLevel: ExperienceLevel?, val techStacks: List<String>, val domain: String?,
    val photoUrl: String?, val location: Location = Location(),
)

@Serializable
enum class PostType(val label: String) {
    INTERVIEW_EXPERIENCE("Interview experience"),
    WORK_STATUS("Work status"),
    SEMINAR_WORKSHOP("Seminar or workshop"),
}

@Serializable data class QuestionAnswer(val question: String, val answer: String = "")
@Serializable data class InterviewRound(val roundName: String, val questions: List<QuestionAnswer> = emptyList())
@Serializable data class InterviewDetails(val interviewDate: String, val rounds: List<InterviewRound>, val notes: String? = null)
@Serializable data class SeminarDetails(val eventName: String, val eventDate: String, val topic: String, val takeaways: String)

@Serializable
data class Post(
    @SerialName("_id") val id: String,
    val type: PostType,
    val authorId: String,
    val authorName: String,
    val communityId: String? = null,
    val company: String? = null,
    val techStack: String,
    val domain: String? = null,
    val interview: InterviewDetails? = null,
    val seminar: SeminarDetails? = null,
    val imageUrls: List<String> = emptyList(),
    val sharedToGroupIds: List<String> = emptyList(),
    val createdAt: Long = 0,
    val isActive: Boolean = true,
)

@Serializable
data class CreatePostRequest(
    val type: PostType, val communityId: String?, val company: String?, val techStack: String,
    val domain: String? = null, val interview: InterviewDetails? = null, val seminar: SeminarDetails? = null,
    val imageUrls: List<String> = emptyList(), val groupId: String? = null,
)

@Serializable data class SharePostRequest(val groupIds: List<String>)

@Serializable data class Community(@SerialName("_id") val id: String, val name: String, val techStack: String, val description: String = "", val isActive: Boolean = true)
@Serializable data class Company(@SerialName("_id") val id: String, val name: String, val logoUrl: String? = null, val isActive: Boolean = true)

@Serializable enum class CommunityTab { INTERVIEW, WORKSHOP_SEMINAR }
@Serializable enum class GroupKind { PRIVATE, LEARNING }

@Serializable
data class Group(
    @SerialName("_id") val id: String,
    val kind: GroupKind,
    val name: String,
    val description: String = "",
    val ownerId: String,
    val memberIds: List<String> = emptyList(),
    val memberCount: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = 0,
)
@Serializable data class CreateGroupRequest(val kind: GroupKind, val name: String, val description: String = "")
@Serializable data class CreateInviteRequest(val email: String)
@Serializable data class InviteResponse(val token: String, val link: String)

@Serializable enum class ReminderOutcome { PENDING, HAPPENED, DID_NOT_HAPPEN }
@Serializable
data class InterviewReminder(
    @SerialName("_id") val id: String,
    val userId: String = "",
    val company: String,
    val venue: String,
    val scheduledAt: Long,
    val remindEnabled: Boolean = true,
    val outcome: ReminderOutcome = ReminderOutcome.PENDING,
    val createdAt: Long = 0,
)
@Serializable data class CreateReminderRequest(val company: String, val venue: String, val scheduledAt: Long, val remindEnabled: Boolean = true)
@Serializable data class ToggleReminderRequest(val remindEnabled: Boolean)
@Serializable data class OutcomeRequest(val happened: Boolean)

@Serializable enum class SuggestionStatus { SUBMITTED, IN_REVIEW, ACCEPTED, REJECTED, DONE }
@Serializable data class Suggestion(
    @SerialName("_id") val id: String,
    val userId: String = "",
    val title: String,
    val description: String,
    val status: SuggestionStatus = SuggestionStatus.SUBMITTED,
    val createdAt: Long = 0,
)
@Serializable data class CreateSuggestionRequest(val title: String, val description: String)

@Serializable
data class LearningPost(
    @SerialName("_id") val id: String,
    val authorId: String,
    val authorName: String,
    val spaceId: String? = null,
    val groupId: String? = null,
    val date: String,
    val topic: String,
    val description: String,
    val needsHelp: Boolean = false,
    val imageUrl: String? = null,
    val links: List<String> = emptyList(),
    val createdAt: Long = 0,
    val isActive: Boolean = true,
)
@Serializable
data class CreateLearningPostRequest(
    val spaceId: String? = null, val groupId: String? = null, val date: String, val topic: String,
    val description: String, val needsHelp: Boolean = false, val imageUrl: String? = null,
    val links: List<String> = emptyList(),
)

@Serializable data class ApiError(val error: String)
