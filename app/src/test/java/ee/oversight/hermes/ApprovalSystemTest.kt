package ee.oversight.hermes

import ee.oversight.hermes.model.ApprovalMode
import ee.oversight.hermes.model.ApprovalRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApprovalSystemTest {

    @Test
    fun testApprovalRequestCreation() {
        val request = ApprovalRequest(
            runId = "run_test_001",
            callId = "call_test_002",
            sessionId = "sess_main",
            toolName = "terminal",
            command = "sudo systemctl restart nginx",
            reason = "Apply new reverse proxy config",
            message = "Hermes requests permission to restart nginx service."
        )

        assertEquals("run_test_001", request.runId)
        assertEquals("call_test_002", request.callId)
        assertEquals("sess_main", request.sessionId)
        assertEquals("terminal", request.toolName)
        assertEquals("sudo systemctl restart nginx", request.command)
        assertEquals("Apply new reverse proxy config", request.reason)
    }

    @Test
    fun testApprovalModeValues() {
        assertEquals(3, ApprovalMode.values().size)
        assertTrue(ApprovalMode.values().contains(ApprovalMode.MANUAL))
        assertTrue(ApprovalMode.values().contains(ApprovalMode.ALLOW_SESSION))
        assertTrue(ApprovalMode.values().contains(ApprovalMode.ALLOW_ALL))
    }

    @Test
    fun testAutoApproveEvaluationLogic() {
        var globalAutoApprove = false
        val sessionAutoApproveIds = mutableSetOf<String>()

        fun isApproved(sessionId: String?): Boolean {
            if (globalAutoApprove) return true
            if (sessionId == null) return false
            return sessionAutoApproveIds.contains(sessionId)
        }

        // Initially manual
        assertFalse(isApproved("session_1"))
        assertFalse(isApproved("session_2"))

        // Allow session 1
        sessionAutoApproveIds.add("session_1")
        assertTrue(isApproved("session_1"))
        assertFalse(isApproved("session_2"))

        // Global Allow All
        globalAutoApprove = true
        assertTrue(isApproved("session_1"))
        assertTrue(isApproved("session_2"))
        assertTrue(isApproved("any_session"))

        // Reset to manual
        globalAutoApprove = false
        sessionAutoApproveIds.clear()
        assertFalse(isApproved("session_1"))
    }

    @Test
    fun testPinnedSessionsSerialization() {
        val pinned = setOf("sess_123", "sess_456")
        val raw = pinned.joinToString(",")
        assertEquals("sess_123,sess_456", raw)

        val deserialized = raw.split(",").filter { it.isNotBlank() }.toSet()
        assertEquals(pinned, deserialized)

        // Empty case
        val emptyRaw = ""
        val emptyDeserialized = emptyRaw.split(",").filter { it.isNotBlank() }.toSet()
        assertTrue(emptyDeserialized.isEmpty())
    }
}
