package com.mdeo.backend.service

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthRateLimiterTest {
    @Test
    fun `a successful attempt is never counted against the caller`() {
        val limiter = AuthRateLimiter()

        // Smart HTTP authenticates twice per clone, fetch or push, so a user
        // doing ordinary git work passes through here far more often than the
        // per-username limit would allow if success counted. Ten operations'
        // worth, well past that limit, all with correct credentials.
        repeat(20) {
            assertTrue(limiter.isAllowed("alice", "10.0.0.1"))
            limiter.recordSuccess("alice", "10.0.0.1")
        }

        assertTrue(limiter.isAllowed("alice", "10.0.0.1"))
    }

    @Test
    fun `failures for one username are refused once the limit is reached`() {
        val limiter = AuthRateLimiter()

        repeat(5) {
            assertTrue(limiter.isAllowed("alice", "10.0.0.$it"))
            limiter.recordFailure("alice", "10.0.0.$it")
        }

        // Spread over five different addresses, so this is the per-username
        // limit talking and not the per-address one.
        assertFalse(limiter.isAllowed("alice", "10.0.0.99"))
    }

    @Test
    fun `failures from one address are refused once the limit is reached`() {
        val limiter = AuthRateLimiter()

        repeat(20) {
            limiter.recordFailure("user$it", "10.0.0.1")
        }

        // A username that has never failed, from the exhausted address.
        assertFalse(limiter.isAllowed("alice", "10.0.0.1"))
        // The same username from elsewhere is unaffected.
        assertTrue(limiter.isAllowed("alice", "10.0.0.2"))
    }

    @Test
    fun `a success clears failures the same caller had accumulated`() {
        val limiter = AuthRateLimiter()

        repeat(4) { limiter.recordFailure("alice", "10.0.0.1") }
        assertTrue(limiter.isAllowed("alice", "10.0.0.1"))

        limiter.recordSuccess("alice", "10.0.0.1")

        // Back to a full allowance rather than one attempt away from lockout,
        // so a mistyped password a few times does not follow a user around
        // once they get it right.
        repeat(4) {
            assertTrue(limiter.isAllowed("alice", "10.0.0.1"))
            limiter.recordFailure("alice", "10.0.0.1")
        }
        assertTrue(limiter.isAllowed("alice", "10.0.0.1"))
    }

    @Test
    fun `usernames are matched case insensitively`() {
        val limiter = AuthRateLimiter()

        repeat(5) { limiter.recordFailure("Alice", "10.0.0.$it") }

        assertFalse(limiter.isAllowed("alice", "10.0.0.99"))
    }

    @Test
    fun `a refused attempt does not itself count, so the window still expires`() {
        val limiter = AuthRateLimiter()

        repeat(5) { limiter.recordFailure("alice", "10.0.0.1") }
        assertFalse(limiter.isAllowed("alice", "10.0.0.1"))

        // Asking repeatedly must not push the window's start forward, which
        // would let a caller lock an account out indefinitely just by
        // continuing to knock.
        repeat(100) { assertFalse(limiter.isAllowed("alice", "10.0.0.1")) }

        limiter.recordSuccess("alice", "10.0.0.1")
        assertTrue(limiter.isAllowed("alice", "10.0.0.1"))
    }
}
