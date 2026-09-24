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
    UNAUTHORIZED_TODO_ACCESS(
        message = "자신의 Todo만 접근할 수 있습니다.",
        errorCode = "T003_GET_TODO_AUTHORIZATION",
        httpStatusCode = 401
    ),

    // CATEGORY
    CATEGORY_NOT_EXIST(message = "존재하지 않은 Category 입니다", errorCode = "CAOOO_CATEGORY_NOT_EXIST", 404),
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
    AUTH_INVALID_TOKEN_TYPE(message = "잘못된 토큰 타입입니다", errorCode = "A005_TOKEN_INVALID_TYPE", httpStatusCode = 401),
    AUTH_FORBIDDEN_USER_ACCESS(
        message = "본인 계좌에만 접근할 수 있습니다.",
        errorCode = "A006_FORBIDDEN_USER_ACCESS",
        httpStatusCode = 403
    ),

    // MONEY
    BALANCE_NOT_ENOUGH(message = "잔액 부족입니다.", errorCode = "M000_BALANCE_NOT_ENOUGH", httpStatusCode = 400),
    BALANCE_CAN_NOT_BE_NEGATIVE(
        message = "잔액은 음수일 수 없습니다.",
        errorCode = "M001_CAN_NOT_BE_NEGATIVE",
        httpStatusCode = 400
    ),

    // LOCK
    LOCK_GET_FAILED(
        message = "LOCK 획득에 실패했습니다.",
        errorCode = "L001_LOCK_GET_FAILED",
        httpStatusCode = 500
    ),

    // TRADE
    CAN_NOT_TRADE_OWN_TODO(
        message = "자신의 Todo를 팔 수 없습니다.",
        errorCode = "TR000_CAN_NOT_TRADE_TODO",
        httpStatusCode = 400
    ),
    CAN_NOT_PURCHASE(
        message = "이미 구매 대기중인 Todo 입니다.",
        errorCode = "TR001_CAN_NOT_PURCHASE",
        httpStatusCode = 400
    ),
    CAN_NOT_FOUND_PURCHASE(
        message = "주문 내역이 없습니다.",
        errorCode = "TR002_CAN_NOT_FOUND_PURCHASE",
        httpStatusCode = 400,
    ),
    NOT_PENDING(
        message = "판매중인 Todo가 아닙니다.",
        errorCode = "TR003_NOT_PENDING",
        httpStatusCode = 400,
    ),
    CAN_NOT_CANCEL_OWN_TODO(
        message = "자신의 Todo 판매를 취소할 수 없습니다.",
        errorCode = "TR004_CAN_NOT_CANCEL_OWN_TODO",
        httpStatusCode = 400
    ),
    PENDING(
        message = "판매중인 Todo로 수정할 수 없습니다.",
        errorCode = "TR005_PENDING",
        httpStatusCode = 400,
    ),
    YOU_ARE_NOT_BUYER(
        message = "요청자만 취소할 수 있습니다",
        errorCode = "TR006_YOU_ARE_NOT_BUYER",
        httpStatusCode = 403,
    ),

    // Outbox
    OUTBOX_UPDATE_ERROR(
        message = "outbox table 상태 업데이트 실패",
        errorCode = "OB000_OUTBOX_UPDATE_ERROR",
        httpStatusCode = 500,
    ),
    OUTBOX_NOT_FOUND(
        message = "outbox not found",
        errorCode = "OB001_NOT_FOUND",
        httpStatusCode = 500,
    )
}
