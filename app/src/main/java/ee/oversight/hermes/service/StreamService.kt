package ee.oversight.hermes.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import ee.oversight.hermes.HermesApp
import ee.oversight.hermes.MainActivity
import ee.oversight.hermes.R

/**
 * Foreground service that keeps the SSE chat stream alive while the app is in
 * the background or the screen is off. Started when a reply begins streaming,
 * stopped when it completes or the user hits Stop.
 *
 * The actual stream logic lives in HermesViewModel (Application-scoped); this
 * service only holds the process priority + shows the status notification.
 */
class StreamService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat()
        return START_STICKY
    }

    private fun startForegroundCompat() {
        val app = application as HermesApp
        val channelId = HermesApp.CHANNEL_STREAM

        // Stop action -> tells the ViewModel to cancel the stream
        val stopIntent = Intent(this, StreamStopReceiver::class.java)
        val stopPi = PendingIntent.getBroadcast(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tap -> open the app
        val openIntent = Intent(this, MainActivity::class.java)
        val openPi = PendingIntent.getActivity(
            this, 1, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Hermes is working")
            .setContentText("Reply is streaming from your agent")
            .setSmallIcon(R.drawable.ic_stat_hermes)
            .setOngoing(true)
            .setContentIntent(openPi)
            .addAction(0, "Stop", stopPi)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                HermesApp.NOTIF_STREAM_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(HermesApp.NOTIF_STREAM_ID, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
