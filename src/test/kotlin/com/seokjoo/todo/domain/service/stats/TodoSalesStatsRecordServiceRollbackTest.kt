package com.seokjoo.todo.domain.service.stats

import com.seokjoo.todo.annotation.TodoTest
import com.seokjoo.todo.domain.repository.stats.DailySalesStatsRepository
import com.seokjoo.todo.domain.repository.stats.StatsProcessedEventRepository
import com.seokjoo.todo.domain.service.sales.TodoSalesStatsRecordService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.LocalDate
import java.util.UUID

/**
 * 처리기록 INSERT와 집계 upsert가 "한 트랜잭션"인지 검증한다.
 */
@TodoTest
class TodoSalesStatsRecordServiceRollbackTest @Autowired constructor(
    private val service: TodoSalesStatsRecordService,
    private val statsProcessedEventRepository: StatsProcessedEventRepository,
) {
    // 집계 레포지토리만 가짜로 바꿔 upsert 실패를 흉내 낸다. 처리기록 레포지토리는 진짜(H2).
    @MockitoBean
    private lateinit var dailySalesStatsRepository: DailySalesStatsRepository

    private val eventId = UUID.randomUUID().toString()
    private val sellerId = "seller-${UUID.randomUUID().toString().take(8)}"
    private val salesDate = LocalDate.of(2026, 10, 19)

    @AfterEach
    fun cleanUp() {
        if (statsProcessedEventRepository.existsById(eventId)) {
            statsProcessedEventRepository.deleteById(eventId)
        }
    }

    @Test
    fun `집계 upsert가 실패하면 처리기록도 롤백되어, 재시도 때 다시 처리된다`() {
        // 첫 번째 upsert는 DB 장애로 실패, 두 번째는 성공한다고 가정
        whenever(dailySalesStatsRepository.upsertDailySalesStats(any(), any(), any()))
            .thenThrow(RuntimeException("DB 장애 가정"))
            .thenReturn(1)

        // 1차 시도: upsert 실패 → 예외가 서비스 밖으로 → 트랜잭션 롤백
        assertThrows<RuntimeException> {
            service.record(eventId = eventId, amount = 100L, sellerId = sellerId, salesDate = salesDate)
        }
        // 처리기록 INSERT도 함께 롤백되어 남아 있지 않아야 한다 (따로 커밋됐다면 여기서 true → 재시도 때 skip → 매출 누락)
        assertThat(statsProcessedEventRepository.existsById(eventId)).isFalse()

        // 2차 시도 (Kafka 재시도 상황): 처리기록이 없으니 INSERT IGNORE = 1 → 다시 집계
        val retried = service.record(eventId = eventId, amount = 100L, sellerId = sellerId, salesDate = salesDate)

        assertThat(retried).isTrue()
        assertThat(statsProcessedEventRepository.existsById(eventId)).isTrue()
        verify(dailySalesStatsRepository, times(2)).upsertDailySalesStats(any(), any(), any())
    }
}
