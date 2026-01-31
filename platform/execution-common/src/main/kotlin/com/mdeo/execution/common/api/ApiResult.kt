package com.mdeo.execution.common.api

/**
 * Result wrapper for API operations.
 * Provides a type-safe way to handle success and failure cases.
 *
 * @param T The type of data on success
 */
sealed class ApiResult<out T> {
    /**
     * Successful API result with data.
     */
    data class Success<T>(val data: T) : ApiResult<T>()

    /**
     * Failed API result with error details.
     */
    data class Error(val message: String, val statusCode: Int? = null) : ApiResult<Nothing>()

    /**
     * Returns the data if successful, null otherwise.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    /**
     * Returns the data if successful, throws exception otherwise.
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw ApiException(message, statusCode)
    }

    /**
     * Maps the success value to a new type.
     */
    inline fun <R> map(transform: (T) -> R): ApiResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    /**
     * Executes the given block if successful.
     */
    inline fun onSuccess(action: (T) -> Unit): ApiResult<T> {
        if (this is Success) action(data)
        return this
    }

    /**
     * Executes the given block if failed.
     */
    inline fun onError(action: (message: String, statusCode: Int?) -> Unit): ApiResult<T> {
        if (this is Error) action(message, statusCode)
        return this
    }
}

/**
 * Exception thrown when an API operation fails.
 */
class ApiException(
    message: String,
    val statusCode: Int? = null
) : Exception(message)
