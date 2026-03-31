package com.mdeo.optimizerexecution.service

import com.mdeo.execution.common.api.BackendApiClient
import com.mdeo.metamodel.data.MetamodelData
import com.mdeo.modeltransformation.ast.TypedAst as TransformationTypedAst
import com.mdeo.metamodel.data.ModelData
import com.mdeo.modeltransformation.ast.statements.TypedTransformationStatement
import com.mdeo.modeltransformation.ast.statements.TypedTransformationStatementSerializer
import com.mdeo.modeltransformation.ast.patterns.TypedPatternElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternElementSerializer
import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.modeltransformation.ast.expressions.TypedExpressionSerializer as TransformationExpressionSerializer
import com.mdeo.script.ast.TypedAst as ScriptTypedAst
import com.mdeo.script.ast.expressions.TypedExpressionSerializer as ScriptExpressionSerializer
import com.mdeo.expression.ast.statements.TypedStatement
import com.mdeo.script.ast.statements.TypedStatementSerializer
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

/**
 * API client for fetching all data the optimizer needs from the backend.
 *
 * Uses two internal HTTP clients: one configured with transformation AST
 * serializers (for model-transformation typed ASTs) and one with script AST
 * serializers (for script typed ASTs). Model/metamodel data use the parent
 * client which needs no special serializers.
 *
 * @param baseUrl Base URL of the backend API
 */
class OptimizerApiClient(baseUrl: String) : BackendApiClient(baseUrl) {

    private val transformationClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                serializersModule = SerializersModule {
                    contextual(TypedExpression::class, TransformationExpressionSerializer)
                    contextual(TypedTransformationStatement::class, TypedTransformationStatementSerializer)
                    contextual(TypedPatternElement::class, TypedPatternElementSerializer)
                }
            })
        }
    }

    private val scriptClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                serializersModule = SerializersModule {
                    contextual(TypedExpression::class, ScriptExpressionSerializer)
                    contextual(TypedStatement::class, TypedStatementSerializer)
                }
            })
        }
    }

    /**
     * Fetches the typed AST for a model transformation file.
     */
    suspend fun getTransformationTypedAst(
        projectId: String, filePath: String, jwtToken: String
    ): TransformationTypedAst? {
        return try {
            logger.info("Fetching transformation typed AST for $filePath")
            val response = transformationClient.get("$baseUrl/projects/$projectId/file-data/typed-ast") {
                parameter("path", filePath)
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $jwtToken")
            }
            if (response.status == HttpStatusCode.OK) {
                response.body<TransformationTypedAstResponse>().data
            } else {
                logger.warn("Failed to fetch transformation typed AST for $filePath: ${response.status}")
                null
            }
        } catch (e: Exception) {
            logger.error("Error fetching transformation typed AST for $filePath", e)
            null
        }
    }

    /**
     * Fetches the typed AST for a script file.
     */
    suspend fun getScriptTypedAst(
        projectId: String, filePath: String, jwtToken: String
    ): ScriptTypedAst? {
        return try {
            logger.info("Fetching script typed AST for $filePath")
            val response = scriptClient.get("$baseUrl/projects/$projectId/file-data/typed-ast") {
                parameter("path", filePath)
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $jwtToken")
            }
            if (response.status == HttpStatusCode.OK) {
                response.body<ScriptTypedAstResponse>().data
            } else {
                logger.warn("Failed to fetch script typed AST for $filePath: ${response.status}")
                null
            }
        } catch (e: Exception) {
            logger.error("Error fetching script typed AST for $filePath", e)
            null
        }
    }

    /**
     * Fetches model data for a model file.
     */
    suspend fun getModelData(
        projectId: String, filePath: String, jwtToken: String
    ): ModelData? {
        return try {
            logger.info("Fetching model data for $filePath")
            val response = client.get("$baseUrl/projects/$projectId/file-data/model-data") {
                parameter("path", filePath)
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $jwtToken")
            }
            if (response.status == HttpStatusCode.OK) {
                response.body<ModelDataResponse>().data
            } else {
                logger.warn("Failed to fetch model data for $filePath: ${response.status}")
                null
            }
        } catch (e: Exception) {
            logger.error("Error fetching model data", e)
            null
        }
    }

    /**
     * Fetches metamodel data for a metamodel file.
     */
    suspend fun getMetamodelData(
        projectId: String, filePath: String, jwtToken: String
    ): MetamodelData? {
        return try {
            logger.info("Fetching metamodel data for $filePath")
            val response = client.get("$baseUrl/projects/$projectId/file-data/metamodel") {
                parameter("path", filePath)
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $jwtToken")
            }
            if (response.status == HttpStatusCode.OK) {
                response.body<MetamodelDataResponse>().data
            } else {
                logger.warn("Failed to fetch metamodel data for $filePath: ${response.status}")
                null
            }
        } catch (e: Exception) {
            logger.error("Error fetching metamodel data", e)
            null
        }
    }

    /**
     * Closes all HTTP clients including the extra ones for transformations and scripts.
     */
    fun closeAll() {
        close()
        transformationClient.close()
        scriptClient.close()
    }
}

@Serializable
internal data class TransformationTypedAstResponse(
    val data: TransformationTypedAst?,
    val version: Int? = null
)

@Serializable
internal data class ScriptTypedAstResponse(
    val data: ScriptTypedAst?,
    val version: Int? = null
)

@Serializable
internal data class ModelDataResponse(
    val data: ModelData?,
    val version: Int? = null
)

@Serializable
internal data class MetamodelDataResponse(
    val data: MetamodelData?,
    val version: Int? = null
)
