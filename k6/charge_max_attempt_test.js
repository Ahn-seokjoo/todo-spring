import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// charge()의 maxAttempts vs LockManager의 retryCount 경쟁 테스트.
// seller 1명의 todo를 buyer 여러 명이 동시 구매(-> seller 잔액 증가) + seller 본인 self-charge.
// 같은 seller row에 두 코드 경로가 동시에 쓰기 -> 낙관락 충돌 재현.
//
// 사용법: charge()의 maxAttempts를 1로 낮추고 BUYER_VUS를 올려가며 실행 ->
// charge_conflict_exhausted(=500 비율)로 원시 충돌률 측정 -> 5로 복구 후 재비교.
// unexpected_failure는 충돌과 무관한 실패(잔액부족 등)이니 0에 가까운지 같이 확인.
// own_todo_collision(자기 todo 재구매)은 테스트 풀 크기 때문에 생기는 harmless한 현상이라 무시해도 됨.

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PASSWORD = __ENV.PASSWORD || 'loadtest-pw-1234!';
const RUN_ID = __ENV.RUN_ID || `${Date.now()}`;
const TODO_PRICE = Number(__ENV.TODO_PRICE || 100);
const BUYER_VUS = Number(__ENV.BUYER_VUS || 5);
const CHARGER_VUS = Number(__ENV.CHARGER_VUS || 1);
const DURATION = __ENV.DURATION || '30s';
const BUYER_SLEEP = Number(__ENV.BUYER_SLEEP ?? 0.02);
const CHARGER_SLEEP = Number(__ENV.CHARGER_SLEEP ?? 0.02);
// VU 한 명이 DURATION 동안 도는 횟수만큼만 있으면 되고 BUYER_VUS에 비례할 필요는 없다
// (VU가 늘어도 한 VU가 도는 횟수가 늘어나는 게 아니라서). 고정값 하나로 충분.
const TODO_POOL_SIZE = Number(__ENV.TODO_POOL_SIZE || 5000);

export const chargeConflictExhausted = new Rate('charge_conflict_exhausted');
export const buyConflictExhausted = new Rate('buy_conflict_exhausted');
export const buyUnexpectedFailure = new Rate('buy_unexpected_failure');
export const chargeUnexpectedFailure = new Rate('charge_unexpected_failure');
// pool이 작고 실행이 길어지면 buyer가 자기가 이미 산 todo를 또 사려는 시도가 생긴다.
// 이건 앱 버그가 아니라 테스트 설계상의 harmless한 현상이라 별도로 분리해서 센다.
export const buyOwnTodoCollision = new Rate('buy_own_todo_collision');

export const options = {
  setupTimeout: '120s', // pool 생성이 오래 걸릴 때 기본 60초 제한에 안 걸리게 여유를 둔다
  scenarios: {
    buyers: { executor: 'constant-vus', exec: 'buyFlow', vus: BUYER_VUS, duration: DURATION },
    selfCharger: { executor: 'constant-vus', exec: 'selfChargeFlow', vus: CHARGER_VUS, duration: DURATION },
  },
};

function signupAndLogin(userId, password) {
  const payload = JSON.stringify({ user_id: userId, password });
  const headers = { 'Content-Type': 'application/json' };
  http.post(`${BASE_URL}/api/v1/auth/signup`, payload, { headers });
  const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, payload, { headers });
  if (loginRes.status !== 200) {
    throw new Error(`login failed for ${userId}: ${loginRes.status} ${loginRes.body}`);
  }
  return loginRes.json().accessToken;
}

// seller 1명 + buyer들이 나눠 살 todo 풀 준비 (1회 실행)
export function setup() {
  const sellerId = `loadtest-seller-${RUN_ID}`;
  const sellerToken = signupAndLogin(sellerId, PASSWORD);
  const sellerHeaders = { Authorization: `Bearer ${sellerToken}`, 'Content-Type': 'application/json' };

  const todoIds = [];
  for (let i = 0; i < TODO_POOL_SIZE; i++) {
    const res = http.post(
      `${BASE_URL}/api/v1/todos`,
      JSON.stringify({ todo: `실험용 투두 ${i}`, price: TODO_PRICE }),
      { headers: sellerHeaders }
    );
    if (res.status !== 201 && res.status !== 200) {
      throw new Error(`todo 생성 실패: ${res.status} ${res.body}`);
    }
    todoIds.push(res.json().id);
  }

  console.log(`[setup] seller=${sellerId}, todo ${todoIds.length}개, BUYER_VUS=${BUYER_VUS}, CHARGER_VUS=${CHARGER_VUS}`);
  return { sellerId, sellerToken, todoIds };
}

let buyerToken = null;
let buyerId = null;

export function buyFlow(data) {
  if (!buyerToken) {
    buyerId = `loadtest-buyer-${RUN_ID}-${__VU}`;
    buyerToken = signupAndLogin(buyerId, PASSWORD);
    // 재판매 제한이 없어 거의 모든 구매가 실제로 잔액을 깎으므로 넉넉히 충전
    const fundRes = http.post(
      `${BASE_URL}/api/v1/charge/${buyerId}`,
      JSON.stringify({ amount: TODO_PRICE * 1000000 }),
      { headers: { Authorization: `Bearer ${buyerToken}`, 'Content-Type': 'application/json' } }
    );
    if (fundRes.status !== 200) {
      console.error(`[buyer 자금충전 실패] vu=${__VU} status=${fundRes.status} body=${fundRes.body}`);
    }
  }

  // 서로 다른 todo로 분산 (같은 todo면 Redisson 락이 직렬화시켜 충돌이 안 만들어짐)
  const todoIndex = (__VU * 97 + __ITER) % data.todoIds.length;
  const todoId = data.todoIds[todoIndex];

  const res = http.post(
    `${BASE_URL}/api/v1/trade`,
    JSON.stringify({ todo_id: todoId }),
    { headers: { Authorization: `Bearer ${buyerToken}`, 'Content-Type': 'application/json' } }
  );

  const exhausted = res.status === 500;
  buyConflictExhausted.add(exhausted);
  check(res, { 'buy: not exhausted (500)': (r) => r.status !== 500 });
  if (exhausted) console.error(`[buy 500] vu=${__VU} iter=${__ITER} todoId=${todoId} body=${res.body}`);

  // TR000_CAN_NOT_TRADE_TODO(자기가 이미 산 todo 재구매)는 앱 버그가 아니라 테스트
  // 풀 크기/실행시간에 따른 harmless한 현상이므로 unexpected_failure와 분리해서 센다.
  const isOwnTodoCollision = res.status === 400 && res.body && res.body.includes('TR000_CAN_NOT_TRADE_TODO');
  buyOwnTodoCollision.add(isOwnTodoCollision);

  const unexpected = res.status !== 200 && res.status !== 500 && !isOwnTodoCollision;
  buyUnexpectedFailure.add(unexpected);
  if (unexpected) console.error(`[buy 기타실패 status=${res.status}] vu=${__VU} iter=${__ITER} body=${res.body}`);

  if (BUYER_SLEEP > 0) sleep(BUYER_SLEEP);
}

export function selfChargeFlow(data) {
  const res = http.post(
    `${BASE_URL}/api/v1/charge/${data.sellerId}`,
    JSON.stringify({ amount: 10 }),
    { headers: { Authorization: `Bearer ${data.sellerToken}`, 'Content-Type': 'application/json' } }
  );

  const exhausted = res.status === 500;
  chargeConflictExhausted.add(exhausted);
  check(res, { 'charge: not exhausted (500)': (r) => r.status !== 500 });
  if (exhausted) console.error(`[charge 500] vu=${__VU} iter=${__ITER} body=${res.body}`);

  const unexpected = res.status !== 200 && res.status !== 500;
  chargeUnexpectedFailure.add(unexpected);
  if (unexpected) console.error(`[charge 기타실패 status=${res.status}] vu=${__VU} iter=${__ITER} body=${res.body}`);

  if (CHARGER_SLEEP > 0) sleep(CHARGER_SLEEP);
}
