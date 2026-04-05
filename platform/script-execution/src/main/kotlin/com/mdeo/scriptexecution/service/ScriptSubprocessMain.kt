package com.mdeo.scriptexecution.service

import com.mdeo.execution.common.subprocess.SubprocessMain
import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.statements.TypedStatement
import com.mdeo.metamodel.data.MetamodelData
import com.mdeo.metamodel.data.ModelData
import com.mdeo.script.ast.TypedAst
import com.mdeo.script.ast.expressions.TypedExpressionSerializer
import com.mdeo.script.ast.statements.TypedStatementSerializer
import com.mdeo.script.compiler.CompilationInput
import com.mdeo.script.compiler.ScriptCompiler
import com.mdeo.script.runtime.ExecutionEnvironment
import com.mdeo.script.runtime.SimpleScriptContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/**
 * Subprocess entry point for executing scripts in an isolated JVM process.
 *
 * Receives a JSON-serialized [ScriptCommand] as the command payload and handles two
 * command types: [ScriptCommand.Execute] (compiles the scripts and runs the target method,
 * returning a [ScriptResponse.ExecuteOk]) and [ScriptCommand.Reset] (confirms readiness
 * for the next execution, returning [ScriptResponse.ResetOk]).
 */
class ScriptSubprocessMain : SubprocessMain() {

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            serializersModule = SerializersModule {
                contextual(TypedExpression::class, TypedExpressionSerializer)
                contextual(TypedStatement::class, TypedStatementSerializer)
            }
        }

        /**
         * Serializes the script execute command into a byte array for subprocess communication.
         *
         * @param typedAsts All resolved typed ASTs keyed by file path.
         * @param metamodelData Optional metamodel data.
         * @param modelData Optional model data.
         * @param filePath Path of the main file.
         * @param methodName Method to invoke.
         * @param timeoutMs Execution timeout in milliseconds (0 = no timeout).
         * @return JSON-encoded byte array.
         */
        fun serializeInput(
            typedAsts: Map<String, TypedAst>,
            metamodelData: MetamodelData?,
            modelData: ModelData?,
            filePath: String,
            methodName: String,
            timeoutMs: Long = 0L
        ): ByteArray {
            val cmd = ScriptCommand.Execute(typedAsts, metamodelData, modelData, filePath, methodName, timeoutMs)
            return json.encodeToString<ScriptCommand>(cmd).toByteArray()
        }

        @JvmStatic
        fun main(args: Array<String>) {
            ScriptSubprocessMain().run(args)
        }
    }

    override fun handleCommand(payload: ByteArray): ByteArray {
        return when (val cmd = json.decodeFromString<ScriptCommand>(payload.decodeToString())) {
            is ScriptCommand.Execute -> handleExecute(cmd)
            ScriptCommand.Reset -> json.encodeToString<ScriptResponse>(ScriptResponse.ResetOk).toByteArray()
        }
    }

    private fun handleExecute(cmd: ScriptCommand.Execute): ByteArray {
        val timeoutId = 0
        val outputStream = ByteArrayOutputStream()
        val printStream = PrintStream(outputStream, true, Charsets.UTF_8)

        if (cmd.timeoutMs > 0) registerTimeout(timeoutId, cmd.timeoutMs)
        try {
            val compiledProgram = ScriptCompiler().compile(
                CompilationInput(cmd.typedAsts),
                cmd.metamodelData
            )

            val env = ExecutionEnvironment(compiledProgram)

            val model = if (cmd.modelData != null && compiledProgram.metamodel != null) {
                compiledProgram.metamodel!!.loadModel(cmd.modelData)
            } else {
                null
            }

            val context = SimpleScriptContext(printStream, model)
            val result = env.invoke(cmd.filePath, cmd.methodName, context)

            return json.encodeToString<ScriptResponse>(
                ScriptResponse.ExecuteOk(
                    result = result?.toString(),
                    output = outputStream.toString(Charsets.UTF_8)
                )
            ).toByteArray()
        } catch (t: Throwable) {
            val stackTraceBuf = ByteArrayOutputStream()
            t.printStackTrace(PrintStream(stackTraceBuf, true, Charsets.UTF_8))
            return json.encodeToString<ScriptResponse>(
                ScriptResponse.ExecuteError(
                    message = t.toString(),
                    stackTrace = stackTraceBuf.toString(Charsets.UTF_8),
                    output = outputStream.toString(Charsets.UTF_8)
                )
            ).toByteArray()
        } finally {
            cancelTimeout(timeoutId)
            printStream.close()
        }
    }
}

/**
 * Command sent from the parent process to the script subprocess.
 */
@Serializable
sealed class ScriptCommand {
    /**
     * Execute a script method in the subprocess. Carries all compilation inputs so each
     * execution is self-contained (no persistent compiled state between executions).
     */
    @Serializable
    @SerialName("execute")
    data class Execute(
        val typedAsts: Map<String, TypedAst>,
        val metamodelData: MetamodelData?,
        val modelData: ModelData?,
        val filePath: String,
        val methodName: String,
        val timeoutMs: Long = 0L
    ) : ScriptCommand()

    /**
     * Signals the subprocess to confirm it is alive and ready for the next execution.
     * Since the script subprocess is stateless, this is a lightweight heartbeat / pool
     * health check rather than an actual state-clearing operation.
     */
    @Serializable
    @SerialName("reset")
    data object Reset : ScriptCommand()
}

/**
 * Response produced by the script subprocess for a given [ScriptCommand].
 */
@Serializable
sealed class ScriptResponse {
    /**
     * Successful execution result. 
     */
    @Serializable
    @SerialName("execute_ok")
    data class ExecuteOk(
        val result: String?,
        val output: String?
    ) : ScriptResponse()

    /**
     * Runtime error during script execution (e.g. [StackOverflowError], [OutOfMemoryError]).
     * Carries the throwable's string representation, its full stack trace, and any output
     * that was captured before the error occurred.
     */
    @Serializable
    @SerialName("execute_error")
    data class ExecuteError(
        val message: String,
        val stackTrace: String?,
        val output: String?
    ) : ScriptResponse()

    /**
     * Acknowledgement of a [ScriptCommand.Reset]. 
     */
    @Serializable
    @SerialName("reset_ok")
    data object ResetOk : ScriptResponse()
}

/**
 * Deprecated alias kept for callers that deserialise execution results directly.
 * Prefer [ScriptResponse.ExecuteOk].
 */
@Serializable
data class ScriptOutput(
    val result: String?,
    val output: String?
)
