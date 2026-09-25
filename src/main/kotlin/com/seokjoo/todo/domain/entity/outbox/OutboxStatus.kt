package com.seokjoo.todo.domain.entity.outbox

enum class OutboxStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    DLQ;
}
