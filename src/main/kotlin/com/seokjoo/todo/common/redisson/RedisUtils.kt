package com.seokjoo.todo.common.redisson

import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class RedisUtils(
    private val redisson: RedissonClient,
) {
    fun <T> tryLock(key: String, block: () -> T): T {
        val lock = redisson.getLock(key)
        try {
            // 5초간 락 시도, 3초간 락을 유지
            if (lock.tryLock(5, 3, TimeUnit.SECONDS)) return block.invoke()
            else throw IllegalStateException()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IllegalStateException()
        } catch (e: Exception) {
            throw IllegalStateException()
        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
    }
}
