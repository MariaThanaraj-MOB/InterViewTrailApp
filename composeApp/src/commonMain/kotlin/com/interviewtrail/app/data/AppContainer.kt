package com.interviewtrail.app.data

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/** Manual DI – small enough that a framework isn't worth the weight. */
class AppContainer {
    val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }

    // Engine comes from the platform dependency: OkHttp (Android), Darwin (iOS), Js (Web).
    val http = HttpClient {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) { requestTimeoutMillis = 20_000 }
    }

    val auth = AuthRepository(http)
    val api = ApiClient(http, auth)
}
