package com.seokjoo.todo.domain.service.todo

import com.seokjoo.todo.domain.repository.todo.TodoRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("prod")
class TodoServiceCreateTest @Autowired constructor(
    private val todoRepository: TodoRepository,
) {

    /**
     *  todo를 200개 생성해줍니다
     *  테스트 용이니 내부는 주석처리 되어 있습니다. 사용 후에 주석 꼭 해주세용
     */
    @Test
    fun `createTodo 200개 생성`() {
        // for (i in 1..200) {
        //     todoRepository.save(Todo(todo = "todo count : $i"))
        // }
    }
}
