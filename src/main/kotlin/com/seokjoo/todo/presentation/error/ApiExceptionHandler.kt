package com.seokjoo.todo.presentation.error

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
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

        val fe = exception.bindingResult.fieldErrors.firstOrNull()

        val error = when (fe?.field) {
            "todo" -> TodoExceptionType.ID_VALIDATION_BAD_REQUEST
            "name" -> TodoExceptionType.CATEGORY_VALIDATION_BAD_REQUEST
            "price" -> when (fe.code) {
                "Min" -> TodoExceptionType.BALANCE_CAN_NOT_BE_NEGATIVE
                else -> TodoExceptionType.COMMON_VALIDATION_BAD_REQUEST
            }

            "isDone" -> TodoExceptionType.COMMON_VALIDATION_BAD_REQUEST
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

    @ExceptionHandler
    fun handleUnexpectedException(exception: Exception): ResponseEntity<ApiErrorResponse> {
        logger.error("Unexpected error", exception)
        return ResponseEntity
            .status(500)
            .body(
                ApiErrorResponse(
                    errorCode = "S000_INTERNAL_ERROR",
                    message = "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                )
            )
    }
}
