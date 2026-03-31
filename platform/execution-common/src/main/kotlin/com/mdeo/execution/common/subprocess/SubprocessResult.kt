package com.mdeo.execution.common.subprocess

/**
 * Result of a subprocess execution.
 */
sealed class SubprocessResult {
    /**
     * The subprocess completed successfully with the given result bytes.
     *
     * @property data The result data from the subprocess.
     */
    data class Success(val data: ByteArray) : SubprocessResult() {
        override fun equals(other: Any?): Boolean =
            other is Success && data.contentEquals(other.data)
        override fun hashCode(): Int = data.contentHashCode()
    }

    /**
     * The subprocess was terminated because a registered timeout expired.
     *
     * @property timeoutId The numeric identifier of the expired timeout.
     */
    data class Timeout(val timeoutId: Int) : SubprocessResult()

    /**
     * The subprocess was terminated because the external cancellation check triggered.
     */
    data object Cancelled : SubprocessResult()

    /**
     * The subprocess failed with an error.
     *
     * @property message Error description.
     * @property exitCode The process exit code, if available.
     */
    data class Failed(val message: String, val exitCode: Int? = null) : SubprocessResult()
}
