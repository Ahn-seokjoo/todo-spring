package com.seokjoo.todo

import com.seokjoo.todo.annotation.TodoTest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@TodoTest
class TodoApplicationTests {

    @Test
    fun contextLoads() {
    }
}
