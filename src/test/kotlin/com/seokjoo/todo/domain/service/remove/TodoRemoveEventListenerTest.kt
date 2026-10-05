package com.seokjoo.todo.domain.service.remove

import com.seokjoo.todo.domain.repository.category.CategoryRepository
import com.seokjoo.todo.domain.service.category.listener.CategoryDeletedListener
import com.seokjoo.todo.domain.service.remove.event.TodoDeletedEvent
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify

class TodoRemoveEventListenerTest : BehaviorSpec({
    val repository: CategoryRepository = mockk()
    val listener = CategoryDeletedListener(repository)

    Given("Todo 삭제 이벤트가 들어오면") {
        every { repository.deleteCategoryByIdsIfNotExistInTodoCategory(any()) } just Runs

        When("categoryIds가 비어있지 않으면") {
            listener.eventListener(TodoDeletedEvent(categoryIds = listOf(1, 2)))
            Then("deleteCategoryByIdsIfNotExistInTodoCategory 가 1회 호출된다") {
                verify(exactly = 1) {
                    repository.deleteCategoryByIdsIfNotExistInTodoCategory(ids = listOf(1, 2))
                }
            }
        }

        When("categoryIds가 비어있으면") {
            listener.eventListener(TodoDeletedEvent(categoryIds = listOf()))

            Then("deleteCategoryByIdsIfNotExistInTodoCategory 가 0회 호출된다") {
                verify(exactly = 0) {
                    repository.deleteCategoryByIdsIfNotExistInTodoCategory(ids = any())
                }
            }
        }
    }
}) {
    override fun isolationMode(): IsolationMode = IsolationMode.InstancePerLeaf
}
