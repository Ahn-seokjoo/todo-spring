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
    AUTH_NOT_MATCHED_PASSWORD(message = "비밀번호가 틀립니다", errorCode = "A001_NOT_MATCHED_PASSWORD", httpStatusCode = 400),
    AUTH_REFRESH_TOKEN_BAD_REQUEST(
        message = "토큰이 존재하지 않거나 형식이 잘못됐습니다",
        errorCode = "A002_TOKEN_BAD_REQUEST",
        httpStatusCode = 400
    ),
    AUTH_REFRESH_TOKEN_NOT_VALID(message = "유효하지 않은 토큰입니다.", errorCode = "A003_TOKEN_NOT_VALID", httpStatusCode = 401),
    AUTH_SIGN_UP_ERROR(message = "이미 존재하는 ID 입니다", errorCode = "A004_USER_ALREADY_EXIST", httpStatusCode = 400),

    // MONEY
    BALANCE_NOT_ENOUGH_MONEY(message = "잔액 부족입니다.", errorCode = "M000_BALANCE_NOT_ENOUGH_MONEY", httpStatusCode = 400),
    BALANCE_CAN_NOT_BE_NEGATIVE(
        message = "잔액은 음수일 수 없습니다.",
        errorCode = "M001_CANT_NOT_BE_NEGATIVE",
        httpStatusCode = 400
    ),
}
