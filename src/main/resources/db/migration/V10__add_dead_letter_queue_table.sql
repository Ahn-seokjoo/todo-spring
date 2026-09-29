CREATE TABLE IF NOT EXISTS `outbox_event_dlq`(
    id VARCHAR(36)         NOT NULL,
    listener_type          VARCHAR(50) NOT NULL,
    event_type             VARCHAR(50) NOT NULL,
    serialized_event       JSON NOT NULL,
    publication_date       DATETIME(6) NOT NULL,
    completion_date        DATETIME(6) DEFAULT NULL NULL,
    status                 VARCHAR(20) NOT NULL,
    completion_attempts    INT DEFAULT 0,
    last_resubmission_date DATETIME(6) DEFAULT NULL NULL,
    last_error_message VARCHAR(1000) DEFAULT NULL NULL,
    PRIMARY KEY (id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='TODO 이벤트 발행 5회 이상 실패 테이블';
