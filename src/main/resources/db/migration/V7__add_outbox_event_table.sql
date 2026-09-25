CREATE TABLE IF NOT EXISTS `outbox_event`(
    id VARCHAR(36)         NOT NULL COMMENT 'uuid id',
    listener_type          VARCHAR(50) NOT NULL COMMENT '이벤트 발행을 듣는 곳',
    event_type             VARCHAR(50) NOT NULL COMMENT '발행된 이벤트 타입',
    serialized_event       JSON NOT NULL COMMENT '발행된 이벤트 JSON 원본',
    publication_date       DATETIME(6) NOT NULL COMMENT '발행된 시각',
    completion_date        DATETIME(6) DEFAULT NULL NULL COMMENT '완료된 시각',
    status                 VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '이벤트 상태',
    completion_attempts    INT DEFAULT 0 COMMENT '이벤트 실행 시도 횟수',
    last_resubmission_date DATETIME(6) DEFAULT NULL NULL COMMENT '마지막 실행 시도 시각',
    PRIMARY KEY (id),
    INDEX idx_status_and_completion_attempts (status, completion_attempts)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci
    COMMENT='TODO 이벤트 발행';

CREATE TABLE IF NOT EXISTS `outbox_event_archive`(
    id VARCHAR(36)         NOT NULL,
    listener_type          VARCHAR(50) NOT NULL,
    event_type             VARCHAR(50) NOT NULL,
    serialized_event       JSON NOT NULL,
    publication_date       DATETIME(6) NOT NULL,
    completion_date        DATETIME(6) DEFAULT NULL NULL,
    status                 VARCHAR(20) NOT NULL,
    completion_attempts    INT DEFAULT 0,
    last_resubmission_date DATETIME(6) DEFAULT NULL NULL,
    PRIMARY KEY (id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='TODO 이벤트 발행 아카이빙';
