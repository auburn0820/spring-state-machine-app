package org.suyeong.springstatemachineapp.common

/**
 * Result wrapper for operations that can succeed or fail.
 * Provides rich information about the outcome.
 */
sealed class Result<out T> {
    
    data class Success<out T>(val value: T) : Result<T>()
    
    data class Error(
        val exception: Throwable,
        val message: String = exception.message ?: "Unknown error"
    ) : Result<Nothing>()
}

/**
 * Extension functions for Result handling
 */
inline fun <T> Result<T>.onSuccess(action: (value: T) -> Unit): Result<T> {
    if (this is Result.Success) action(value)
    return this
}

inline fun <T> Result<T>.onError(action: (exception: Throwable) -> Unit): Result<T> {
    if (this is Result.Error) action(exception)
    return this
}