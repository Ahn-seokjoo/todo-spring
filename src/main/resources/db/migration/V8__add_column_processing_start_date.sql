alter table `outbox_event`
    add column processing_started_date DATETIME(6) DEFAULT NULL NULL
    COMMENT 'processing 으로 변경된 시각 - 해당 값과 threshold 를 통해 processing 중에 앱이 멈추었는지를 평가합니다';
