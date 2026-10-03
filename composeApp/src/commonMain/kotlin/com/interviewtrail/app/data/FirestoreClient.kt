package com.interviewtrail.app.data

import com.interviewtrail.app.AppConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

/**
 * Direct connection to Firebase Firestore REST API (v1).
 * Communicates directly with Firestore using Firebase ID tokens and structured queries.
 */
class FirestoreClient(
    private val http: HttpClient,
    private val auth: AuthRepository,
) {
    private val projectId get() = AppConfig.FIREBASE_PROJECT_ID
    private val baseUrl get() = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents"

    private suspend fun HttpRequestBuilder.authorize() {
        auth.idToken()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
    }
    
    suspend fun uploadFile(path: String, bytes: ByteArray): String {
        val bucket = AppConfig.FIREBASE_STORAGE_BUCKET
        // Firebase Storage object names must have slashes URL-encoded (%2F) in both query params and object URLs
        val encodedName = path.split("/").joinToString("%2F") { it.encodeURLPathPart() }
        val url = "https://firebasestorage.googleapis.com/v0/b/$bucket/o?uploadType=media&name=$encodedName"
        val res = http.post(url) {
            authorize()
            contentType(ContentType.Image.JPEG)
            setBody(bytes)
        }
        if (!res.status.isSuccess()) {
            val errorMsg = when (res.status) {
                HttpStatusCode.NotFound -> "Firebase Storage bucket '$bucket' not found. Storage must be initialized in Firebase Console (Build > Storage)."
                HttpStatusCode.Forbidden, HttpStatusCode.Unauthorized -> "Firebase Storage permission denied (${res.status.value}). Check Firebase Storage Rules."
                else -> "Storage upload failed (${res.status.value})"
            }
            throw ApiException(errorMsg)
        }
        val body = res.body<JsonObject>()
        val token = body["downloadTokens"]?.jsonPrimitive?.content ?: ""
        return "https://firebasestorage.googleapis.com/v0/b/$bucket/o/$encodedName?alt=media&token=$token"
    }

    suspend fun getDoc(collection: String, docId: String): JsonObject? {
        val res = http.get("$baseUrl/$collection/$docId") { authorize() }
        if (res.status == HttpStatusCode.NotFound) return null
        if (!res.status.isSuccess()) throw ApiException("Firestore GET failed (${res.status.value})")
        return decodeFirestoreDoc(res.body<JsonObject>())
    }

    suspend fun listDocs(collection: String): List<JsonObject> {
        val res = http.get("$baseUrl/$collection") { authorize() }
        if (!res.status.isSuccess()) return emptyList()
        val body = res.body<JsonObject>()
        val docs = body["documents"]?.jsonArray ?: return emptyList()
        return docs.mapNotNull { decodeFirestoreDoc(it.jsonObject) }
    }

    suspend fun setDoc(collection: String, docId: String, data: JsonObject): JsonObject {
        val fields = encodeToFirestoreFields(data)
        val body = buildJsonObject { put("fields", fields) }
        val res = http.patch("$baseUrl/$collection/$docId") {
            authorize()
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        if (!res.status.isSuccess()) {
            val err = runCatching { res.body<JsonObject>()["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content }.getOrNull()
            throw ApiException(err ?: "Firestore write failed (${res.status.value})")
        }
        return decodeFirestoreDoc(res.body<JsonObject>()) ?: data
    }

    suspend fun createDoc(collection: String, data: JsonObject, docId: String? = null): JsonObject {
        val fields = encodeToFirestoreFields(data)
        val body = buildJsonObject { put("fields", fields) }
        val url = if (docId.isNullOrBlank()) "$baseUrl/$collection" else "$baseUrl/$collection?documentId=$docId"
        val res = http.post(url) {
            authorize()
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        if (!res.status.isSuccess()) {
            val err = runCatching { res.body<JsonObject>()["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content }.getOrNull()
            throw ApiException(err ?: "Firestore create failed (${res.status.value})")
        }
        return decodeFirestoreDoc(res.body<JsonObject>()) ?: data
    }

    suspend fun deleteDoc(collection: String, docId: String) {
        val res = http.delete("$baseUrl/$collection/$docId") { authorize() }
        if (!res.status.isSuccess() && res.status != HttpStatusCode.NotFound) {
            throw ApiException("Firestore delete failed (${res.status.value})")
        }
    }

    suspend fun runQuery(collection: String, fieldFilter: Pair<String, String>? = null): List<JsonObject> {
        val structuredQuery = buildJsonObject {
            putJsonArray("from") {
                add(buildJsonObject { put("collectionId", collection) })
            }
            if (fieldFilter != null) {
                put("where", buildJsonObject {
                    put("fieldFilter", buildJsonObject {
                        put("field", buildJsonObject { put("fieldPath", fieldFilter.first) })
                        put("op", "EQUAL")
                        put("value", buildJsonObject { put("stringValue", fieldFilter.second) })
                    })
                })
            }
        }
        val res = http.post("$baseUrl:runQuery") {
            authorize()
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("structuredQuery", structuredQuery) })
        }
        if (!res.status.isSuccess()) return emptyList()
        val array = res.body<JsonArray>()
        return array.mapNotNull { item ->
            val doc = item.jsonObject["document"]?.jsonObject ?: return@mapNotNull null
            decodeFirestoreDoc(doc)
        }
    }

    private fun decodeFirestoreDoc(doc: JsonObject): JsonObject? {
        val name = doc["name"]?.jsonPrimitive?.content ?: return null
        val id = name.substringAfterLast('/')
        val fields = doc["fields"]?.jsonObject ?: JsonObject(emptyMap())
        val map = mutableMapOf<String, JsonElement>()
        map["_id"] = JsonPrimitive(id)
        for ((k, v) in fields) {
            map[k] = decodeFirestoreValue(v.jsonObject)
        }
        return JsonObject(map)
    }

    private fun decodeFirestoreValue(v: JsonObject): JsonElement {
        return when {
            v.containsKey("stringValue") -> v["stringValue"]!!
            v.containsKey("booleanValue") -> v["booleanValue"]!!
            v.containsKey("integerValue") -> JsonPrimitive(v["integerValue"]!!.jsonPrimitive.content.toLongOrNull() ?: 0L)
            v.containsKey("doubleValue") -> v["doubleValue"]!!
            v.containsKey("arrayValue") -> {
                val values = v["arrayValue"]?.jsonObject?.get("values")?.jsonArray ?: JsonArray(emptyList())
                JsonArray(values.map { decodeFirestoreValue(it.jsonObject) })
            }
            v.containsKey("mapValue") -> {
                val fields = v["mapValue"]?.jsonObject?.get("fields")?.jsonObject ?: JsonObject(emptyMap())
                JsonObject(fields.mapValues { decodeFirestoreValue(it.value.jsonObject) })
            }
            else -> JsonNull
        }
    }

    private fun encodeToFirestoreFields(data: JsonObject): JsonObject {
        val fields = mutableMapOf<String, JsonElement>()
        for ((k, v) in data) {
            if (k == "_id") continue
            fields[k] = encodeToFirestoreValue(v)
        }
        return JsonObject(fields)
    }

    private fun encodeToFirestoreValue(v: JsonElement): JsonObject {
        return when (v) {
            is JsonNull -> buildJsonObject { put("nullValue", JsonNull) }
            is JsonPrimitive -> {
                if (v.isString) buildJsonObject { put("stringValue", v.content) }
                else if (v.booleanOrNull != null) buildJsonObject { put("booleanValue", v.boolean) }
                else if (v.longOrNull != null) buildJsonObject { put("integerValue", v.content) }
                else if (v.doubleOrNull != null) buildJsonObject { put("doubleValue", v.double) }
                else buildJsonObject { put("stringValue", v.content) }
            }
            is JsonArray -> buildJsonObject {
                put("arrayValue", buildJsonObject {
                    putJsonArray("values") {
                        v.forEach { add(encodeToFirestoreValue(it)) }
                    }
                })
            }
            is JsonObject -> buildJsonObject {
                put("mapValue", buildJsonObject {
                    put("fields", encodeToFirestoreFields(v))
                })
            }
        }
    }
}
