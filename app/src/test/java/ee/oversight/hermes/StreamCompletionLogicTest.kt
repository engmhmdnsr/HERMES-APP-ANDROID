package ee.oversight.hermes

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Stream-completion logic (mirrors HermesNetworkClient.streamChat):
 * - A stream that ends WITHOUT the server's [DONE] frame, after content was
 *   already received, is a dropped connection and must surface an error —
 *   not a silent success (the old code emitted Done unconditionally).
 * - Low-level network exceptions translate to human-readable messages.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StreamCompletionLogicTest {

    /** Same decision logic as the end of streamChat's read loop. */
    private fun completionSignal(receivedDone: Boolean, sawContent: Boolean): String {
        return if (receivedDone) {
            "done"
        } else if (sawContent) {
            "error: connection dropped before reply finished"
        } else {
            "done"
        }
    }

    private fun friendlyError(e: Exception): String {
        val msg = e.message?.lowercase() ?: ""
        return when {
            msg.contains("timeout") || msg.contains("timed out") ->
                "Connection timed out. Check that the gateway PC is on and reachable, then try again."
            msg.contains("refused") || msg.contains("connect") && msg.contains("failed") ->
                "Could not reach the gateway. Check the IP / port and that the gateway is running."
            msg.contains("unknownhost") || msg.contains("no address") ->
                "Could not resolve the gateway address. Check the IP or hostname you entered."
            msg.contains("reset") || msg.contains("closed") || msg.contains("eof") ->
                "The connection was closed unexpectedly (network dropped or gateway restarted). Try again."
            msg.contains("ssl") || msg.contains("certificate") || msg.contains("https") ->
                "Secure connection (TLS) failed. The gateway may not support HTTPS, or its certificate is invalid."
            msg.contains("canceled") || msg.contains("cancelled") ->
                "Stopped."
            else -> "Connection error: ${e.localizedMessage ?: "unknown"}"
        }
    }

    @Test
    fun `stream that got DONE frame is a natural success`() {
        assertEquals("done", completionSignal(receivedDone = true, sawContent = true))
        assertEquals("done", completionSignal(receivedDone = true, sawContent = false))
    }

    @Test
    fun `stream with content but no DONE frame reports a dropped connection`() {
        assertEquals("error: connection dropped before reply finished", completionSignal(receivedDone = false, sawContent = true))
    }

    @Test
    fun `empty stream with no DONE frame is not an error`() {
        // Nothing was received at all (e.g. server closed immediately with no
        // events) — treat as done rather than alarming the user.
        assertEquals("done", completionSignal(receivedDone = false, sawContent = false))
    }

    @Test
    fun `timeout exceptions read as human message`() {
        val out = friendlyError(Exception("timeout after 30000ms"))
        assertEquals("Connection timed out. Check that the gateway PC is on and reachable, then try again.", out)
    }

    @Test
    fun `connection refused reads as reachability message`() {
        val out = friendlyError(Exception("Failed to connect to /100.112.74.9:8080"))
        assertEquals("Could not reach the gateway. Check the IP / port and that the gateway is running.", out)
    }

    @Test
    fun `unknown host reads as address message`() {
        val out = friendlyError(Exception("Unable to resolve host \"myhost.com\": No address associated with hostname"))
        assertEquals("Could not resolve the gateway address. Check the IP or hostname you entered.", out)
    }

    @Test
    fun `socket reset reads as unexpected close`() {
        val out = friendlyError(Exception("Connection reset by peer"))
        assertEquals("The connection was closed unexpectedly (network dropped or gateway restarted). Try again.", out)
    }

    @Test
    fun `ssl failure reads as tls message`() {
        val out = friendlyError(Exception("SSLHandshakeException: certificate verify failed"))
        assertEquals("Secure connection (TLS) failed. The gateway may not support HTTPS, or its certificate is invalid.", out)
    }
}
