# InterviewTrail – Client (Android · iOS · Web)

One Kotlin Multiplatform project with **Compose Multiplatform** (Jetpack Compose APIs) –
the whole UI lives in `composeApp/src/commonMain` and runs on all three platforms.

```
app/
├── composeApp/src/
│   ├── commonMain/   shared UI, navigation, theme, API client, Firebase auth (REST)
│   ├── androidMain/  MainActivity, speech-to-text (SpeechRecognizer), share sheet
│   ├── iosMain/      MainViewController, share sheet (UIActivityViewController)
│   └── wasmJsMain/   Web entry + index.html, Web Speech API, Web Share API
└── iosApp/           SwiftUI host for the iOS framework (see iosApp/README.md)
```

## Setup
1. Put your Firebase **Web API key** in `commonMain/.../AppConfig.kt`.
2. Start the backend (`../backend`) on port 8080.
3. Open this folder in Android Studio (with the Kotlin Multiplatform plugin); it generates
   the Gradle wrapper. Or run `gradle wrapper` once.

| Platform | Run |
|---|---|
| Android | `./gradlew :composeApp:installDebug` (emulator reaches backend at `10.0.2.2:8080`) |
| Web | `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` |
| iOS | open `iosApp` in Xcode (see its README) and run on a simulator |

## What's in
Email/password sign-up with mandatory email verification · 5-tab bottom nav
(Communities, Groups, Companies, Learning, Profile) · community Interview / Workshop tabs with
company search · guided post forms (interview rounds with Q&A, work status, seminar) · edit /
delete / cross-post restricted to the original poster · private & learning groups with
email-bound invite links via the native share sheet · company follow + stack filter · interview
reminders and the post-interview yes/no prompt · suggestions with status · learning logs with a
"stuck" flag · voice-to-text on text fields (Android + Web) · calm light/dark theme.

## Still to wire (marked `TODO` in code)
- Google sign-in per platform (`rememberGoogleSignIn` – button hides until wired)
- Voice-to-text on iOS (`SFSpeechRecognizer`)
- Image pick → compress → upload (Firebase Storage, Cloudinary fallback)
- FCM token registration on device (`POST /api/v1/me/fcm-token`) and deep-link handling for invites
- Persisting the refresh token so users stay signed in across launches
