package com.interviewtrail.app.data

import com.interviewtrail.app.AppConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

sealed interface AuthState {
    data object SignedOut : AuthState
    data class NeedsVerification(val email: String) : AuthState
    data class SignedIn(val uid: String, val email: String) : AuthState
}

/**
 * Firebase Authentication via its REST API, so the same code runs on Android, iOS and Web.
 * Email/password + mandatory email verification. Google sign-in: a platform launcher obtains a
 * Google ID token and passes it to [signInWithGoogleIdToken].
 *
 * TODO: persist the refresh token (DataStore / NSUserDefaults / localStorage) to stay signed in.
 */
class AuthRepository(private val http: HttpClient) {
    private val key = AppConfig.FIREBASE_WEB_API_KEY
    private val base = "https://identitytoolkit.googleapis.com/v1/accounts"

    private val _state = MutableStateFlow<AuthState>(AuthState.SignedOut)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private var idToken: String? = null
    private var refreshToken: String? = null
    private var expiresAtMs: Long = 0
    private var uid: String? = null
    private var email: String? = null

    suspend fun signUp(email: String, password: String) {
        val r: TokenResponse = post("signUp", buildJsonObject {
            put("email", email); put("password", password); put("returnSecureToken", true)
        })
        store(r)
        runCatching { sendVerificationEmail() }
        _state.value = AuthState.NeedsVerification(r.email.ifBlank { email })
    }

    suspend fun signIn(email: String, password: String) {
        val r: TokenResponse = post("signInWithPassword", buildJsonObject {
            put("email", email); put("password", password); put("returnSecureToken", true)
        })
        store(r)
        val verified = runCatching {
            val lookup: LookupResponse = post("lookup", buildJsonObject { put("idToken", r.idToken) })
            lookup.users.firstOrNull()?.emailVerified ?: true
        }.getOrDefault(true)

        val userEmail = r.email.ifBlank { email }
        _state.value = if (verified) AuthState.SignedIn(r.localId, userEmail)
        else AuthState.NeedsVerification(userEmail)
    }

    suspend fun signInWithGoogleIdToken(googleIdToken: String) {
        val r: TokenResponse = post("signInWithIdp", buildJsonObject {
            put("postBody", "id_token=$googleIdToken&providerId=google.com")
            put("requestUri", "http://localhost")
            put("returnSecureToken", true)
        })
        store(r)
        val userEmail = r.email.ifBlank { "google_user" }
        _state.value = AuthState.SignedIn(r.localId, userEmail)
    }

    suspend fun sendVerificationEmail() {
        post<JsonObject>("sendOobCode", buildJsonObject {
            put("requestType", "VERIFY_EMAIL"); put("idToken", idToken ?: return)
        })
    }

    suspend fun sendPasswordReset(email: String) {
        post<JsonObject>("sendOobCode", buildJsonObject { put("requestType", "PASSWORD_RESET"); put("email", email) })
    }

    /** Call after the user taps the link in their inbox. */
    suspend fun refreshVerificationState(forceTokenRefresh: Boolean = true) {
        if (forceTokenRefresh) {
            runCatching { refresh() }
        }
        val currentToken = idToken ?: return
        val currentUid = uid ?: return
        val currentEmail = email.orEmpty()
        val verified = runCatching {
            val lookup: LookupResponse = post("lookup", buildJsonObject { put("idToken", currentToken) })
            lookup.users.firstOrNull()?.emailVerified ?: true
        }.getOrDefault(true)

        _state.value = if (verified) AuthState.SignedIn(currentUid, currentEmail)
        else AuthState.NeedsVerification(currentEmail)
    }

    /** Fresh ID token for backend calls (refreshes 1 min before expiry). */
    suspend fun idToken(): String? {
        if (idToken == null && refreshToken == null) return null
        if (refreshToken != null && Clock.System.now().toEpochMilliseconds() > expiresAtMs - 60_000) {
            runCatching { refresh() }
        }
        return idToken
    }

    fun signOut() {
        idToken = null; refreshToken = null; uid = null; email = null
        _state.value = AuthState.SignedOut
    }

    private suspend fun refresh() {
        val rt = refreshToken ?: return
        val res = http.submitForm(
            url = "https://securetoken.googleapis.com/v1/token?key=$key",
            formParameters = parameters { append("grant_type", "refresh_token"); append("refresh_token", rt) },
        )
        if (!res.status.isSuccess()) throw AuthException(firebaseMessage(res.bodyAsText()))
        val r: RefreshResponse = res.body()
        idToken = r.idToken; refreshToken = r.refreshToken; uid = r.userId
        expiresAtMs = Clock.System.now().toEpochMilliseconds() + r.expiresIn.toLong() * 1000
    }

    private fun store(r: TokenResponse) {
        idToken = r.idToken; refreshToken = r.refreshToken; uid = r.localId; email = r.email
        expiresAtMs = Clock.System.now().toEpochMilliseconds() + r.expiresIn.toLong() * 1000
    }

    private suspend inline fun <reified T> post(method: String, body: JsonObject): T {
        val res = http.post("$base:$method?key=$key") {
            contentType(ContentType.Application.Json); setBody(body)
        }
        if (!res.status.isSuccess()) throw AuthException(firebaseMessage(res.bodyAsText()))
        return res.body()
    }

    private fun firebaseMessage(raw: String): String {
        val code = runCatching {
            Json.parseToJsonElement(raw).jsonObject["error"]!!.jsonObject["message"]!!.jsonPrimitive.content
        }.getOrDefault("")
        return when {
            code.startsWith("EMAIL_EXISTS") -> "An account with this email already exists. Sign in instead."
            code.startsWith("INVALID_LOGIN_CREDENTIALS") || code.startsWith("INVALID_PASSWORD") ||
            code.startsWith("EMAIL_NOT_FOUND") -> "Email or password is incorrect."
            code.startsWith("WEAK_PASSWORD") -> "Use a password with at least 6 characters."
            code.startsWith("OPERATION_NOT_ALLOWED") -> "Email/Password sign-in is not enabled in Firebase Console."
            code.startsWith("TOO_MANY_ATTEMPTS") -> "Too many attempts. Wait a few minutes and try again."
            else -> if (code.isNotBlank()) "Authentication error: $code" else "Sign-in failed. Check your connection and try again."
        }
    }

    @Serializable private data class TokenResponse(
        val idToken: String, val refreshToken: String, val expiresIn: String,
        val localId: String, val email: String = "",
    )
    @Serializable private data class RefreshResponse(
        @SerialName("id_token") val idToken: String,
        @SerialName("refresh_token") val refreshToken: String,
        @SerialName("expires_in") val expiresIn: String,
        @SerialName("user_id") val userId: String,
    )
    @Serializable private data class LookupUser(val email: String = "", val emailVerified: Boolean = false)
    @Serializable private data class LookupResponse(val users: List<LookupUser>)
}

class AuthException(message: String) : Exception(message)
