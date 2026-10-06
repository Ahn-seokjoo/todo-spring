package com.seokjoo.todo.domain.service.outbox

enum class OutboxEventType {
    PURCHASE_REQUEST,
    PURCHASE_APPROVED,
    PURCHASE_REJECTED,
}

sealed interface PublishableEvent {
    val eventType: OutboxEventType

    data class PurchaseRequestEvent(
        val sellerId: String,
        val buyerId: String,
        val todoId: Long,
        val price: Long,
        val purchaseId: Long,
    ) : PublishableEvent {
        override val eventType: OutboxEventType = OutboxEventType.PURCHASE_REQUEST
    }

    data class PurchaseApprovedEvent(
        val sellerId: String,
        val buyerId: String,
        val todoId: Long,
        val price: Long,
        val purchaseId: Long,
    ) : PublishableEvent {
        override val eventType: OutboxEventType = OutboxEventType.PURCHASE_APPROVED
    }

    data class PurchaseRejectedEvent(
        val sellerId: String,
        val buyerId: String,
        val todoId: Long,
        val price: Long,
        val purchaseId: Long,
    ) : PublishableEvent {
        override val eventType: OutboxEventType = OutboxEventType.PURCHASE_REJECTED
    }
}
