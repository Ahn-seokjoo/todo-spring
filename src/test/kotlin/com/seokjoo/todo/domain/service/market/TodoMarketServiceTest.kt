package com.seokjoo.todo.domain.service.market

import com.seokjoo.todo.annotation.TodoTest
import com.seokjoo.todo.domain.entity.category.Category
import com.seokjoo.todo.domain.entity.todo.Todo
import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.repository.category.CategoryRepository
import com.seokjoo.todo.domain.repository.todo.TodoRepository
import com.seokjoo.todo.domain.repository.todouser.TodoAuthRepository
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@TodoTest
@Transactional
class TodoMarketServiceTest @Autowired constructor(
    private val marketService: TodoMarketService,
    private val todoRepository: TodoRepository,
    private val authRepository: TodoAuthRepository,
    private val categoryRepository: CategoryRepository,
    private val em: EntityManager,
) {
    private lateinit var me: User
    private lateinit var other: User

    private lateinit var savedIds: List<Long>

    private lateinit var expectedCategories: Map<Long, List<String>>

    @BeforeEach
    fun setUp() {
        // 마켓은 전체 할일을 보여주므로, 다른 테스트가 커밋하고 안 지운 할일이 있으면 페이지 개수가 틀어진다
        assertThat(todoRepository.count())
            .withFailMessage("todo 테이블이 비어 있어야 함. 다른 테스트가 남긴 데이터 확인 필요")
            .isZero()

        val suffix = UUID.randomUUID().toString().take(8)
        me = authRepository.save(User(userId = "market_me_$suffix", password = "pw"))
        other = authRepository.save(User(userId = "market_other_$suffix", password = "pw"))

        // category.name이 unique라 suffix를 붙임
        val categories = listOf("운동", "공부", "독서").map { name ->
            categoryRepository.save(Category(name = "${name}_$suffix"))
        }

        // 1) 카테고리 없이 먼저 저장 → id가 i 순서대로 매겨진다
        //    (카테고리를 먼저 붙이면 Category.todoCategories cascade를 타고 다른 할일이 먼저 저장돼서 id 순서가 섞인다)
        val saved = todoRepository.saveAll(
            (0 until 25).map { i ->
                Todo(
                    todo = "market-$i",
                    owner = if (i % 2 == 0) me else other,
                    price = (i + 1) * 100L,
                )
            }
        )

        // 2) id가 확정된 뒤에 카테고리를 0~3개씩 붙인다 (flush 때 todo_category insert)
        saved.forEachIndexed { i, todo ->
            categories.take(i % 4).forEach { todo.addCategory(it) }
        }

        savedIds = saved.map { it.id }.sorted()
        expectedCategories = saved.associate { todo ->
            todo.id to todo.todoCategories.map { it.category!!.name }
        }

        // 1차 캐시를 비워야 조회가 메모리 객체가 아니라 실제 쿼리 결과로 검증된다
        em.flush()
        em.clear()
    }

    @Test
    fun `첫 페이지 요청인 MAX_VALUE 가 내림차순 20개를 준다`() {
        val result = marketService.getPagedMarketTodos(
            pageServiceDTO = TodoMarketPageServiceDTO(20, Long.MAX_VALUE),
        )

        assertThat(result.responseList.size).isEqualTo(20)
    }

    @Test
    fun `페이지 요청시에 30 을 쓰면 coreceIn 을 통해 최대값 20으로 요청을 받아온다`() {
        val result = marketService.getPagedMarketTodos(
            pageServiceDTO = TodoMarketPageServiceDTO(30, Long.MAX_VALUE),
        )

        assertThat(result.responseList.size).isEqualTo(20)
    }

    @Test
    fun `10개씩 next cursor로 연속 조회시 10개씩 받고 마지막엔 5개와 함께 cursor가 null 이다`() {
        val pageSize = 10
        val lastPageSize = 5
        val allSize = 25

        val result1 = marketService.getPagedMarketTodos(
            pageServiceDTO = TodoMarketPageServiceDTO(pageSize, Long.MAX_VALUE),
        )
        val result1Cursor = result1.responseList.last().id
        assertThat(result1.responseList.size).isEqualTo(pageSize)
        assertThat(result1.nextCursor).isEqualTo(result1Cursor)

        val result2 = marketService.getPagedMarketTodos(
            pageServiceDTO = TodoMarketPageServiceDTO(pageSize, result1Cursor),
        )
        val result2Cursor = result2.responseList.last().id
        assertThat(result2.responseList.size).isEqualTo(pageSize)
        assertThat(result2.nextCursor).isEqualTo(result2Cursor)

        val result3 = marketService.getPagedMarketTodos(
            pageServiceDTO = TodoMarketPageServiceDTO(pageSize, result2Cursor),
        )
        assertThat(result3.responseList.size).isEqualTo(lastPageSize)
        assertThat(result3.nextCursor).isNull()

        // 3개를 합쳤을때 25개고, 중복이 없는지
        val allList = result1.responseList + result2.responseList + result3.responseList
        assertThat(allList.size).isEqualTo(allSize)
        assertThat(allList.toSet().size).isEqualTo(allSize)

        // 아이템이 역정렬되어 왔는지
        assertThat(allList.map { it.id }).containsExactlyElementsOf(savedIds.reversed())
    }

    @Test
    fun `카테고리가 여러 개인 할일이 섞여 있어도 할일 개수가 줄지 않고 카테고리가 빠짐없이 붙는다`() {
        // given: 첫 페이지에 와야 하는 할일 10개 (id 내림차순)
        val expectedIds = savedIds.reversed().take(10)

        val joinedRowCount = expectedIds.sumOf { maxOf(expectedCategories.getValue(it).size, 1) }
        // expectedIds로 카테고리 갯수 센게 10개 이상인지
        assertThat(joinedRowCount).isGreaterThan(10)
        // 그게 0~3 안에 있는 값들인지
        assertThat(expectedIds.map { expectedCategories.getValue(it).size }).contains(0, 3)

        // when
        val result = marketService.getPagedMarketTodos(
            pageServiceDTO = TodoMarketPageServiceDTO(pageSize = 10, cursor = Long.MAX_VALUE),
        )

        assertThat(result.responseList.map { it.id }).containsExactlyElementsOf(expectedIds)

        // 카테고리 각 요소가 잘 있는지 체크
        result.responseList.forEach { todo ->
            assertThat(todo.categories)
                .`as`("todo id=${todo.id}")
                .containsExactlyInAnyOrderElementsOf(expectedCategories.getValue(todo.id))
        }
    }
}
