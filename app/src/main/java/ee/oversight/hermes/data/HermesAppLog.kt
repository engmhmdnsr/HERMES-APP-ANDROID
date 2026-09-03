package ee.oversight.hermes.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Simple in-app event logger.
 * Stores the last [MAX_ENTRIES] log lines in memory so the user can view
 * what the app has been doing (connections, sends, errors) from About Us.
 */
object HermesAppLog {
    const val MAX_ENTRIES = 200

    data class LogEntry(
        val timestamp: Long = System.currentTimeMillis(),
        val level: String,   // "INFO", "WARN", "ERROR"
        val message: String
    )

    private val entries = CopyOnWriteArrayList<LogEntry>()

    /** Thread-safe append with automatic size cap. */
    fun log(level: String, message: String) {
        entries.add(LogEntry(level = level, message = message))
        while (entries.size > MAX_ENTRIES) {
            entries.removeAt(0)
        }
    }

    fun info(message: String) = log("INFO", message)
    fun warn(message: String) = log("WARN", message)
    fun error(message: String) = log("ERROR", message)

    fun all(): List<LogEntry> = entries.toList()

    fun clear() = entries.clear()

    /** Render one entry as "HH:mm:ss [LEVEL] message". */
    fun formatEntry(e: LogEntry): String {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(e.timestamp))
        return "$time [${e.level}] ${e.message}"
    }
}
