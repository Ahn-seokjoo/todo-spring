CREATE TABLE IF NOT EXISTS `email_sent_record`(
    id      VARCHAR(36) NOT NULL,
    sent_at DATETIME(6) NOT NULL COMMENT '발송 시각',
    PRIMARY KEY (id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='이메일 전송 여부';
