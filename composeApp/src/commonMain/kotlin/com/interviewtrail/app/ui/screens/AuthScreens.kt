package com.interviewtrail.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.interviewtrail.app.LocalContainer
import com.interviewtrail.app.platform.rememberGoogleSignIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import com.interviewtrail.app.ui.components.ErrorText
import kotlinx.coroutines.launch

@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // "iT" logo mark
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "iT",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 72.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(56.dp).offset(x = 12.dp, y = (-4).dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "INTERVIEW ",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "TRAIL",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Your Career Journey, Simplified",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        AppLogo()
    }
}

@Composable
fun AuthScreen() {
    val auth = LocalContainer.current.auth
    val scope = rememberCoroutineScope()
    val google = rememberGoogleSignIn()
    var creating by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    fun launchAction(block: suspend () -> Unit) = scope.launch {
        busy = true; error = null; info = null
        runCatching { block() }.onFailure { error = it.message }
        busy = false
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).widthIn(max = 440.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppLogo(Modifier.padding(bottom = 32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        ErrorText(error)
        info?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp)) }
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                launchAction {
                    val cleanEmail = email.trim()
                    if (creating) auth.signUp(cleanEmail, password) else auth.signIn(cleanEmail, password)
                }
            },
            enabled = !busy && email.trim().contains("@") && password.length >= 6,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text(if (creating) "Create account" else "Sign in") }

        if (google != null) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { launchAction { google()?.let { auth.signInWithGoogleIdToken(it) } } },
                enabled = !busy, modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("Continue with Google") }
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { creating = !creating; error = null }) {
                Text(if (creating) "I already have an account" else "Create an account")
            }
            if (!creating) TextButton(
                onClick = { launchAction { auth.sendPasswordReset(email.trim()); info = "Password reset email sent to ${email.trim()}." } },
                enabled = email.trim().contains("@"),
            ) { Text("Forgot password") }
        }
    }
}

@Composable
fun VerifyEmailScreen(email: String) {
    val auth = LocalContainer.current.auth
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(24.dp).widthIn(max = 440.dp), verticalArrangement = Arrangement.Center) {
        Text("Check your inbox", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("We sent a verification link to $email. Open it, then come back here.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            scope.launch {
                runCatching { auth.refreshVerificationState() }.onFailure { message = it.message }
                if (auth.state.value !is com.interviewtrail.app.data.AuthState.SignedIn)
                    message = "Not verified yet. The link can take a minute to arrive."
            }
        }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("I've verified my email") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = {
            scope.launch { runCatching { auth.sendVerificationEmail() }; message = "Sent another link to $email." }
        }, modifier = Modifier.fillMaxWidth()) { Text("Resend link") }
        TextButton(onClick = auth::signOut) { Text("Use a different email") }
        message?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)) }
    }
}
