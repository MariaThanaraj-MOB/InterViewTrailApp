package com.interviewtrail.app

import com.interviewtrail.app.platform.defaultApiBaseUrl

object AppConfig {
    /** Firebase console → Project settings → Web API key. */
    const val FIREBASE_WEB_API_KEY = "AIzaSyAFPuxQqQnX16hNizTGrfLvFDJNAAJnkIE"

    /** Firebase console → Project settings → Project ID. */
    const val FIREBASE_PROJECT_ID = "interviewtrail"

    /** Firebase console → Project settings → Project Number / Sender ID. */
    const val FIREBASE_PROJECT_NUMBER = "953842341742"

    /** Firebase Storage Bucket for avatars and post images. */
    const val FIREBASE_STORAGE_BUCKET = "interviewtrail.firebasestorage.app"

    /** Override per build/environment. Android emulator default is 10.0.2.2. */
    val apiBaseUrl: String = defaultApiBaseUrl
}
