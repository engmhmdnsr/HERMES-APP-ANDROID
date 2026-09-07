package ee.oversight.hermes.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ee.oversight.hermes.HermesApp
import ee.oversight.hermes.model.ApprovalMode

/**
 * Handles both:
 * 1. The Stop button on the streaming notification -> cancels the current run.
 * 2. Approve/Deny actions on the approval notification -> resolves the pending
 *    approval request without opening the app.
 */
class StreamStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as HermesApp
        when (intent.action) {
            "ee.oversight.hermes.DENY" -> {
                val runId = intent.getStringExtra("run_id")
                val approved = intent.getBooleanExtra("approved", false)
                val sessionId = intent.getStringExtra("session_id")
                app.viewModel.resolveApprovalFromNotification(runId, approved, sessionId)
            }
            else -> {
                // Stop streaming button
                app.viewModel.stopStreaming()
            }
        }
    }
}
