package com.seokjoo.todo.presentation.configuration

import org.slf4j.LoggerFactory
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig(
    private val asyncTaskExecutor: ThreadPoolTaskExecutor,
) : AsyncConfigurer {
    private val logger = LoggerFactory.getLogger(AsyncConfig::class.java)

    override fun getAsyncExecutor(): Executor = asyncTaskExecutor

    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler {
        return AsyncUncaughtExceptionHandler { ex, method, _ ->
            logger.error("Async method ${method.name} failed", ex)
        }
    }
}
