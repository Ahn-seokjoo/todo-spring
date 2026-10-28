package com.seokjoo.todo.domain.service.todo.listener

import com.seokjoo.todo.common.annotation.TodoCacheEventListener
import com.seokjoo.todo.domain.service.todo.event.TodoCacheEvictEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.data.redis.core.ScanOptions
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
class TodoCacheEvictEventListener(
    @param:Qualifier("todoCacheManager") private val cacheManager: CacheManager,
    private val redisTemplate: StringRedisTemplate,
) {
    private val logger = LoggerFactory.getLogger(TodoCacheEvictEventListener::class.java)

    @TodoCacheEventListener
    fun eventListener(event: TodoCacheEvictEvent) {
        event.userIds.forEach { userId ->
            runCatching {
                val pattern = "todos::${escapeGlob(userId)}:*"
                val options = ScanOptions.scanOptions().match(pattern).count(100).build()
                val keys = redisTemplate.scan(options).use { cursor ->
                    cursor.asSequence().toSet()
                }
                if (keys.isNotEmpty()) {
                    redisTemplate.unlink(keys)
                }
            }.onFailure { logger.warn("todos 캐시 삭제 실패 userId = $userId", it) }
        }
        event.todoIds.forEach { todoId ->
            runCatching { cacheManager.getCache("todo")?.evict("$todoId") }
                .onFailure { logger.warn("todo 캐시 삭제 실패 id = $todoId", it) }
        }
    }

    private fun escapeGlob(value: String): String =
        value.replace("\\", "\\\\")   // \ 를 제일 먼저
            .replace("*", "\\*")
            .replace("?", "\\?")
            .replace("[", "\\[")
            .replace("]", "\\]")
}
