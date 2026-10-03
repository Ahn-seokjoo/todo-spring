package com.seokjoo.todo.domain.service.outbox

import com.seokjoo.todo.domain.service.outbox.service.OutboxDLQReconciler
import com.seokjoo.todo.domain.service.outbox.service.OutboxService
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class OutboxDLQReconcilerTest : BehaviorSpec({
    val outboxService: OutboxService = mockk()
    val reconciler = OutboxDLQReconciler(outboxService)

    Given("reconcile이 수행될 때") {
        When("정상적으로 수행되면") {
            every { outboxService.reconcileConfirmedSentDlqEvents() } returns 2

            Then("outboxService.reconcileConfirmedSentDlqEvents가 1회 호출된다") {
                reconciler.reconcile()
                verify(exactly = 1) { outboxService.reconcileConfirmedSentDlqEvents() }
            }
        }

        When("outboxService 호출 중 예외가 발생해도") {
            every { outboxService.reconcileConfirmedSentDlqEvents() } throws RuntimeException("boom")

            Then("예외가 밖으로 전파되지 않고 조용히 삼켜진다") {
                reconciler.reconcile()
                verify(exactly = 1) { outboxService.reconcileConfirmedSentDlqEvents() }
            }
        }
    }
}) {
    override fun isolationMode(): IsolationMode = IsolationMode.InstancePerLeaf
}
