package com.seokjoo.todo.domain.service.outbox.annotation

import org.springframework.scheduling.annotation.Async
import org.springframework.transaction.event.TransactionalEventListener

@Async
@TransactionalEventListener
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TodoTransactionalEventListener
