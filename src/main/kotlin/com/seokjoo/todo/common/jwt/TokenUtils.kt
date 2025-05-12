package com.seokjoo.todo.common.jwt

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import jakarta.servlet.http.HttpServletRequest

fun HttpServletRequest.getBearerToken() = getHeader("Authorization")?.takeIf {
    it.startsWith("Bearer ")
}?.substring(7) ?: throw TodoException.of(TodoExceptionType.AUTH_REFRESH_TOKEN_BAD_REQUEST)
