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

        val error = when (exception.bindingResult.fieldErrors.firstOrNull()?.field) {
            "todo" -> TodoExceptionType.ID_VALIDATION_BAD_REQUEST
            "name" -> TodoExceptionType.CATEGORY_VALIDATION_BAD_REQUEST
            else -> TodoExceptionType.COMMON_VALIDATION_BAD_REQUEST
        }

        return ResponseEntity
            .status(error.httpStatusCode)
            .body(
                ApiErrorResponse(
                    errorCode = error.errorCode,
                    message = error.message,
                )
            )
    }

    @ExceptionHandler
    fun handlerTypeMismatchException(
        request: HttpServletRequest,
        exception: MethodArgumentTypeMismatchException,
    ): ResponseEntity<ApiErrorResponse> {
        logger.info(exception.message, exception)
        val error = when (exception.parameter.parameterName) {
            "id" -> TodoExceptionType.ID_BAD_REQUEST
            else -> TodoExceptionType.COMMON_BAD_REQUEST
        }

        return ResponseEntity
            .status(error.httpStatusCode)
            .body(
                ApiErrorResponse(
                    errorCode = error.errorCode,
                    message = error.message,
                )
            )
    }
}
