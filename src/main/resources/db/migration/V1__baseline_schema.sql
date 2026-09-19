-- 기존에 Hibernate ddl-auto(update)로 생성/누적돼있던 스키마를 그대로 baseline으로 옮긴 마이그레이션.
-- 로컬 DB의 실제 SHOW CREATE TABLE 결과를 기준으로 작성함 (2026-09-19).
-- AUTO_INCREMENT 시작값은 기존 데이터의 흔적이라 여기서는 제외 — 신규 스키마는 1부터 시작.

CREATE TABLE `todo_user` (
    `todo_user_id` BIGINT NOT NULL AUTO_INCREMENT,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    `money` BIGINT NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `user_id` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`todo_user_id`),
    UNIQUE KEY `UC_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `category` (
    `category_id` BIGINT NOT NULL AUTO_INCREMENT,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    `name` VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (`category_id`),
    UNIQUE KEY `UK46ccwnsi9409t36lurvtyljak` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `todo` (
    `todo_id` BIGINT NOT NULL AUTO_INCREMENT,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    `is_done` BIT(1) NOT NULL,
    `price` BIGINT NOT NULL,
    `todo` VARCHAR(255) DEFAULT NULL,
    `owner_todo_user_id` BIGINT DEFAULT NULL,
    `version` BIGINT NOT NULL,
    PRIMARY KEY (`todo_id`),
    KEY `FK4ggwae0141n58mw6oc6x4ner` (`owner_todo_user_id`),
    CONSTRAINT `FK4ggwae0141n58mw6oc6x4ner` FOREIGN KEY (`owner_todo_user_id`) REFERENCES `todo_user` (`todo_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `todo_category` (
    `todo_category_id` BIGINT NOT NULL AUTO_INCREMENT,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    `category_id` BIGINT DEFAULT NULL,
    `todo_id` BIGINT DEFAULT NULL,
    PRIMARY KEY (`todo_category_id`),
    KEY `FKcyvi5ouyhwmfqnxrpvqh4xfi3` (`category_id`),
    KEY `FKi3073x7gl07du43tlbwdqgaiv` (`todo_id`),
    CONSTRAINT `FKcyvi5ouyhwmfqnxrpvqh4xfi3` FOREIGN KEY (`category_id`) REFERENCES `category` (`category_id`),
    CONSTRAINT `FKi3073x7gl07du43tlbwdqgaiv` FOREIGN KEY (`todo_id`) REFERENCES `todo` (`todo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
