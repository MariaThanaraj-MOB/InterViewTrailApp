# InterviewTrail - Product Scope Document (Final)

## Mission
Help job seekers who struggle in interviews understand real interview processes to prepare better and land jobs. InterviewTrail is a lightweight, community-driven app for sharing real interview experiences. It is explicitly **not** a chat app — no free-form messaging, only structured guided posts.

## Scope
- IT-focused only (not non-IT roles)
- Location scope: India only at launch (Country/State/City hierarchy retained for future scalability)

## Tech Stack
- **Frontend:** Kotlin Multiplatform + Jetpack Compose (Android, iOS, Web)
- **Backend:** None. No custom backend service. Mobile and web apps connect directly to Firebase using the official Firebase SDKs (Firestore, Auth, Storage, Messaging).
- **Database:** Firebase Firestore (NoSQL, free Spark tier to start)
- **Auth / Push Notifications / Storage:** Firebase (free tier) — Authentication, Cloud Messaging, and Storage, all under the same Firebase project as Firestore
- **Server-side logic (where unavoidable):** Firebase Cloud Functions — used only for the small pieces of logic that cannot safely live on the client, such as auto-emailing the admin on a new Suggestion, sending scheduled Interview Reminders, and keeping counters (`subscriber_count`, `member_count`) in sync. Everything else is handled by the client apps directly reading and writing Firestore.
- **Permissions and validation:** enforced through Firestore Security Rules (e.g. only the document's original author can edit/delete it) rather than backend application code.

## Non-Functional Requirements
- Lightweight performance: minimal animations, compressed images
- Peaceful, calming UI color theme

## Core Features

### 1. Registration and Profile
Sign up via Google or email/password, with mandatory email verification. Profile includes first/last name, current company, experience level, technology stack, domain, and photo.

### 2. Structured Post Types
Three post types, all built as guided forms rather than free text chat:
- **Interview Experience Post:** date, company, stack, domain, questions and answers organized by interview round, notes, optional images.
- **Work Status Post:** company, technology stack.
- **Seminar/Workshop Post:** event name, date, topic, takeaways, images.

Voice-to-text (English only) is available globally on all text/description fields.

### 3. Communities
Pre-built communities organized by technology stack. Each community is split into tabs by content type: an Interview tab (searchable and filterable by company name) and a Workshop/Seminar tab.

### 4. Private Groups
User-created, invite-only groups. Invitations go out via a shareable link tied to the invited person's email (no phone number invites). The invite link can be shared through any external channel, such as WhatsApp, using the native share sheet.

### 5. Cross-Posting
Only the original poster can share their own community post into their private groups. Ownership is always preserved; nobody can share a post on someone else's behalf.

### 6. Companies Tab
A dedicated bottom navigation tab to browse content by company, then by stack or technology within that company. Users can follow companies to get notified of new posts.

### 7. Post Permissions
Only the original poster can edit or delete their own post, across all post types.

### 8. Interview Reminder and Post-Interview Prompt
Users can log an upcoming interview (date, time, company, venue) and receive a pre-interview reminder (user can toggle this off). After the interview date passes, a simple yes/no "did the interview happen" prompt leads into the post creation flow. There is no feedback or rating collection.

### 9. Profile Suggestions
Users can submit suggestions (title and description) for things like a missing technology stack. These are automatically emailed to the admin and tracked with a status.

### 10. Learning Section
A separate top-level section of the app, alongside Communities, Groups, and Companies, for day-by-day learning journals, similar in spirit to a LinkedIn-style progress feed.

Two ways to participate:
- **Technology-based Learning Space:** mirrors the Community structure. Anyone subscribed to a technology's Learning Space can post their day-by-day learning logs.
- **Learning Groups:** user-created and invite-only, using the same invite-by-email-link model as private Groups. Members post their daily logs within the group.

Each Learning Post includes: the date, a topic, a description, and an optional flag for a problem or doubt the user is stuck on and wants help solving. An optional image can also be attached.

Voice-to-text (English only) is available on both the topic field and the description field, consistent with the rest of the app — the user speaks, it is converted to text, and they can then review and post.

Only the original poster can edit or delete their own Learning post.

Comments are explicitly deferred to Phase 2, and will apply to both Learning posts and Interview Experience posts. Phase 1 ships without any comment or reply functionality.

## Go-To-Market
Seed content within a tight network (college alumni, colleagues) before opening up to public communities, to solve the cold-start problem common to community-driven apps.

## Competitive Landscape
Similar in spirit to AmbitionBox, Blind, and Ratelys, but differentiated by: structured Q&A format tied to company, stack, and domain; private group sharing; a strict non-chat, lightweight design; and the Learning section as a unique addition not offered by direct competitors.

## Infrastructure Note
The entire backend now runs on a single Firebase project — Firestore for data, Authentication for sign-in, Cloud Messaging for push notifications, and Storage for images. This keeps everything under one vendor and one free tier, simplifying setup for a first-time build.

## First Release Philosophy
This scope is considered complete and sufficient for a first release (Phase 1). The plan is to launch with this feature set, seed it within a trusted network, and let real user feedback determine what (if anything) gets added next, rather than expanding scope before launch.
