package ee.oversight.hermes

import ee.oversight.hermes.model.HermesSession
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Session ordering rules (the drawer list):
 * - Sessions sort by lastActiveAt DESC (most recent activity first).
 * - When lastActiveAt is 0 (server didn't report it), fall back to startedAt.
 * - bumpedSessionActivity sets lastActiveAt = now, so a session the user just
 *   messaged in must jump above everything with an older activity time.
 */
class SessionSortingTest {

    private fun session(id: String, startedAt: Long, lastActiveAt: Long = 0L, messageCount: Int = 0) =
        HermesSession(id = id, title = "S$id", startedAt = startedAt, lastActiveAt = lastActiveAt, messageCount = messageCount)

    private fun sortByActivity(sessions: List<HermesSession>): List<HermesSession> =
        sessions.sortedByDescending { it.lastActiveAt.takeIf { t -> t > 0 } ?: it.startedAt }

    @Test
    fun sessions_sort_by_last_active_descending() {
        val old = session("old", startedAt = 1000L, lastActiveAt = 2000L)
        val recent = session("recent", startedAt = 3000L, lastActiveAt = 4000L)
        val newest = session("newest", startedAt = 5000L, lastActiveAt = 6000L)

        val sorted = sortByActivity(listOf(old, newest, recent))

        assertEquals(listOf("newest", "recent", "old"), sorted.map { it.id })
    }

    @Test
    fun sessions_with_zero_last_active_fall_back_to_started_at() {
        // lastActiveAt=0 means the server didn't report activity — use creation time.
        val a = session("a", startedAt = 1000L)          // lastActiveAt = 0 -> falls back to 1000
        val b = session("b", startedAt = 2000L)          // falls back to 2000
        val c = session("c", startedAt = 1500L, lastActiveAt = 5000L) // real activity

        val sorted = sortByActivity(listOf(a, c, b))

        // c has real activity (5000) -> first. b (2000) > a (1000).
        assertEquals(listOf("c", "b", "a"), sorted.map { it.id })
    }

    @Test
    fun recently_bumped_session_jumps_to_top() {
        val now = 1_000_000L
        val a = session("a", startedAt = 5000L, lastActiveAt = 9000L)
        val b = session("b", startedAt = 4000L, lastActiveAt = 8000L)
        val c = session("c", startedAt = 3000L, lastActiveAt = 7000L)

        // simulate bumpSessionActivity("a") -> its lastActiveAt = now
        val bumped = listOf(
            a.copy(lastActiveAt = now),
            b, c
        )
        val sorted = sortByActivity(bumped)

        assertEquals("a", sorted.first().id)
    }

    @Test
    fun server_session_list_keeps_newer_local_bump_on_merge() {
        // loadSessions merges server rows with the local list; a local bump
        // newer than the server's timestamp must survive the refresh.
        val serverRow = session("s1", startedAt = 1000L, lastActiveAt = 5000L, messageCount = 3)
        val localBumped = serverRow.copy(lastActiveAt = 9000L)

        val merged = listOf(serverRow).map { fresh ->
            val old = localBumped
            if (old.lastActiveAt > fresh.lastActiveAt) fresh.copy(lastActiveAt = old.lastActiveAt) else fresh
        }

        assertEquals(9000L, merged.first().lastActiveAt)
    }
}
