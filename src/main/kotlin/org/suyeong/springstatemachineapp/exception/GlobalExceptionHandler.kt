package org.suyeong.springstatemachineapp.exception

import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

private val logger = KotlinLogging.logger {}

@RestControllerAdvice
class GlobalExceptionHandler {
    
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        logger.warn { "IllegalArgumentException: ${ex.message}" }
        
        val errorResponse = ErrorResponse(
            timestamp = Instant.now().toEpochMilli(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message ?: "Invalid argument",
            path = null
        )
        
        return ResponseEntity.badRequest().body(errorResponse)
    }
    
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(ex: IllegalStateException): ResponseEntity<ErrorResponse> {
        logger.warn { "IllegalStateException: ${ex.message}" }
        
        val errorResponse = ErrorResponse(
            timestamp = Instant.now().toEpochMilli(),
            status = HttpStatus.CONFLICT.value(),
            error = "Conflict",
            message = ex.message ?: "Invalid state",
            path = null
        )
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse)
    }
    
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ValidationErrorResponse> {
        logger.warn { "Validation error: ${ex.message}" }
        
        val fieldErrors = mutableMapOf<String, String>()
        ex.bindingResult.allErrors.forEach { error ->
            if (error is FieldError) {
                fieldErrors[error.field] = error.defaultMessage ?: "Invalid value"
            }
        }
        
        val errorResponse = ValidationErrorResponse(
            timestamp = Instant.now().toEpochMilli(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Validation Failed",
            message = "Request validation failed",
            fieldErrors = fieldErrors
        )
        
        return ResponseEntity.badRequest().body(errorResponse)
    }
    
    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(ex: RuntimeException): ResponseEntity<ErrorResponse> {
        logger.error(ex) { "RuntimeException: ${ex.message}" }
        
        val errorResponse = ErrorResponse(
            timestamp = Instant.now().toEpochMilli(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = ex.message ?: "An unexpected error occurred",
            path = null
        )
        
        return ResponseEntity.internalServerError().body(errorResponse)
    }
    
    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error(ex) { "Unexpected exception: ${ex.message}" }
        
        val errorResponse = ErrorResponse(
            timestamp = Instant.now().toEpochMilli(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "An unexpected error occurred",
            path = null
        )
        
        return ResponseEntity.internalServerError().body(errorResponse)
    }
}

data class ErrorResponse(
    val timestamp: Long,
    val status: Int,
    val error: String,
    val message: String,
    val path: String?
)

data class ValidationErrorResponse(
    val timestamp: Long,
    val status: Int,
    val error: String,
    val message: String,
    val fieldErrors: Map<String, String>
)