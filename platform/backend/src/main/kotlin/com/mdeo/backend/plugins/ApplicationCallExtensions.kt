package com.mdeo.backend.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*

/**
 * Extension function to respond with an ApiResult.
 * On success, responds with the value directly (with 200 OK).
 * On failure, responds with an object containing an error key (with 400 Bad Request).
 *
 * @param result The ApiResult to respond with
 */
suspend inline fun <reified T> ApplicationCall.respondApiResult(result: com.mdeo.common.model.ApiResult<T>) {
    when (result) {
        is com.mdeo.common.model.ApiResult.Success -> respondNullable(result.value)
        is com.mdeo.common.model.ApiResult.Failure -> respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to result.error)
        )
    }
}

/**
 * Extension function to respond with an ApiResult.
 * On success, responds with the value directly (with 200 OK).
 * On failure, responds with an object containing an error key (with 400 Bad Request).
 *
 * @param result The ApiResult to respond with
 */
@JvmName("respondApiResultNonNullable")
suspend inline fun <reified T : Any> ApplicationCall.respondApiResult(result: com.mdeo.common.model.ApiResult<T>) {
    when (result) {
        is com.mdeo.common.model.ApiResult.Success -> respond(result.value)
        is com.mdeo.common.model.ApiResult.Failure -> respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to result.error)
        )
    }
}