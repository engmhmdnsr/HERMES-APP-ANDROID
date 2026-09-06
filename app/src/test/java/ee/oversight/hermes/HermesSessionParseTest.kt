package ee.oversight.hermes

import ee.oversight.hermes.model.HermesSession
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Session JSON parsing rules (mirrors HermesNetworkClient.fetchSessions):
 * - Server sends started_at / last_active as FLOAT epoch SECONDS -> app stores millis.
 * - costUsd prefers actual_cost_usd, falls back to estimated_cost_usd.
 * - A missing/blank last_active falls back to started_at.
 * Runs under Robolectric because org.json is an Android stub in plain unit tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HermesSessionParseTest {

    /** Same parse logic as HermesNetworkClient.fetchSessions. */
    private fun parseSession(obj: JSONObject): HermesSession {
        val startedRaw = obj.optDouble("started_at", 0.0)
        return HermesSession(
            id = obj.getString("id"),
            title = obj.optString("title", ""),
            startedAt = (startedRaw * 1000).toLong().takeIf { it > 0 } ?: System.currentTimeMillis(),
            costUsd = obj.optDouble("actual_cost_usd", 0.0)
                .takeIf { it > 0 } ?: obj.optDouble("estimated_cost_usd", 0.0),
            lastActiveAt = obj.optString("last_active").let { raw ->
                val ts = raw.toDoubleOrNull() ?: obj.optDouble("started_at", 0.0)
                (ts * 1000).toLong().takeIf { it > 0 } ?: System.currentTimeMillis()
            }
        )
    }

    @Test
    fun started_at_float_seconds_converts_to_millis() {
        val obj = JSONObject(
            """{"id":"20260901_120000_abc","title":"Test","started_at":1788397452.099}"""
        )
        val s = parseSession(obj)
        assertEquals(1788397452099L, s.startedAt)
    }

    @Test
    fun last_active_string_float_is_parsed() {
        val obj = JSONObject(
            """{"id":"s1","started_at":1000.5,"last_active":"1788579684.179"}"""
        )
        val s = parseSession(obj)
        assertEquals(1788579684179L, s.lastActiveAt)
    }

    @Test
    fun missing_last_active_falls_back_to_started_at() {
        val obj = JSONObject(
            """{"id":"s2","started_at":1000.5}"""
        )
        val s = parseSession(obj)
        assertEquals(1000500L, s.lastActiveAt)
    }

    @Test
    fun cost_prefers_actual_over_estimated() {
        val obj = JSONObject(
            """{"id":"s3","started_at":1.0,"actual_cost_usd":0.0421,"estimated_cost_usd":0.05}"""
        )
        val s = parseSession(obj)
        assertEquals(0.0421, s.costUsd, 0.0001)
    }

    @Test
    fun cost_falls_back_to_estimated_when_actual_zero() {
        val obj = JSONObject(
            """{"id":"s4","started_at":1.0,"actual_cost_usd":0.0,"estimated_cost_usd":0.031}"""
        )
        val s = parseSession(obj)
        assertEquals(0.031, s.costUsd, 0.0001)
    }

    @Test
    fun pinned_flag_and_message_count_parse() {
        val obj = JSONObject(
            """{"id":"s5","pinned":true,"message_count":42,"source":"mobile_app"}"""
        )
        val s = HermesSession(
            id = obj.getString("id"),
            title = "",
            messageCount = obj.optInt("message_count", 0),
            isPinned = obj.optBoolean("pinned", false),
            source = obj.optString("source", "")
        )
        assertTrue(s.isPinned)
        assertEquals(42, s.messageCount)
        assertEquals("mobile_app", s.source)
    }
}
