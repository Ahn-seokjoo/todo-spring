package com.seokjoo.todo.presentation.error

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

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

    @ExceptionHandler
    fun handleException(exception: MethodArgumentNotValidException): ResponseEntity<ApiErrorResponse> {
        logger.info(exception.message, exception)

        val message = exception.bindingResult.fieldErrors.getOrNull(0)?.defaultMessage

        return ResponseEntity
            .status(TodoExceptionType.ID_VALIDATION_BAD_REQUEST.httpStatusCode)
            .body(
                ApiErrorResponse(
                    errorCode = TodoExceptionType.ID_VALIDATION_BAD_REQUEST.errorCode,
                    message = TodoExceptionType.ID_VALIDATION_BAD_REQUEST.message,
                )
            )
    }

    @ExceptionHandler
    fun handlerTypeMismatchException(
        request: HttpServletRequest,
        exception: MethodArgumentTypeMismatchException,
    ): ResponseEntity<ApiErrorResponse> {
        logger.info(exception.message, exception)

        if (request.requestURI.contains("/api/v1/todo")) {
            TodoExceptionType.ID_BAD_REQUEST
        } else {
            TodoExceptionType.COMMON_BAD_REQUEST
        }.also {
            return ResponseEntity
                .status(it.httpStatusCode)
                .body(
                    ApiErrorResponse(
                        errorCode = it.errorCode,
                        message = it.message,
                    )
                )
        }
    }
}
