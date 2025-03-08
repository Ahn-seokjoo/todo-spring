package com.seokjoo.todo.presentation.error

import com.seokjoo.todo.common.exception.TodoException
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {
    private val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

    @ExceptionHandler
    fun handlerTodoException(exception: TodoException): ResponseEntity<ApiErrorResponse> {
        logger.info(exception.message, exception)
        
        return ResponseEntity
            .status(exception.httpStatusCode)
            .body(
                ApiErrorResponse(
                    errorCode = exception.errorCode,
                    message = exception.message,
                )
            )
    }
}
