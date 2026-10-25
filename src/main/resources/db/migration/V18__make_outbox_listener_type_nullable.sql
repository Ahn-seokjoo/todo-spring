-- listener_type은 더 이상 애플리케이션에서 쓰지 않는다 (Kafka 전환 후 라우팅은 컨슈머 그룹 + event_type이 담당).
-- 1단계: 새 코드는 이 컬럼을 INSERT에서 빼므로, DB 기본값으로 채워지게 한다.
-- NULL이 아니라 기존 값과 같은 기본값을 쓰는 이유: 이전 이미지로 롤백했을 때 이전 엔티티(non-null 필드)가
-- 새 버전이 쓴 행을 읽어 아카이브/DLQ로 옮겨도 NPE가 나지 않게 하기 위함.
-- 컬럼 삭제는 다음 마이그레이션에서.
-- UPDATE: 이전 버전의 V18(NULL 허용)이 적용된 환경에 생긴 NULL 행을 먼저 채운다. 없으면 0행.

UPDATE outbox_event SET listener_type = 'EMAIL_NOTIFICATION' WHERE listener_type IS NULL;
ALTER TABLE outbox_event
    MODIFY listener_type VARCHAR(50) NOT NULL DEFAULT 'EMAIL_NOTIFICATION'
    COMMENT '사용 중단 - 롤백 호환용 기본값, 다음 마이그레이션에서 삭제 예정';

UPDATE outbox_event_archive SET listener_type = 'EMAIL_NOTIFICATION' WHERE listener_type IS NULL;
ALTER TABLE outbox_event_archive
    MODIFY listener_type VARCHAR(50) NOT NULL DEFAULT 'EMAIL_NOTIFICATION'
    COMMENT '사용 중단 - 롤백 호환용 기본값, 다음 마이그레이션에서 삭제 예정';

UPDATE outbox_event_dlq SET listener_type = 'EMAIL_NOTIFICATION' WHERE listener_type IS NULL;
ALTER TABLE outbox_event_dlq
    MODIFY listener_type VARCHAR(50) NOT NULL DEFAULT 'EMAIL_NOTIFICATION'
    COMMENT '사용 중단 - 롤백 호환용 기본값, 다음 마이그레이션에서 삭제 예정';
