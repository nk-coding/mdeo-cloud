package com.mdeo.execution.common.subprocess

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException

/**
 * Typed message in the parent-subprocess communication protocol.
 *
 * Messages are framed as `[4-byte big-endian length][CBOR-encoded SubprocessMessage]`.
 * Integer [id] fields on request/response messages allow request-response correlation
 * and provide a stable hook for future concurrent request support.
 *
 * Protocol flow:
 * 1. Parent starts subprocess; subprocess calls [SubprocessMain.run].
 * 2. Subprocess sends [Ready] once initialization is complete.
 * 3. Parent sends [Command]; subprocess responds with [Result] or [Error] carrying the same [id].
 * 4. Either side may send [Channel] at any time for out-of-band communication.
 * 5. Parent sends [Quit] to request clean shutdown.
 */
@Serializable
sealed class SubprocessMessage {

    /**
     * Sent by the parent to request command execution.
     *
     * @param id Correlation identifier; the subprocess echoes it in [Result] or [Error].
     * @param payload Opaque command bytes interpreted by the subprocess implementation.
     */
    @Serializable
    @SerialName("cmd")
    data class Command(val id: Int, val payload: ByteArray) : SubprocessMessage() {
        override fun equals(other: Any?) = other is Command && id == other.id && payload.contentEquals(other.payload)
        override fun hashCode() = 31 * id + payload.contentHashCode()
    }

    /**
     * Sent by the subprocess after a [Command] completes successfully.
     *
     * @param id The [Command.id] being answered.
     * @param payload Opaque result bytes.
     */
    @Serializable
    @SerialName("result")
    data class Result(val id: Int, val payload: ByteArray) : SubprocessMessage() {
        override fun equals(other: Any?) = other is Result && id == other.id && payload.contentEquals(other.payload)
        override fun hashCode() = 31 * id + payload.contentHashCode()
    }

    /**
     * Sent by the subprocess when a [Command] fails.
     *
     * @param id The [Command.id] being answered.
     * @param message Human-readable error description.
     */
    @Serializable
    @SerialName("error")
    data class Error(val id: Int, val message: String) : SubprocessMessage()

    /**
     * Sent by the subprocess to forward a line of text to the parent's stdout.
     *
     * @param text The text to display.
     */
    @Serializable
    @SerialName("stdout")
    data class Stdout(val text: String) : SubprocessMessage()

    /**
     * Sent by the parent to request a clean subprocess shutdown.
     */
    @Serializable
    @SerialName("quit")
    data object Quit : SubprocessMessage()

    /**
     * Sent by the subprocess to signal that initialization is complete and it is
     * ready to receive [Command] messages.
     */
    @Serializable
    @SerialName("ready")
    data object Ready : SubprocessMessage()

    /**
     * General-purpose bidirectional out-of-band message.
     *
     * @param id Message identifier assigned by the sender; callers may use this for
     *        their own correlation if needed.
     * @param payload Opaque message bytes.
     */
    @Serializable
    @SerialName("channel")
    data class Channel(val id: Int, val payload: ByteArray) : SubprocessMessage() {
        override fun equals(other: Any?) = other is Channel && id == other.id && payload.contentEquals(other.payload)
        override fun hashCode() = 31 * id + payload.contentHashCode()
    }

    /**
     * Sent by the subprocess when an internally registered timeout expires.
     *
     * The subprocess sends this message just before halting the JVM so the parent can
     * distinguish a timeout from an unexpected crash. The parent's reader thread converts
     * this into a [SubprocessResult.Timeout].
     *
     * @param timeoutId The numeric identifier of the expired timeout.
     */
    @Serializable
    @SerialName("timeout")
    data class Timeout(val timeoutId: Int) : SubprocessMessage()

    @OptIn(ExperimentalSerializationApi::class)
    companion object {
        private val cbor = Cbor { ignoreUnknownKeys = true }

        /**
         * Encodes [message] with CBOR, writes it length-prefixed to [out], and flushes.
         *
         * @param out The stream to write to.
         * @param message The message to encode and send.
         */
        fun write(out: DataOutputStream, message: SubprocessMessage) {
            val bytes = cbor.encodeToByteArray(serializer(), message)
            out.writeInt(bytes.size)
            out.write(bytes)
            out.flush()
        }

        /**
         * Reads a length-prefixed CBOR-encoded [SubprocessMessage] from [input].
         *
         * @param input The stream to read from.
         * @return The decoded message, or `null` on EOF.
         */
        fun read(input: DataInputStream): SubprocessMessage? {
            val length = try { input.readInt() } catch (_: EOFException) { return null }
            val bytes = ByteArray(length)
            input.readFully(bytes)
            return cbor.decodeFromByteArray(serializer(), bytes)
        }
    }
}
