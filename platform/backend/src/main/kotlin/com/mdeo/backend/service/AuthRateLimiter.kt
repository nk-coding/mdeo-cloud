package com.mdeo.backend.service

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Throttles failed password verification attempts, shared between the login
 * route and git's HTTP basic authentication.
 *
 * Every request against either carries a username and password, and nothing
 * in the application locks out a failing account, so both are equally a
 * bcrypt oracle: the CPU cost verifyPassword is deliberately configured to
 * spend on every guess is otherwise available to a caller in a loop, and
 * git's basic-auth flow makes that trivial to script against.
 *
 * Only *failures* count. Correct credentials are free, and clear whatever
 * the same caller had accumulated. That distinction is the whole point:
 * smart HTTP authenticates twice per clone, fetch or push, so counting every
 * attempt would lock a legitimate user out of their own repository partway
 * through their third git command in a minute, while doing nothing extra
 * against a guessing loop - which never authenticates successfully, and so
 * is counted on every single try either way.
 *
 * Two independent limits apply to every failure, so neither a single
 * username nor a single address has to be exhausted on its own: many
 * usernames tried from one address, and one username tried from many
 * addresses, are both slowed down.
 *
 * Callers check with [isAllowed] before verifying and report the outcome
 * with [recordFailure] or [recordSuccess] afterwards.
 */
class AuthRateLimiter {
    private data class Window(var count: Int, var startedAt: Instant)

    private val byUsername = ConcurrentHashMap<String, Window>()
    private val byAddress = ConcurrentHashMap<String, Window>()

    /**
     * Whether an attempt may proceed to password verification at all.
     *
     * Records nothing itself, so a caller already over either limit never
     * reaches bcrypt for this attempt, and a caller under it pays nothing
     * for asking.
     *
     * @param username The username being authenticated as
     * @param remoteAddress The caller's address
     * @return true if the attempt is allowed, false if either limit is exhausted
     */
    fun isAllowed(username: String, remoteAddress: String): Boolean {
        val usernameOk = withinLimit(byUsername, username.lowercase(), MAX_FAILURES_PER_USERNAME)
        val addressOk = withinLimit(byAddress, remoteAddress, MAX_FAILURES_PER_ADDRESS)
        return usernameOk && addressOk
    }

    /**
     * Records one failed verification against both limits.
     *
     * Both counters are always recorded, even once one side has already
     * failed, so a caller alternating between two exhausted keys cannot use
     * a short circuit to dodge one side's window entirely.
     *
     * @param username The username the failed attempt was for
     * @param remoteAddress The address the failed attempt came from
     */
    fun recordFailure(username: String, remoteAddress: String) {
        record(byUsername, username.lowercase())
        record(byAddress, remoteAddress)
    }

    /**
     * Clears both windows after a successful verification, so a user who
     * mistyped a password a few times is not still carrying those failures
     * once they get it right.
     *
     * @param username The username that authenticated
     * @param remoteAddress The address it authenticated from
     */
    fun recordSuccess(username: String, remoteAddress: String) {
        byUsername.remove(username.lowercase())
        byAddress.remove(remoteAddress)
    }

    private fun withinLimit(windows: ConcurrentHashMap<String, Window>, key: String, limit: Int): Boolean {
        val window = windows[key] ?: return true
        if (Duration.between(window.startedAt, Instant.now()) > WINDOW) {
            return true
        }
        return window.count < limit
    }

    private fun record(windows: ConcurrentHashMap<String, Window>, key: String) {
        val now = Instant.now()
        windows.compute(key) { _, existing ->
            if (existing == null || Duration.between(existing.startedAt, now) > WINDOW) {
                Window(1, now)
            } else {
                existing.count += 1
                existing
            }
        }
        evictExpired(windows, now)
    }

    /**
     * Drops windows that have already expired, once a map has grown past the
     * point where holding them is worth anything.
     *
     * Without this the maps only ever grow: an unauthenticated caller
     * supplying a fresh username on every request adds one permanently
     * retained entry each time. The size check keeps this a rare full pass
     * rather than a scan on every failed attempt, and expired entries are
     * exactly the ones that no longer affect any decision, so dropping them
     * loses nothing.
     *
     * @param windows The map to sweep
     * @param now The current time, reused from the caller
     */
    private fun evictExpired(windows: ConcurrentHashMap<String, Window>, now: Instant) {
        if (windows.size <= EVICTION_THRESHOLD) {
            return
        }
        windows.entries.removeIf { Duration.between(it.value.startedAt, now) > WINDOW }
    }

    companion object {
        private val WINDOW: Duration = Duration.ofMinutes(1)

        /**
         * Deliberately tighter than the per-address limit: this is the one
         * that actually protects a specific account against a distributed
         * guessing attempt.
         */
        private const val MAX_FAILURES_PER_USERNAME = 5

        /**
         * Looser than the per-username limit, since one address (a shared
         * office connection, or a reverse proxy whose forwarded-for header
         * is not trusted) can legitimately carry many different users'
         * traffic.
         */
        private const val MAX_FAILURES_PER_ADDRESS = 20

        /**
         * How many tracked keys one map may hold before a failure also
         * sweeps it. Far above what any real deployment's genuine failures
         * reach in a minute, so ordinary operation never pays for the scan.
         */
        private const val EVICTION_THRESHOLD = 10_000
    }
}
