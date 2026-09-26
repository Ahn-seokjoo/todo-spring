alter table `outbox_event`
    add column last_error_message VARCHAR(1000) DEFAULT NULL NULL
    COMMENT '마지막 실패 에러 메시지를 저장';

alter table `outbox_event_archive`
    add column last_error_message VARCHAR(1000) DEFAULT NULL NULL
    COMMENT '마지막 실패 에러 메시지를 저장 (첫 시도에 성공하지 못했을 수도 있기 때문)';
