package com.seokjoo.todo.common.exception

enum class TodoExceptionType(
    val message: String,
    val errorCode: String,
    val httpStatusCode: Int,
) {
    // TODO
    NOT_EXISTED_TODO(message = "존재하지 않는 Todo 입니다", errorCode = "T000_TODO_ERROR", httpStatusCode = 404),
    ID_BAD_REQUEST(message = "ID 형식을 잘못 입력했습니다.", errorCode = "TO01_BAD_REQUEST", httpStatusCode = 400),
    ID_VALIDATION_BAD_REQUEST(message = "Todo는 빈 값일 수 없습니다", errorCode = "TO02_BAD_REQUEST", httpStatusCode = 400),

    // CATEGORY
    CATEGORY_NOT_EXIST(message = "존재하지 않은 Category 입니다", errorCode = "CAOOO_CATEGORY_NOT_EXIT", 404),

    // COMMON
    COMMON_BAD_REQUEST(message = "잘못된 요청입니다", errorCode = "C000_BAD_REQUEST", httpStatusCode = 400);
}
