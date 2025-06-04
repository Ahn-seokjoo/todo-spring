package com.seokjoo.todo.common.redisson

import com.seokjoo.todo.common.exception.TodoException
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class RedisLockManager(
    private val redisson: RedissonClient,
) {
    fun <T> tryLock(key: String, block: () -> T): T {
        val lock = redisson.getLock(key)
        try {
            // 5초간 락 시도, 3초간 락을 유지
            if (lock.tryLock(5, 3, TimeUnit.SECONDS)) return block.invoke()
            else error("tryLock error")
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            error("InterruptedException")
        } catch (e: TodoException) {
            throw e
        } catch (e: Exception) {
            error("just exception")
        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
    }
}
