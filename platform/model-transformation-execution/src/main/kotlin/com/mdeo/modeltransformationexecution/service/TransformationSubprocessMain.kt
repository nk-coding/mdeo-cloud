package com.mdeo.modeltransformationexecution.service

import com.mdeo.execution.common.subprocess.SubprocessMain
import com.mdeo.metamodel.Metamodel
import com.mdeo.metamodel.data.MetamodelData
import com.mdeo.metamodel.data.ModelData
import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.expressions.TypedExpressionSerializer
import com.mdeo.modeltransformation.ast.patterns.TypedPatternElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternElementSerializer
import com.mdeo.modeltransformation.ast.statements.TypedTransformationStatement
import com.mdeo.modeltransformation.ast.statements.TypedTransformationStatementSerializer
import com.mdeo.modeltransformation.graph.MdeoModelGraph
import com.mdeo.modeltransformation.runtime.TransformationEngine
import com.mdeo.modeltransformation.runtime.TransformationExecutionResult
import com.mdeo.expression.ast.expressions.TypedExpression
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

/**
 * Subprocess entry point for executing model transformations in an isolated JVM process.
 *
 * Receives a JSON-serialized [TransformationInput] as the command payload, executes the
 * transformation engine, and returns the resulting [ModelData] as JSON bytes.
 */
class TransformationSubprocessMain : SubprocessMain() {

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            serializersModule = SerializersModule {
                contextual(TypedExpression::class, TypedExpressionSerializer)
                contextual(TypedTransformationStatement::class, TypedTransformationStatementSerializer)
                contextual(TypedPatternElement::class, TypedPatternElementSerializer)
            }
        }

        /**
         * Serializes the transformation execute command into a byte array for subprocess communication.
         *
         * @param typedAst The transformation typed AST.
         * @param metamodelData The metamodel data.
         * @param modelData The input model data.
         * @param timeoutMs Execution timeout in milliseconds (0 = no timeout).
         * @return JSON-encoded byte array.
         */
        fun serializeInput(typedAst: TypedAst, metamodelData: MetamodelData, modelData: ModelData, timeoutMs: Long = 0L): ByteArray {
            val cmd = TransformationCommand.Execute(typedAst, metamodelData, modelData, timeoutMs)
            return json.encodeToString<TransformationCommand>(cmd).toByteArray()
        }

        @JvmStatic
        fun main(args: Array<String>) {
            TransformationSubprocessMain().run(args)
        }
    }

    override fun handleCommand(payload: ByteArray): ByteArray {
        return when (val cmd = json.decodeFromString<TransformationCommand>(payload.decodeToString())) {
            is TransformationCommand.Execute -> handleExecute(cmd)
            TransformationCommand.Reset ->
                json.encodeToString<TransformationResponse>(TransformationResponse.ResetOk).toByteArray()
        }
    }

    private fun handleExecute(cmd: TransformationCommand.Execute): ByteArray {
        val timeoutId = 0
        if (cmd.timeoutMs > 0) registerTimeout(timeoutId, cmd.timeoutMs)
        val metamodel = Metamodel.compile(cmd.metamodelData)
        val modelGraph = MdeoModelGraph.create(cmd.modelData, metamodel)

        return try {
            val engine = TransformationEngine.create(
                modelGraph, cmd.typedAst, deterministic = false
            )
            val result = engine.execute()
            when (result) {
                is TransformationExecutionResult.Success -> {
                    json.encodeToString<TransformationResponse>(
                        TransformationResponse.ExecuteOk(modelGraph.toModelData())
                    ).toByteArray()
                }
                is TransformationExecutionResult.Failure -> {
                    throw RuntimeException("Transformation failed: ${result.reason}")
                }
                is TransformationExecutionResult.Stopped -> {
                    if (result.isNormalStop) {
                        json.encodeToString<TransformationResponse>(
                            TransformationResponse.ExecuteOk(modelGraph.toModelData())
                        ).toByteArray()
                    } else {
                        throw RuntimeException("Transformation killed explicitly")
                    }
                }
            }
        } finally {
            cancelTimeout(timeoutId)
            modelGraph.close()
        }
    }
}

/**
 * Command sent from the parent process to the transformation subprocess.
 */
@Serializable
sealed class TransformationCommand {
    /**
     * Execute a model transformation. Carries all compilation inputs so each execution
     * is self-contained (no persistent compiled state between executions).
     */
    @Serializable
    @SerialName("execute")
    data class Execute(
        val typedAst: TypedAst,
        val metamodelData: MetamodelData,
        val modelData: ModelData,
        val timeoutMs: Long = 0L
    ) : TransformationCommand()

    /**
     * Signals the subprocess to confirm it is alive and ready for the next execution.
     * Since the transformation subprocess is stateless, this is a lightweight heartbeat
     * / pool health check rather than an actual state-clearing operation.
     */
    @Serializable
    @SerialName("reset")
    data object Reset : TransformationCommand()
}

/**
 * Response produced by the transformation subprocess for a given [TransformationCommand].
 */
@Serializable
sealed class TransformationResponse {
    /** Successful transformation result. */
    @Serializable
    @SerialName("execute_ok")
    data class ExecuteOk(val modelData: ModelData) : TransformationResponse()

    /** Acknowledgement of a [TransformationCommand.Reset]. */
    @Serializable
    @SerialName("reset_ok")
    data object ResetOk : TransformationResponse()
}
