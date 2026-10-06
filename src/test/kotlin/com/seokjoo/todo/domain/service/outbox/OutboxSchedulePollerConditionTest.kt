package com.seokjoo.todo.domain.service.outbox

import com.seokjoo.todo.domain.service.outbox.service.OutboxEventProcessor
import com.seokjoo.todo.domain.service.outbox.service.OutboxSchedulePoller
import com.seokjoo.todo.domain.service.outbox.service.OutboxService
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class OutboxSchedulePollerConditionTest {
    private val runner = ApplicationContextRunner()
        .withBean(OutboxService::class.java, { mockk<OutboxService>() })
        .withBean(OutboxEventProcessor::class.java, { mockk<OutboxEventProcessor>() })
        .withUserConfiguration(OutboxSchedulePoller::class.java)

    @Test
    fun `프로퍼티가 아예 없으면 폴러가 만들어진다 (matchIfMissing)`() {
        runner.run { context ->
            assertThat(context.getBeansOfType(OutboxSchedulePoller::class.java)).hasSize(1)
        }
    }

    @Test
    fun `enabled=true 이면 폴러가 만들어진다`() {
        runner.withPropertyValues("outbox.poller.enabled=true").run { context ->
            assertThat(context.getBeansOfType(OutboxSchedulePoller::class.java)).hasSize(1)
        }
    }

    @Test
    fun `enabled=false 이면 폴러가 만들어지지 않는다`() {
        runner.withPropertyValues("outbox.poller.enabled=false").run { context ->
            assertThat(context.getBeansOfType(OutboxSchedulePoller::class.java)).isEmpty()
        }
    }
}
