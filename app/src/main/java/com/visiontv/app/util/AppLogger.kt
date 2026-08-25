package com.visiontv.app.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel {
    INFO, WARNING, ERROR
}

data class LogEntry(
    val level: LogLevel,
    val message: String,
    val tags: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val throwable: Throwable? = null,
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

object AppLogger {
    private const val TAG = "VisionTV"
    private const val MAX_LOGS = 500

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun info(message: String, tags: List<String> = emptyList()) {
        val tagString = if (tags.isEmpty()) "" else "[${tags.joinToString(", ")}] "
        Log.i(TAG, "$tagString$message")
        addLog(LogEntry(LogLevel.INFO, message, tags))
    }

    fun warning(message: String, tags: List<String> = emptyList()) {
        val tagString = if (tags.isEmpty()) "" else "[${tags.joinToString(", ")}] "
        Log.w(TAG, "$tagString$message")
        addLog(LogEntry(LogLevel.WARNING, message, tags))
    }

    fun error(message: String, tags: List<String> = emptyList(), throwable: Throwable? = null) {
        val tagString = if (tags.isEmpty()) "" else "[${tags.joinToString(", ")}] "
        Log.e(TAG, "$tagString$message", throwable)
        addLog(LogEntry(LogLevel.ERROR, message, tags, throwable = throwable))
    }

    private fun addLog(entry: LogEntry) {
        _logs.update { current ->
            (current + entry).takeLast(MAX_LOGS)
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }
}
