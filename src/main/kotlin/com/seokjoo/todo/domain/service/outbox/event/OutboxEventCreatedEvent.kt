package com.seokjoo.todo.domain.service.outbox.event

// 아웃박스 행이 저장됐다는 신호. 커밋 후 리스너가 받아 즉시 발행을 시도한다 (실패하면 폴러가 이어받음).
data class OutboxEventCreatedEvent(
    val outboxEventId: String,
)
