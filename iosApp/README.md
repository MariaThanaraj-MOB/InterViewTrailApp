# iosApp (Xcode host)

Contains only the SwiftUI entry point. To create the Xcode project:

1. In Xcode: File → New → Project → iOS App, name `iosApp`, save into this folder,
   and replace the generated Swift files with the two here.
2. Build Phases → add a **Run Script** before "Compile Sources":
   ```
   cd "$SRCROOT/../"
   ./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
   ```
3. Build Settings:
   - Framework Search Paths: `$(SRCROOT)/../composeApp/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`
   - Other Linker Flags: `-framework ComposeApp`
   - User Script Sandboxing: `No`
4. Info.plist:
   - `NSMicrophoneUsageDescription` / `NSSpeechRecognitionUsageDescription` (voice-to-text)
   - `NSAppTransportSecurity → NSAllowsLocalNetworking = YES` for local `http://localhost:8080`
   - `CADisableMinimumFrameDurationOnPhone = YES`

Faster alternative: generate a fresh project at https://kmp.jetbrains.com (Android + iOS + Web,
Compose shared UI) and copy its `iosApp/` folder here – it comes pre-wired.
