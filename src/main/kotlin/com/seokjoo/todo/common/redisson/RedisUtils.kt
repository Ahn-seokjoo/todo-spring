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
            // 5초간 락 시도, 3초간 락을 유지
            if (lock.tryLock(5, 3, TimeUnit.SECONDS)) block.invoke()
            else throw IllegalStateException()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (e: Exception) {
            // 처리 고민 ,,
        } finally {
            if (lock.isHeldByCurrentThread) { lock.unlock() }
        }
    }
}
