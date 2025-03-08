package com.seokjoo.todo.common.exception

class TodoException(
    val errorCode: String,
    val httpStatusCode: Int,
    override val message: String,
) : RuntimeException() {
    companion object {
        fun of(type: TodoExceptionType) = TodoException(
            errorCode = type.errorCode,
            httpStatusCode = type.httpStatusCode,
            message = type.message,
        )

        fun of(type: TodoExceptionType, message: String) = TodoException(
            errorCode = type.errorCode,
            httpStatusCode = type.httpStatusCode,
            message = message,
        )
    }
}
