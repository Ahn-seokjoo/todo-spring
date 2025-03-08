package com.seokjoo.todo.common.exception

enum class TodoExceptionType(
    val message: String,
    val errorCode: String,
    val httpStatusCode: Int,
) {
    // TODO
    NOT_EXISTED_TODO(message = "존재하지 않는 Todo 입니다", errorCode = "T000_TODO_ERROR", httpStatusCode = 404)
}
