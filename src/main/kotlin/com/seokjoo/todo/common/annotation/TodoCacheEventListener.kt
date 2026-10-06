package com.seokjoo.todo.common.annotation

import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TodoCacheEventListener
