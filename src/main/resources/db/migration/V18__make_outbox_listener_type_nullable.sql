-- listener_type은 더 이상 애플리케이션에서 쓰지 않는다 (Kafka 전환 후 라우팅은 컨슈머 그룹 + event_type이 담당).
-- 값을 넣지 않아도 INSERT가 되도록 NULL 허용으로만 바꾼다.
-- 컬럼은 남겨 두어 이전 이미지로 롤백해도 동작하게 한다. 컬럼 삭제는 다음 마이그레이션에서.
ALTER TABLE outbox_event
    MODIFY listener_type VARCHAR(50) NULL COMMENT '사용 중단 - 다음 마이그레이션에서 삭제 예정';

ALTER TABLE outbox_event_archive
    MODIFY listener_type VARCHAR(50) NULL COMMENT '사용 중단 - 다음 마이그레이션에서 삭제 예정';

ALTER TABLE outbox_event_dlq
    MODIFY listener_type VARCHAR(50) NULL COMMENT '사용 중단 - 다음 마이그레이션에서 삭제 예정';
