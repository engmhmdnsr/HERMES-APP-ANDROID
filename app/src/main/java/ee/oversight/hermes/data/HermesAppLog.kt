package ee.oversight.hermes.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Simple in-app event logger.
 * Stores the last [MAX_ENTRIES] log lines in memory so the user can view
 * what the app has been doing (connections, sends, errors) from About Us.
 *
 * Exposes a live [StateFlow] so any screen collecting [all] updates in real
 * time as soon as a new entry is appended, with no manual refresh needed.
 */
object HermesAppLog {
    const val MAX_ENTRIES = 200

    data class LogEntry(
        val timestamp: Long = System.currentTimeMillis(),
        val level: String,   // "INFO", "WARN", "ERROR"
        val message: String
    )

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())

    /** Live snapshot of all buffered entries (newest appended at the end). */
    val entries: StateFlow<List<LogEntry>> = _entries

    /** Thread-safe append with automatic size cap. */
    fun log(level: String, message: String) {
        val current = _entries.value
        val updated = (current + LogEntry(level = level, message = message)).takeLast(MAX_ENTRIES)
        _entries.value = updated
    }

    fun info(message: String) = log("INFO", message)
    fun warn(message: String) = log("WARN", message)
    fun error(message: String) = log("ERROR", message)

    /** Snapshot for one-shot reads (e.g. markdown export). */
    fun all(): List<LogEntry> = _entries.value

    fun clear() {
        _entries.value = emptyList()
    }

    /** Render one entry as "HH:mm:ss [LEVEL] message". */
    fun formatEntry(e: LogEntry): String {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(e.timestamp))
        return "$time [${e.level}] ${e.message}"
    }
}
