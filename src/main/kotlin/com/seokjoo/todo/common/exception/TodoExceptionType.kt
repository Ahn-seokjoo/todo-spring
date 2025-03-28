package com.seokjoo.todo.common.exception

enum class TodoExceptionType(
    val message: String,
    val errorCode: String,
    val httpStatusCode: Int,
) {
    // TODO
    NOT_EXISTED_TODO(message = "존재하지 않는 Todo 입니다", errorCode = "T000_TODO_ERROR", httpStatusCode = 404),
    ID_BAD_REQUEST(message = "ID 형식을 잘못 입력했습니다.", errorCode = "TO01_BAD_REQUEST", httpStatusCode = 400),
    ID_VALIDATION_BAD_REQUEST(
        message = "Todo는 빈 값일 수 없습니다",
        errorCode = "TO02_TODO_VALIDATION_BAD_REQUEST",
        httpStatusCode = 400
    ),

    // CATEGORY
    CATEGORY_NOT_EXIST(message = "존재하지 않은 Category 입니다", errorCode = "CAOOO_CATEGORY_NOT_EXIT", 404),
    CATEGORY_VALIDATION_BAD_REQUEST(
        message = "Category 이름은 빈 값일 수 없습니다",
        errorCode = "CA001_CATEGORY_VALIDATION_BAD_REQUEST",
        httpStatusCode = 400
    ),

    // COMMON
    COMMON_BAD_REQUEST(message = "잘못된 요청입니다", errorCode = "C000_BAD_REQUEST", httpStatusCode = 400),
    COMMON_VALIDATION_BAD_REQUEST(
        message = "잘못된 요청입니다. 필수 값(혹은 빈값)을 확인하세요",
        errorCode = "C001_VALIDATION_BAD_REQUEST",
        httpStatusCode = 400
    ),

    // AUTH
    AUTH_USER_NOT_EXIST(message = "회원이 아닙니다", errorCode = "A000_NOT_EXIST", httpStatusCode = 400),
    AUTH_NOT_MATCHED_PASSWORD(message = "비밀번호가 틀립니다", errorCode = "A001_NOT_MATCHED_PASSWORD", httpStatusCode = 400)
}
