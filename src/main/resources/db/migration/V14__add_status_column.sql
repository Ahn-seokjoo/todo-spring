ALTER TABLE `email_sent_record`
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'READY' COMMENT '발송 상태 (READY: 발송 시도 중, SENT: 발송 확정)',
    MODIFY COLUMN sent_at DATETIME(6) NULL DEFAULT NULL COMMENT '발송이 확정된 시각 - status가 SENT로 바뀔 때만 채워짐 (READY인 동안은 NULL)';
