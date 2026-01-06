package com.seokjoo.todo.common.redisson

interface LockManager {
    fun <T> tryLock(key: String, retryCount: Int = 3, block: () -> T): T
}
