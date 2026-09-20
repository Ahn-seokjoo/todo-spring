CREATE TABLE if not exists `todo_purchase`(
    id BIGINT NOT NULL AUTO_INCREMENT ,
    todo_id BIGINT NOT NULL COMMENT 'todo id',
    seller_id VARCHAR(255) NOT NULL COMMENT '판매자 ID',
    buyer_id VARCHAR(255) NOT NULL COMMENT '구매자 ID',
    price BIGINT NOT NULL COMMENT 'todo 가격',
    purchase_status VARCHAR(32) NOT NULL COMMENT '구매 상태',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='TODO 구매 상태';
