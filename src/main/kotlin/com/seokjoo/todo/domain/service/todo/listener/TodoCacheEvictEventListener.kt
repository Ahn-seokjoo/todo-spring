package com.seokjoo.todo.domain.service.todo.listener

import com.seokjoo.todo.common.annotation.TodoCacheEventListener
import com.seokjoo.todo.domain.service.todo.event.TodoCacheEvictEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component

@Component
class TodoCacheEvictEventListener(
    @param:Qualifier("todoCacheManager") private val cacheManager: CacheManager,
) {
    private val logger = LoggerFactory.getLogger(TodoCacheEvictEventListener::class.java)

    @TodoCacheEventListener
    fun eventListener(event: TodoCacheEvictEvent) {
        if (event.isEvictAll) {
            runCatching { cacheManager.getCache("todos")?.clear() }
                .onFailure { logger.warn("todos 캐시 삭제 실패", it) }
        }
        event.todoIds.forEach { todoId ->
            runCatching { cacheManager.getCache("todo")?.evict("$todoId") }
                .onFailure { logger.warn("todo 캐시 삭제 실패 id = $todoId", it) }
        }
    }
}
