package com.seokjoo.todo.common.exception

class LockNotAcquiredException(key: String) : RuntimeException("Lock key 획득 실패 : $key")
