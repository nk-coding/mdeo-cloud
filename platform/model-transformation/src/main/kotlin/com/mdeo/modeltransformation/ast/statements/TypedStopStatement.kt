package com.mdeo.modeltransformation.ast.statements

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

/**
 * Stop statement for terminating transformation execution.
 *
 * Stops the transformation execution, optionally with different semantics
 * based on the keyword used.
 *
 * @param kind Always "stop" for this statement type.
 * @param keyword The keyword used: "stop" for normal termination or "kill" for
 *                immediate termination. The semantic difference may affect
 *                cleanup or finalization behavior.
 */
@Serializable
data class TypedStopStatement(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val kind: String = "stop",
    val keyword: String
) : TypedTransformationStatement
