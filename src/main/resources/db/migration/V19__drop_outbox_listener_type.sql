-- listener_type 제거 2단계(contract): V18에서 애플리케이션이 이 컬럼을 더 이상 읽고 쓰지 않게 바꿨고,
-- 기본값으로 이전 버전 롤백 호환을 유지했다. 운영 배포 후 아웃박스 경로 정상 확인, V18 이전 이미지로는 롤백하지 않기로 판단해 삭제한다.
-- 참고: V18 파일 이름은 "nullable"이지만 실제 내용은 NOT NULL + DEFAULT (리뷰 반영, 운영 기록과 맞추려고 이름 유지).
ALTER TABLE outbox_event DROP COLUMN listener_type;
ALTER TABLE outbox_event_archive DROP COLUMN listener_type;
ALTER TABLE outbox_event_dlq DROP COLUMN listener_type;
