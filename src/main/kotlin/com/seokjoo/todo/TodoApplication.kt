package com.seokjoo.todo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableCaching
class TodoApplication

fun main(args: Array<String>) {
    runApplication<TodoApplication>(*args)
}
