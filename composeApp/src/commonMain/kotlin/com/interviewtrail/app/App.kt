package com.interviewtrail.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.interviewtrail.app.data.AppContainer
import com.interviewtrail.app.data.AuthState
import com.interviewtrail.app.ui.screens.AuthScreen
import com.interviewtrail.app.ui.screens.MainScreen
import com.interviewtrail.app.ui.screens.VerifyEmailScreen
import com.interviewtrail.app.ui.theme.InterviewTrailTheme

import com.interviewtrail.app.ui.screens.SplashScreen
import kotlinx.coroutines.delay

val LocalContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }

/** Shared entry point for Android, iOS and Web. */
@Composable
fun App(container: AppContainer = remember { AppContainer() }) {
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2000) // Show splash for 2 seconds
        showSplash = false
    }

    InterviewTrailTheme {
        CompositionLocalProvider(LocalContainer provides container) {
            val auth by container.auth.state.collectAsState()
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                if (showSplash) {
                    SplashScreen()
                } else {
                    when (val s = auth) {
                        AuthState.SignedOut -> AuthScreen()
                        is AuthState.NeedsVerification -> VerifyEmailScreen(s.email)
                        is AuthState.SignedIn -> MainScreen(myUid = s.uid)
                    }
                }
            }
        }
    }
}
