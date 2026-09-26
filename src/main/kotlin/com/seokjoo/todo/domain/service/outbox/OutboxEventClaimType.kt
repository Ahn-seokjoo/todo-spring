package com.seokjoo.todo.domain.service.outbox

import com.seokjoo.todo.domain.entity.outbox.OutboxEvent

sealed class OutboxEventClaimType
data class ClaimSuccess(val outboxEvent: OutboxEvent) : OutboxEventClaimType()
sealed class ClaimFailure : OutboxEventClaimType() {
    data object CheckDLQ : ClaimFailure()
    data object Processing : ClaimFailure()
}
