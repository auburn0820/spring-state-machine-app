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

inline fun <T, R> Result<T>.map(transform: (value: T) -> R): Result<R> {
    return when (this) {
        is Result.Success -> Result.Success(transform(value))
        is Result.Error -> this
    }
}

inline fun <T, R> Result<T>.flatMap(transform: (value: T) -> Result<R>): Result<R> {
    return when (this) {
        is Result.Success -> transform(value)
        is Result.Error -> this
    }
}

fun <T> Result<T>.getOrThrow(): T {
    return when (this) {
        is Result.Success -> value
        is Result.Error -> throw exception
    }
}

fun <T> Result<T>.getOrNull(): T? {
    return when (this) {
        is Result.Success -> value
        is Result.Error -> null
    }
}

fun <T> Result<T>.getOrDefault(defaultValue: T): T {
    return when (this) {
        is Result.Success -> value
        is Result.Error -> defaultValue
    }
}

fun <T> Result<T>.isSuccess(): Boolean = this is Result.Success

fun <T> Result<T>.isError(): Boolean = this is Result.Error

/**
 * Helper functions to create Results
 */
fun <T> T.toSuccess(): Result<T> = Result.Success(this)

fun Throwable.toError(): Result<Nothing> = Result.Error(this)

fun String.toError(): Result<Nothing> = Result.Error(RuntimeException(this))

/**
 * Try to execute a block and return Result
 */
inline fun <T> resultOf(block: () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: Exception) {
        Result.Error(e)
    }
}

/**
 * Try to execute a suspend block and return Result
 */
suspend inline fun <T> suspendResultOf(crossinline block: suspend () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: Exception) {
        Result.Error(e)
    }
}