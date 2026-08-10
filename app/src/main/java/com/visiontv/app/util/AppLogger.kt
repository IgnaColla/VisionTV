package com.visiontv.app.util

import android.util.Log

object AppLogger {
    private const val TAG = "VisionTV"

    fun info(message: String, tags: List<String> = emptyList()) {
        val tagString = if (tags.isEmpty()) "" else "[${tags.joinToString(", ")}] "
        Log.i(TAG, "$tagString$message")
    }

    fun warning(message: String, tags: List<String> = emptyList()) {
        val tagString = if (tags.isEmpty()) "" else "[${tags.joinToString(", ")}] "
        Log.w(TAG, "$tagString$message")
    }

    fun error(message: String, tags: List<String> = emptyList(), throwable: Throwable? = null) {
        val tagString = if (tags.isEmpty()) "" else "[${tags.joinToString(", ")}] "
        Log.e(TAG, "$tagString$message", throwable)
    }
}
