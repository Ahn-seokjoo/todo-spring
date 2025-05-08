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
        } catch (e: IllegalStateException) {
            // do nothing
            // 왜냐하면, lock걸고 추가하는 카테고리의 경우, 1개만 들어가면 되기 때문~
        } finally {
            lock.unlock()
        }
    }
}
