package com.seokjoo.todo.domain.service.email

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class EmailSentRecordReconcilerTest : BehaviorSpec({
    val emailService: EmailService = mockk()
    val reconciler = EmailSentRecordReconciler(emailService)

    Given("reconcile이 수행될 때") {
        When("정상적으로 수행되면") {
            every { emailService.reconcileConfirmedSentRecords() } returns 2

            Then("emailService.reconcileConfirmedSentRecords가 1회 호출된다") {
                reconciler.reconcile()
                verify(exactly = 1) { emailService.reconcileConfirmedSentRecords() }
            }
        }

        When("emailService 호출 중 예외가 발생해도") {
            every { emailService.reconcileConfirmedSentRecords() } throws RuntimeException("문제 발생")

            Then("예외가 밖으로 전파되지 않고 조용히 삼켜진다") {
                reconciler.reconcile()
                verify(exactly = 1) { emailService.reconcileConfirmedSentRecords() }
            }
        }
    }
}) {
    override fun isolationMode(): IsolationMode = IsolationMode.InstancePerLeaf
}
