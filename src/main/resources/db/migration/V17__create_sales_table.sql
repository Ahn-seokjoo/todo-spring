CREATE TABLE IF NOT EXISTS daily_sales_stats(
    seller_id               VARCHAR(255) NOT NULL COMMENT '판매자 ID',
    sales_amount            BIGINT       NOT NULL COMMENT '판매 금액 합계',
    sales_count             BIGINT       NOT NULL COMMENT '판매 건수',
    sales_date              DATE         NOT NULL COMMENT '판매 날짜 (승인 시각 기준, KST)',
    created_at              DATETIME(6)  NOT NULL,
    updated_at              DATETIME(6)  NOT NULL,
    PRIMARY KEY (seller_id, sales_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='TODO 판매 통계';

CREATE TABLE IF NOT EXISTS stats_processed_event(
    id           VARCHAR(36) NOT NULL COMMENT '아웃박스 이벤트 id',
    processed_at DATETIME(6) NOT NULL COMMENT '처리 시각',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='매출 통계에 반영된 이벤트';
