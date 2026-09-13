package com.seokjoo.todo.common.redisson

interface LockManager {
    fun <T> tryLock(
        key: String,
        retryCount: Int = 5,
        delay: Long = 100L,
        maxDelay: Long = 3000L,
        block: () -> T,
    ): T
}
