package com.seokjoo.todo.domain.service.email

import com.seokjoo.todo.annotation.TodoTest
import com.seokjoo.todo.domain.entity.email.EmailSentStatus
import com.seokjoo.todo.domain.entity.outbox.OutboxEventArchive
import com.seokjoo.todo.domain.entity.outbox.OutboxStatus
import com.seokjoo.todo.domain.service.outbox.OutboxEventType
import com.seokjoo.todo.domain.service.outbox.OutboxListenerType
import com.seokjoo.todo.domain.service.outbox.repository.OutboxArchiveRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@TodoTest
@Transactional
class EmailServiceTest @Autowired constructor(
    private val emailService: EmailService,
    private val outboxArchiveRepository: OutboxArchiveRepository,
) {

    @Test
    fun `같은 outboxEventId로 saveEmailSentRecordToReady를 두 번 호출하면 두 번째는 중복 키 예외가 발생한다`() {
        val id = UUID.randomUUID().toString()

        emailService.saveEmailSentRecordToReady(id = id)

        assertThrows(DataIntegrityViolationException::class.java) {
            emailService.saveEmailSentRecordToReady(id = id)
        }
    }

    @Test
    fun `상태는 저장 전 null, saveEmailSentRecordToReady 이후 READY, markSentAt 이후 SENT로 전이된다`() {
        val id = UUID.randomUUID().toString()

        assertThat(emailService.findStatusByEmailId(id)).isNull()

        emailService.saveEmailSentRecordToReady(id = id)
        assertThat(emailService.findStatusByEmailId(id)).isEqualTo(EmailSentStatus.READY)

        emailService.markSentAt(id = id)
        assertThat(emailService.findStatusByEmailId(id)).isEqualTo(EmailSentStatus.SENT)
    }

    @Test
    fun `markSentAt은 READY 상태가 아닌 레코드에는 영향을 주지 않고 조용히 무시된다`() {
        // 이미 SENT인 레코드에 다시 markSentAt을 호출해도 상태가 바뀌거나 예외가 나지 않는다.
        // (updateSent 쿼리의 where 조건이 status = READY이기 때문에 영향받은 row가 0건일 뿐 조용히 넘어간다)
        val alreadySentId = UUID.randomUUID().toString()
        emailService.saveEmailSentRecordToReady(id = alreadySentId)
        emailService.markSentAt(id = alreadySentId)

        emailService.markSentAt(id = alreadySentId)
        assertThat(emailService.findStatusByEmailId(alreadySentId)).isEqualTo(EmailSentStatus.SENT)

        // 존재하지 않는 id에 호출해도 마찬가지로 조용히 무시된다.
        val missingId = UUID.randomUUID().toString()
        emailService.markSentAt(id = missingId)
        assertThat(emailService.findStatusByEmailId(missingId)).isNull()
    }

    @Test
    fun `deleteSentRecord 호출 후에는 findStatusByEmailId가 다시 null을 반환한다`() {
        val id = UUID.randomUUID().toString()
        emailService.saveEmailSentRecordToReady(id = id)
        assertThat(emailService.findStatusByEmailId(id)).isEqualTo(EmailSentStatus.READY)

        emailService.deleteSentRecord(id = id)

        assertThat(emailService.findStatusByEmailId(id)).isNull()
    }

    @Test
    fun `reconcileConfirmedSentRecords는 READY인데 대응하는 outbox_event가 이미 archive에 있으면 SENT로 되돌린다`() {
        val id = UUID.randomUUID().toString()
        emailService.saveEmailSentRecordToReady(id = id)
        // sendEmail은 성공했지만 markSentAt 기록만 실패한 상황을 흉내낸다: outbox는 이미 SUCCESS로 종결(=archive에 존재).
        outboxArchiveRepository.save(
            OutboxEventArchive(
                id = id,
                listenerType = OutboxListenerType.EMAIL_NOTIFICATION,
                eventType = OutboxEventType.PURCHASE_REQUEST,
                serializedEvent = "dummy",
                publicationDate = LocalDateTime.now(),
                status = OutboxStatus.SUCCESS,
            )
        )

        val updatedCount = emailService.reconcileConfirmedSentRecords()

        assertThat(updatedCount).isEqualTo(1)
        assertThat(emailService.findStatusByEmailId(id)).isEqualTo(EmailSentStatus.SENT)
    }

    @Test
    fun `reconcileConfirmedSentRecords는 대응하는 outbox_event_archive가 없는 READY 레코드는 건드리지 않는다`() {
        val id = UUID.randomUUID().toString()
        emailService.saveEmailSentRecordToReady(id = id)

        val updatedCount = emailService.reconcileConfirmedSentRecords()

        assertThat(updatedCount).isEqualTo(0)
        assertThat(emailService.findStatusByEmailId(id)).isEqualTo(EmailSentStatus.READY)
    }
}
