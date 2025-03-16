package com.seokjoo.todo.presentation.error

data class ApiErrorResponse(
    val errorCode: String,
    val message: String,
)
