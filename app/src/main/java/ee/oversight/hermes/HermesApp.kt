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
    }

    companion object {
        const val CHANNEL_STREAM = "hermes_stream"
        const val CHANNEL_REPLY = "hermes_reply"
        const val NOTIF_STREAM_ID = 1001
        const val NOTIF_REPLY_ID = 1002
    }
}
