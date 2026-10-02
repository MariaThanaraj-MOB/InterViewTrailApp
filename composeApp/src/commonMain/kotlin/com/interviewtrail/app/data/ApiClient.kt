package com.interviewtrail.app.data

import kotlinx.datetime.Clock
import kotlinx.serialization.json.*

class ApiException(message: String) : Exception(message)

/**
 * Direct Firebase Firestore client implementing all operations against Firestore collections
 * as specified in InterviewTrail_Database_Document.md.
 */
class ApiClient(
    private val http: io.ktor.client.HttpClient,
    private val auth: AuthRepository,
) {
    val firestore = FirestoreClient(http, auth)
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }

    // Master seed constants matching InterviewTrail_Database_Document.md
    object MasterData {
        val techStacks = listOf(
            "Android", "iOS", "Mobile Hybrid", "Frontend", "Backend", "Full Stack",
            "Data Science", "Data Engineering", "DevOps", "Cloud", "SAP", "E-commerce",
            "Oracle", "Database Administration", "Testing/QA", "UI/UX Design",
            "Embedded Systems", "Machine Learning/AI", "Cybersecurity", "Networking"
        )
        val domains = listOf(
            "Banking/Financial Services", "Insurance", "Healthcare", "E-commerce/Retail",
            "Telecom", "Education/EdTech", "Travel/Hospitality", "Logistics/Supply Chain",
            "Manufacturing", "Media/Entertainment", "Government/Public Sector",
            "Real Estate", "Automotive", "Energy/Utilities", "HR/HRTech"
        )
        val defaultCompanies = listOf(
            Company("c1", "Google"), Company("c2", "Microsoft"), Company("c3", "Amazon"),
            Company("c4", "Meta"), Company("c5", "Apple"), Company("c6", "Infosys"),
            Company("c7", "TCS"), Company("c8", "Wipro"), Company("c9", "Accenture"),
            Company("c10", "Cognizant"), Company("c11", "Flipkart"), Company("c12", "Paytm")
        )
        val defaultCommunities = listOf(
            Community("comm1", "Android Developers", "Android", "Discussion and interview experiences for Android Engineers"),
            Community("comm2", "iOS & Swift Engineers", "iOS", "Swift, SwiftUI, and iOS interview prep"),
            Community("comm3", "Mobile Hybrid", "Mobile Hybrid", "Flutter, React Native, and cross-platform mobile"),
            Community("comm4", "Frontend Developers", "Frontend", "React, Vue, Web Development"),
            Community("comm5", "Backend & Microservices", "Backend", "System Design, Microservices, JVM, Go, Node"),
            Community("comm6", "Full Stack Engineers", "Full Stack", "End-to-end web & mobile engineering"),
            Community("comm7", "Data Science & AI", "Machine Learning/AI", "Data Engineering, AI, ML interviews"),
            Community("comm8", "DevOps & Cloud", "DevOps", "AWS, GCP, Kubernetes, CI/CD")
        )
        val defaultLearningSpaces = listOf(
            Community("ls1", "Android Learning Hub", "Android", "Master Android development and Kotlin Multiplatform"),
            Community("ls2", "iOS Learning Hub", "iOS", "Master Swift, SwiftUI, and iOS Architecture"),
            Community("ls3", "Backend Systems & Architecture", "Backend", "Deep dive into High Availability, Caching, and Databases"),
            Community("ls4", "Frontend Masters", "Frontend", "Modern CSS, JS frameworks, and Web Performance"),
            Community("ls5", "Data Engineering & AI", "Machine Learning/AI", "Data Pipelines, PyTorch, and LLMs")
        )
        val experienceLevels = listOf(
            "Fresher/0yr", "0-1 years", "1-3 years", "3-5 years",
            "5-8 years", "8-12 years", "12+ years"
        )
        val designations = listOf(
            "Software Engineer", "Senior Software Engineer", "Associate Software Engineer",
            "Technical Lead", "Engineering Manager", "QA Engineer", "DevOps Engineer",
            "Data Analyst", "Data Scientist", "Product Manager", "Business Analyst",
            "UI/UX Designer", "System Administrator", "Database Administrator"
        )
        val genders = listOf("Male", "Female", "Other", "Prefer not to say")
        val countries = listOf("India")
        val states = listOf(
            "Karnataka", "Telangana", "Maharashtra", "Tamil Nadu",
            "West Bengal", "Delhi NCR", "Haryana", "Uttar Pradesh", "Gujarat", "Kerala"
        )
        val cities = listOf(
            "Bangalore", "Hyderabad", "Mumbai", "Pune", "Chennai", "Coimbatore",
            "Kolkata", "Delhi", "Gurgaon", "Noida", "Ahmedabad", "Kochi", "Trivandrum"
        )
        val interviewRoundTypes = listOf(
            "Online Assessment/Coding Test", "Technical Round 1", "Technical Round 2",
            "Managerial Round", "HR Round", "System Design Round",
            "Group Discussion", "Final/Bar Raiser Round"
        )
        val jobTypes = listOf("Full-time", "Internship", "Contract", "Part-time")
        val notificationCategories = listOf(
            "Followed Company New Post", "Group Invitation", "New Post in Subscribed Community",
            "Interview Reminder", "Post-Interview Prompt", "Group Member Joined"
        )
    }

    private fun currentUid(): String {
        return auth.state.value.let {
            if (it is AuthState.SignedIn) it.uid else "anonymous"
        }
    }

    private fun currentEmail(): String {
        return auth.state.value.let {
            if (it is AuthState.SignedIn) it.email else ""
        }
    }

    // Profile
    suspend fun me(): User {
        val uid = currentUid()
        val doc = firestore.getDoc("users", uid)
        if (doc != null) {
            return runCatching { json.decodeFromJsonElement<User>(doc) }.getOrNull()
                ?: User(id = uid, email = currentEmail())
        }
        val defaultUser = User(id = uid, email = currentEmail())
        runCatching {
            firestore.createDoc("users", json.encodeToJsonElement(defaultUser).jsonObject, docId = uid)
        }
        return defaultUser
    }

    suspend fun updateMe(r: UpdateProfileRequest): User {
        val uid = currentUid()
        val existing = me()
        val updated = existing.copy(
            firstName = r.firstName,
            lastName = r.lastName,
            currentCompany = r.currentCompany,
            experienceLevel = r.experienceLevel,
            techStacks = r.techStacks,
            domain = r.domain,
            photoUrl = r.photoUrl,
            location = r.location
        )
        firestore.setDoc("users", uid, json.encodeToJsonElement(updated).jsonObject)
        return updated
    }

    suspend fun myPosts(page: Int = 0): List<Post> {
        val uid = currentUid()
        val docs = firestore.runQuery("interview_posts", "authorId" to uid) +
                firestore.runQuery("work_status_posts", "authorId" to uid) +
                firestore.runQuery("seminar_posts", "authorId" to uid)
        return docs.mapNotNull { runCatching { json.decodeFromJsonElement<Post>(it) }.getOrNull() }
            .sortedByDescending { it.createdAt }
    }

    // Communities & posts
    suspend fun communities(): List<Community> {
        val docs = firestore.listDocs("communities")
        if (docs.isEmpty()) {
            MasterData.defaultCommunities.forEach { comm ->
                runCatching { firestore.setDoc("communities", comm.id, json.encodeToJsonElement(comm).jsonObject) }
            }
            return MasterData.defaultCommunities
        }
        return docs.mapNotNull { runCatching { json.decodeFromJsonElement<Community>(it) }.getOrNull() }
    }

    suspend fun communityPosts(id: String, tab: CommunityTab, company: String?, page: Int = 0): List<Post> {
        val collection = when (tab) {
            CommunityTab.INTERVIEW -> "interview_posts"
            CommunityTab.WORKSHOP_SEMINAR -> "seminar_posts"
        }
        val docs = firestore.runQuery(collection, "communityId" to id)
        var posts = docs.mapNotNull { runCatching { json.decodeFromJsonElement<Post>(it) }.getOrNull() }
        if (!company.isNullOrBlank()) {
            posts = posts.filter { it.company?.equals(company, ignoreCase = true) == true }
        }
        return posts.sortedByDescending { it.createdAt }
    }

    suspend fun post(id: String): Post {
        val doc = firestore.getDoc("interview_posts", id)
            ?: firestore.getDoc("work_status_posts", id)
            ?: firestore.getDoc("seminar_posts", id)
            ?: throw ApiException("Post not found")
        return json.decodeFromJsonElement(doc)
    }

    suspend fun createPost(r: CreatePostRequest): Post {
        val uid = currentUid()
        val user = me()
        val authorName = listOf(user.firstName, user.lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { user.email.substringBefore('@') }
        val collection = when (r.type) {
            PostType.INTERVIEW_EXPERIENCE -> "interview_posts"
            PostType.WORK_STATUS -> "work_status_posts"
            PostType.SEMINAR_WORKSHOP -> "seminar_posts"
        }
        val p = Post(
            id = "",
            type = r.type,
            authorId = uid,
            authorName = authorName,
            communityId = r.communityId,
            company = r.company,
            techStack = r.techStack,
            domain = r.domain,
            interview = r.interview,
            seminar = r.seminar,
            imageUrls = r.imageUrls,
            sharedToGroupIds = if (r.groupId != null) listOf(r.groupId) else emptyList(),
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )
        val resDoc = firestore.createDoc(collection, json.encodeToJsonElement(p).jsonObject)
        return json.decodeFromJsonElement(resDoc)
    }

    suspend fun updatePost(id: String, r: CreatePostRequest): Post {
        val existing = post(id)
        val collection = when (existing.type) {
            PostType.INTERVIEW_EXPERIENCE -> "interview_posts"
            PostType.WORK_STATUS -> "work_status_posts"
            PostType.SEMINAR_WORKSHOP -> "seminar_posts"
        }
        val updated = existing.copy(
            company = r.company,
            techStack = r.techStack,
            domain = r.domain,
            interview = r.interview,
            seminar = r.seminar,
            imageUrls = r.imageUrls,
        )
        val resDoc = firestore.setDoc(collection, id, json.encodeToJsonElement(updated).jsonObject)
        return json.decodeFromJsonElement(resDoc)
    }

    suspend fun deletePost(id: String) {
        runCatching { firestore.deleteDoc("interview_posts", id) }
        runCatching { firestore.deleteDoc("work_status_posts", id) }
        runCatching { firestore.deleteDoc("seminar_posts", id) }
    }

    suspend fun sharePost(id: String, groupIds: List<String>): Post {
        val existing = post(id)
        val collection = when (existing.type) {
            PostType.INTERVIEW_EXPERIENCE -> "interview_posts"
            PostType.WORK_STATUS -> "work_status_posts"
            PostType.SEMINAR_WORKSHOP -> "seminar_posts"
        }
        val updated = existing.copy(sharedToGroupIds = (existing.sharedToGroupIds + groupIds).distinct())
        val resDoc = firestore.setDoc(collection, id, json.encodeToJsonElement(updated).jsonObject)
        return json.decodeFromJsonElement(resDoc)
    }

    // Groups
    suspend fun groups(kind: GroupKind): List<Group> {
        val uid = currentUid()
        val docs = firestore.listDocs("groups")
        val groups = docs.mapNotNull { runCatching { json.decodeFromJsonElement<Group>(it) }.getOrNull() }
        return groups.filter { it.kind == kind && (it.ownerId == uid || uid in it.memberIds) }
    }

    suspend fun createGroup(r: CreateGroupRequest): Group {
        val uid = currentUid()
        val g = Group(
            id = "",
            kind = r.kind,
            name = r.name,
            description = r.description,
            ownerId = uid,
            memberIds = listOf(uid),
            memberCount = 1,
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )
        val resDoc = firestore.createDoc("groups", json.encodeToJsonElement(g).jsonObject)
        return json.decodeFromJsonElement(resDoc)
    }

    suspend fun invite(groupId: String, email: String): InviteResponse {
        val uid = currentUid()
        val token = "invite_${groupId}_${Clock.System.now().toEpochMilliseconds()}"
        val inviteData = buildJsonObject {
            put("groupId", groupId)
            put("invitedBy", uid)
            put("invitedEmail", email)
            put("token", token)
            put("status", "PENDING")
            put("createdAt", Clock.System.now().toEpochMilliseconds())
        }
        firestore.createDoc("group_invitations", inviteData, docId = token)
        return InviteResponse(token, "https://interviewtrail.app/invite/$token")
    }

    suspend fun acceptInvite(token: String): Group {
        val uid = currentUid()
        val inviteDoc = firestore.getDoc("group_invitations", token) ?: throw ApiException("Invalid or expired invite link")
        val groupId = inviteDoc["groupId"]?.jsonPrimitive?.content ?: throw ApiException("Invalid invite")
        val groupDoc = firestore.getDoc("groups", groupId) ?: throw ApiException("Group no longer exists")
        val group = json.decodeFromJsonElement<Group>(groupDoc)
        val updatedGroup = group.copy(
            memberIds = (group.memberIds + uid).distinct(),
            memberCount = (group.memberIds + uid).distinct().size
        )
        firestore.setDoc("groups", groupId, json.encodeToJsonElement(updatedGroup).jsonObject)
        firestore.setDoc("group_invitations", token, buildJsonObject {
            put("status", "ACCEPTED")
            put("acceptedBy", uid)
        })
        return updatedGroup
    }

    suspend fun leaveGroup(id: String) {
        val uid = currentUid()
        val groupDoc = firestore.getDoc("groups", id) ?: return
        val group = json.decodeFromJsonElement<Group>(groupDoc)
        val updated = group.copy(
            memberIds = group.memberIds - uid,
            memberCount = (group.memberIds - uid).size
        )
        firestore.setDoc("groups", id, json.encodeToJsonElement(updated).jsonObject)
    }

    suspend fun deleteGroup(id: String) {
        firestore.deleteDoc("groups", id)
    }

    suspend fun groupPosts(id: String, page: Int = 0): List<Post> {
        val docs = firestore.listDocs("interview_posts") +
                firestore.listDocs("work_status_posts") +
                firestore.listDocs("seminar_posts")
        val posts = docs.mapNotNull { runCatching { json.decodeFromJsonElement<Post>(it) }.getOrNull() }
        return posts.filter { id in it.sharedToGroupIds || it.communityId == id }
            .sortedByDescending { it.createdAt }
    }

    // Companies
    suspend fun companies(q: String? = null): List<Company> {
        val docs = firestore.listDocs("companies")
        if (docs.isEmpty()) {
            MasterData.defaultCompanies.forEach { comp ->
                runCatching { firestore.setDoc("companies", comp.id, json.encodeToJsonElement(comp).jsonObject) }
            }
            return MasterData.defaultCompanies
        }
        var list = docs.mapNotNull { runCatching { json.decodeFromJsonElement<Company>(it) }.getOrNull() }
        if (!q.isNullOrBlank()) {
            list = list.filter { it.name.contains(q, ignoreCase = true) }
        }
        return list
    }

    suspend fun followedCompanies(): List<String> {
        return me().followedCompanyIds
    }

    suspend fun companyStacks(id: String): List<String> {
        val posts = companyPosts(id, null)
        return posts.map { it.techStack }.distinct().ifEmpty { MasterData.techStacks.take(4) }
    }

    suspend fun companyPosts(id: String, stack: String?, page: Int = 0): List<Post> {
        val companyObj = companies().find { it.id == id }
        val name = companyObj?.name ?: id
        val docs = firestore.runQuery("interview_posts", "company" to name) +
                firestore.runQuery("work_status_posts", "company" to name)
        var posts = docs.mapNotNull { runCatching { json.decodeFromJsonElement<Post>(it) }.getOrNull() }
        if (!stack.isNullOrBlank()) {
            posts = posts.filter { it.techStack.equals(stack, ignoreCase = true) }
        }
        return posts.sortedByDescending { it.createdAt }
    }

    suspend fun follow(id: String) {
        val uid = currentUid()
        val user = me()
        val updated = user.copy(followedCompanyIds = (user.followedCompanyIds + id).distinct())
        firestore.setDoc("users", uid, json.encodeToJsonElement(updated).jsonObject)
    }

    suspend fun unfollow(id: String) {
        val uid = currentUid()
        val user = me()
        val updated = user.copy(followedCompanyIds = user.followedCompanyIds - id)
        firestore.setDoc("users", uid, json.encodeToJsonElement(updated).jsonObject)
    }

    // Interview reminders
    suspend fun interviews(): List<InterviewReminder> {
        val uid = currentUid()
        val docs = firestore.runQuery("interview_reminders", "userId" to uid)
        return docs.mapNotNull { runCatching { json.decodeFromJsonElement<InterviewReminder>(it) }.getOrNull() }
            .sortedBy { it.scheduledAt }
    }

    suspend fun logInterview(r: CreateReminderRequest): InterviewReminder {
        val uid = currentUid()
        val reminder = InterviewReminder(
            id = "",
            userId = uid,
            company = r.company,
            venue = r.venue,
            scheduledAt = r.scheduledAt,
            remindEnabled = r.remindEnabled,
            outcome = ReminderOutcome.PENDING,
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )
        val resDoc = firestore.createDoc("interview_reminders", json.encodeToJsonElement(reminder).jsonObject)
        return json.decodeFromJsonElement(resDoc)
    }

    suspend fun pendingPrompts(): List<InterviewReminder> {
        val now = Clock.System.now().toEpochMilliseconds()
        return interviews().filter { it.scheduledAt <= now && it.outcome == ReminderOutcome.PENDING }
    }

    suspend fun toggleReminder(id: String, on: Boolean) {
        val doc = firestore.getDoc("interview_reminders", id) ?: return
        val reminder = json.decodeFromJsonElement<InterviewReminder>(doc)
        val updated = reminder.copy(remindEnabled = on)
        firestore.setDoc("interview_reminders", id, json.encodeToJsonElement(updated).jsonObject)
    }

    suspend fun setOutcome(id: String, happened: Boolean): InterviewReminder {
        val doc = firestore.getDoc("interview_reminders", id) ?: throw ApiException("Reminder not found")
        val reminder = json.decodeFromJsonElement<InterviewReminder>(doc)
        val updated = reminder.copy(outcome = if (happened) ReminderOutcome.HAPPENED else ReminderOutcome.DID_NOT_HAPPEN)
        val resDoc = firestore.setDoc("interview_reminders", id, json.encodeToJsonElement(updated).jsonObject)
        return json.decodeFromJsonElement(resDoc)
    }

    suspend fun deleteInterview(id: String) {
        firestore.deleteDoc("interview_reminders", id)
    }

    // Suggestions
    suspend fun suggestions(): List<Suggestion> {
        val uid = currentUid()
        val docs = firestore.runQuery("suggestions", "userId" to uid)
        return docs.mapNotNull { runCatching { json.decodeFromJsonElement<Suggestion>(it) }.getOrNull() }
            .sortedByDescending { it.createdAt }
    }

    suspend fun suggest(r: CreateSuggestionRequest): Suggestion {
        val uid = currentUid()
        val sugg = Suggestion(
            id = "",
            userId = uid,
            title = r.title,
            description = r.description,
            status = SuggestionStatus.SUBMITTED,
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )
        val resDoc = firestore.createDoc("suggestions", json.encodeToJsonElement(sugg).jsonObject)
        return json.decodeFromJsonElement(resDoc)
    }

    // Learning
    suspend fun learningSpaces(): List<Community> {
        val docs = firestore.listDocs("learning_spaces")
        if (docs.isEmpty()) {
            MasterData.defaultLearningSpaces.forEach { space ->
                runCatching { firestore.setDoc("learning_spaces", space.id, json.encodeToJsonElement(space).jsonObject) }
            }
            return MasterData.defaultLearningSpaces
        }
        return docs.mapNotNull { runCatching { json.decodeFromJsonElement<Community>(it) }.getOrNull() }
    }

    suspend fun subscribe(spaceId: String) {
        val uid = currentUid()
        val user = me()
        val updated = user.copy(subscribedLearningSpaces = (user.subscribedLearningSpaces + spaceId).distinct())
        firestore.setDoc("users", uid, json.encodeToJsonElement(updated).jsonObject)
    }

    suspend fun unsubscribe(spaceId: String) {
        val uid = currentUid()
        val user = me()
        val updated = user.copy(subscribedLearningSpaces = user.subscribedLearningSpaces - spaceId)
        firestore.setDoc("users", uid, json.encodeToJsonElement(updated).jsonObject)
    }

    suspend fun spacePosts(id: String, needsHelp: Boolean, page: Int = 0): List<LearningPost> {
        val docs = firestore.runQuery("learning_posts", "spaceId" to id)
        var posts = docs.mapNotNull { runCatching { json.decodeFromJsonElement<LearningPost>(it) }.getOrNull() }
        if (needsHelp) posts = posts.filter { it.needsHelp }
        return posts.sortedByDescending { it.createdAt }
    }

    suspend fun learningGroupPosts(id: String, needsHelp: Boolean, page: Int = 0): List<LearningPost> {
        val docs = firestore.runQuery("learning_posts", "groupId" to id)
        var posts = docs.mapNotNull { runCatching { json.decodeFromJsonElement<LearningPost>(it) }.getOrNull() }
        if (needsHelp) posts = posts.filter { it.needsHelp }
        return posts.sortedByDescending { it.createdAt }
    }

    suspend fun createLearningPost(r: CreateLearningPostRequest): LearningPost {
        val uid = currentUid()
        val user = me()
        val authorName = listOf(user.firstName, user.lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { user.email.substringBefore('@') }
        val p = LearningPost(
            id = "",
            authorId = uid,
            authorName = authorName,
            spaceId = r.spaceId,
            groupId = r.groupId,
            date = r.date,
            topic = r.topic,
            description = r.description,
            needsHelp = r.needsHelp,
            imageUrl = r.imageUrl,
            links = r.links,
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )
        val resDoc = firestore.createDoc("learning_posts", json.encodeToJsonElement(p).jsonObject)
        return json.decodeFromJsonElement(resDoc)
    }

    suspend fun updateLearningPost(id: String, r: CreateLearningPostRequest): LearningPost {
        val existingDoc = firestore.getDoc("learning_posts", id) ?: throw ApiException("Learning post not found")
        val existing = json.decodeFromJsonElement<LearningPost>(existingDoc)
        val updated = existing.copy(
            topic = r.topic,
            description = r.description,
            needsHelp = r.needsHelp,
            imageUrl = r.imageUrl,
            links = r.links,
        )
        val resDoc = firestore.setDoc("learning_posts", id, json.encodeToJsonElement(updated).jsonObject)
        return json.decodeFromJsonElement(resDoc)
    }

    suspend fun deleteLearningPost(id: String) {
        firestore.deleteDoc("learning_posts", id)
    }
}
