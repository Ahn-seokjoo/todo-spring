package com.seokjoo.todo.domain.entity.outbox

enum class OutboxEventType {
    PURCHASE_APPROVED,
    PURCHASE_REJECTED,
    CACHE_EVICT_ALL,
    CACHE_EVICT_SINGLE,
}

sealed interface PublishableEvent {
    val eventType: OutboxEventType

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

    data object CacheEvictAllEvent : PublishableEvent {
        override val eventType: OutboxEventType = OutboxEventType.CACHE_EVICT_ALL
    }

    data class CacheEvictSingleEvent(
        val todoId: Long,
    ) : PublishableEvent {
        override val eventType: OutboxEventType = OutboxEventType.CACHE_EVICT_SINGLE
    }
}
