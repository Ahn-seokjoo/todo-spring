package com.seokjoo.todo.common.redisson

import com.seokjoo.todo.common.exception.TodoException
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class RedisLockManager(
    private val redisson: RedissonClient,
) {
    fun <T> tryLock(key: String, retryCount: Int = 3, block: () -> T): T {
        val lock = redisson.getLock(key)
        return run retryLoop@{
            repeat(retryCount) {
                try {
                    // 5초간 락 시도, 3초간 락을 유지
                    if (lock.tryLock(5, 3, TimeUnit.SECONDS)) {
                        return@retryLoop block.invoke()
                    } else {
                        Thread.sleep(50L)
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    error("InterruptedException")
                } catch (e: TodoException) {
                    throw e
                } catch (e: Exception) {
                    error("Just exception $key, ${e.message}")
                } finally {
                    if (lock.isLocked && lock.isHeldByCurrentThread) {
                        lock.unlock()
                    }
                }
            }
            error("TryLock fail after retry")
        }
    }
}
