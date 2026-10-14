-- =====================================================================
-- 판매자 일별 판매 통계 부하 테스트용 시드 데이터 (todo_purchase 300만 건)
--
-- ※ Flyway 마이그레이션이 아닌 "수동 실행용 실험 파일"입니다. 로컬 개발 DB에서만 실행하세요.
-- ※ 시드 행은 seller_id / buyer_id 가 'seed_' 로 시작합니다.
--    정리: DELETE FROM todo_purchase WHERE seller_id LIKE 'seed_%';
--          DELETE FROM todo_user WHERE user_id LIKE 'seed_%';   -- k6 setup이 가입시킨 계정
--
-- 분포 (n = 0 ~ 2,999,999, j = n DIV 10000 = 판매자별 몇 번째 건, 0~299)
--   - 판매자 1만 명(seed_seller_00000 ~ 09999) × 300건
--   - 판매자 안에서도 상태 APPROVED 70% / REJECTED 20% / CANCELLED 10%  (j 기준)
--   - 가격 1,000 ~ 5,900원 50종 (j 기준)
--   - created_at: 판매자별로 연속 300일에 하루 1건(나머지 65일은 빈 구간)
--   주의: 상태·가격을 n % 10, n % 50 으로 만들면 10과 50이 10000의 약수라
--         판매자마다 상태·가격이 하나로 고정된다. 그래서 j(n DIV 10000) 기준으로 생성.
--
-- 소요 시간(로컬 참고값): seed_numbers 300만 건 약 8초, todo_purchase 100만 건당 6~7초
-- =====================================================================

-- 1. 숫자 테이블 (n = 0 ~ 2,999,999)
CREATE TABLE digits (d TINYINT PRIMARY KEY);
INSERT INTO digits VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9);

CREATE TABLE seed_numbers (n INT PRIMARY KEY);
INSERT INTO seed_numbers (n)
SELECT d1.d + d2.d*10 + d3.d*100 + d4.d*1000 + d5.d*10000 + d6.d*100000 + d7.d*1000000
FROM digits d1 CROSS JOIN digits d2 CROSS JOIN digits d3 CROSS JOIN digits d4
CROSS JOIN digits d5 CROSS JOIN digits d6 CROSS JOIN digits d7
WHERE d7.d < 3;

SELECT COUNT(*), MIN(n), MAX(n) FROM seed_numbers;   -- 3000000, 0, 2999999

-- 2. todo_purchase 시드 (100만 건씩 3회. 회차마다 마지막 WHERE만 바꿔 한 번씩 실행)
--    1회차: WHERE n < 1000000
--    2회차: WHERE n >= 1000000 AND n < 2000000
--    3회차: WHERE n >= 2000000
INSERT INTO todo_purchase
  (todo_id, seller_id, buyer_id, price, purchase_status, created_at, updated_at)
SELECT
  n + 1,
  CONCAT('seed_seller_', LPAD(n % 10000, 5, '0')),
  CONCAT('seed_buyer_',  LPAD(n % 50000, 5, '0')),
  1000 + ((n DIV 10000) % 50) * 100,
  CASE
    WHEN (n DIV 10000) % 10 < 7 THEN 'APPROVED'
    WHEN (n DIV 10000) % 10 < 9 THEN 'REJECTED'
    ELSE 'CANCELLED'
  END,
  NOW() - INTERVAL ((n DIV 10000 + n % 10000) % 365) DAY - INTERVAL (n % 86400) SECOND,
  NOW() - INTERVAL ((n DIV 10000 + n % 10000) % 365) DAY - INTERVAL (n % 86400) SECOND
FROM seed_numbers
WHERE n < 1000000;

-- 회차마다 확인: 100만 → 200만 → 300만
SELECT COUNT(*) FROM todo_purchase WHERE seller_id LIKE 'seed_%';

-- 3. 검증
-- 전체: APPROVED 2,100,000 / REJECTED 600,000 / CANCELLED 300,000
SELECT purchase_status, COUNT(*) FROM todo_purchase
WHERE seller_id LIKE 'seed_%' GROUP BY purchase_status;

-- 판매자 한 명: 210 / 60 / 30
SELECT purchase_status, COUNT(*) FROM todo_purchase
WHERE seller_id = 'seed_seller_00123' GROUP BY purchase_status;

-- 같은 판매자의 가격 종류 / 날짜 수: 50 / 300 근처 (시각을 빼면서 날짜가 겹쳐 실행 시각에 따라 260~300)
SELECT COUNT(DISTINCT price), COUNT(DISTINCT DATE(created_at)) FROM todo_purchase
WHERE seller_id = 'seed_seller_00123';

-- 4. 마무리: 옵티마이저 통계 갱신 + 임시 테이블 삭제
ANALYZE TABLE todo_purchase;
DROP TABLE seed_numbers;
DROP TABLE digits;

-- 참고: k6/sales_daily_test.js 는 seed_seller_00100 ~ 00199 를 사용한다.
--       created_at 이 NOW() 기준이므로, 시드를 넣은 날짜에 맞춰 k6 의 FROM/TO 를 조정할 것.
--       (판매자 0~99번 중 일부는 최근 30일이 빈 구간에 걸려 결과가 비어 있음)
