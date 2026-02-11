package com.seokjoo.todo.annotation

import com.seokjoo.todo.config.TestLockConfig
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest
@ActiveProfiles("test")
@Import(TestLockConfig::class)
annotation class TodoTest
