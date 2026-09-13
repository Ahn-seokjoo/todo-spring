package com.seokjoo.todo.common.redisson

import org.slf4j.LoggerFactory
import org.springframework.retry.RetryCallback
import org.springframework.retry.RetryContext
import org.springframework.retry.RetryListener
import org.springframework.stereotype.Component

@Component
class LoggingRetryListener : RetryListener {
    private val logger = LoggerFactory.getLogger(LoggingRetryListener::class.java)

    override fun <T : Any?, E : Throwable?> onError(
        context: RetryContext?, callback: RetryCallback<T?, E?>?, throwable: Throwable?
    ) {
        logger.warn(
            "[Retry] Error attempt = ${context?.retryCount}," +
                " exception = ${throwable?.javaClass?.simpleName}," +
                " message = ${throwable?.message}"
        )
    }
}
