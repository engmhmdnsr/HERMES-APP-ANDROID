package ee.oversight.hermes

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import ee.oversight.hermes.ui.HermesViewModel

class HermesApp : Application() {
    // Application-scoped ViewModel: survives backgrounding / rotation, so a
    // streaming reply keeps going even when the UI process is not foregrounded.
    // The foreground StreamService keeps the process itself alive.
    val viewModel: HermesViewModel by lazy {
        HermesViewModel(this)
    }

    // Session id requested via a notification tap ("open_session" extra).
    // Written by MainActivity, consumed by MainScreen (which then clears it).
    val pendingOpenSession = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    // Approval requested via the notification's Approve action. Tapping it
    // opens the app (so biometric app-lock gates it) and this is set; the
    // ViewModel resolves the approval once the user is authenticated.
    val pendingApprovalRunId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val pendingApprovalSessionId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Streaming progress channel (foreground service)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_STREAM,
                "Agent running",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows while a reply is being generated"
                setShowBadge(false)
            }
        )

        // Reply-finished + approvals channel (user attention)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REPLY,
                "Replies & approvals",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reply ready and approval requests"
            }
        )

        // New inbound messages on other sessions
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_NEW_MSG,
                "New messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "A new message arrived on a session from elsewhere"
            }
        )
    }

    companion object {
        const val CHANNEL_STREAM = "hermes_stream"
        const val CHANNEL_REPLY = "hermes_reply"
        const val CHANNEL_NEW_MSG = "hermes_new_msg"
        const val NOTIF_STREAM_ID = 1001
        const val NOTIF_REPLY_ID = 1002
    }
}
