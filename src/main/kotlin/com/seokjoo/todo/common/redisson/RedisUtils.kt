package com.seokjoo.todo.common.redisson

import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class RedisUtils(
    private val redisson: RedissonClient,
) {
    fun tryLock(key: String, block: () -> Unit) {
        val lock = redisson.getLock(key)
        try {
            if (lock.tryLock(5, 3, TimeUnit.SECONDS)) block.invoke()
        } finally {
            lock.unlock()
        }
    }
}
