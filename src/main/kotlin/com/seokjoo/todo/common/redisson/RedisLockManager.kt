package com.seokjoo.todo.common.redisson

import com.seokjoo.todo.common.exception.LockNotAcquiredException
import org.redisson.api.RedissonClient
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class RedisLockManager(
    private val redisson: RedissonClient,
    private val retryListener: LoggingRetryListener,
) : LockManager {

    override fun <T> tryLock(
        key: String,
        retryCount: Int,
        delay: Long,
        maxDelay: Long,
        block: () -> T,
    ): T {
        val retryTemplate = RetryTemplate.builder()
            .maxAttempts(retryCount)
            .exponentialBackoff(delay, 2.0, maxDelay, true)
            .withListener(retryListener)
            .retryOn { t ->
                t is ObjectOptimisticLockingFailureException || t is LockNotAcquiredException
            }.build()

        return retryTemplate.execute<T, Throwable> {
            val lock = redisson.getLock(key)
            try {
                val acquired = lock.tryLock(5, TimeUnit.SECONDS)
                println("[LOCK] thread=${Thread.currentThread().name} key=$key acquired=$acquired at=${System.currentTimeMillis()}")
                if (acquired.not()) {
                    throw LockNotAcquiredException(key = key)
                }
                block.invoke()
            } finally {
                if (lock.isLocked && lock.isHeldByCurrentThread) {
                    lock.unlock()
                    println("[LOCK] thread=${Thread.currentThread().name} key=$key released at=${System.currentTimeMillis()}")
                }
            }
        }
    }
}
