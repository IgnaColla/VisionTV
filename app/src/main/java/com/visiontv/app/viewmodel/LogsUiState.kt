package com.visiontv.app.viewmodel

import com.visiontv.app.util.LogEntry

data class LogsUiState(
    val logs: List<LogEntry> = emptyList()
)
