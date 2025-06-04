import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = 'http://localhost:8080'

const USER_ID = __ENV.USER_ID;
const PASSWORD = __ENV.PASSWORD;

export const options = {
  stages: [
    { duration: '1m', target: 10 }, // Warm-up: 캐시 적재
    { duration: '1m', target: 50 }, // Main: 캐시 hit 여부 성능 측정
    { duration: '1m', target: 0 },  // 종료
  ],
};

export function setup() {
  const payload = JSON.stringify({
    user_id: USER_ID,
    password: PASSWORD,
  });

  const headers = {
    'Content-Type': 'application/json',
  };

  // 1️⃣ 회원가입 요청
  const signupRes = http.post(`${BASE_URL}/api/v1/auth/signup`, payload, { headers });

  // 회원가입 중복일 경우 무시하고 통과 (이미 가입했을 수도 있으니까)
  check(signupRes, {
    'signup success or already exists': (res) =>
      res.status === 200 || res.status === 409 || res.status === 400,
  });

  // 2️⃣ 로그인 요청
  const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, payload, { headers });

  // 상태 코드 체크 먼저
  check(loginRes, {
    'login status is 200': (res) => res.status === 200,
  });

  // 실패한 경우 로그 출력하고 테스트 중단
  if (loginRes.status !== 200) {
    console.error(`❌ Login failed: ${loginRes.status} - ${loginRes.body}`);
    throw new Error('Login failed');
  }

  const accessToken = loginRes.json().accessToken;

  return accessToken;
}

export default function (accessToken) {
  const headers = {
    Authorization: `Bearer ${accessToken}`,
    'Content-Type': 'application/json',
  };

  const res = http.get(`${BASE_URL}/api/v1/todos`, { headers });

  check(res, {
    'todos status is 200': (res) => res.status === 200,
    'todos response time < 100ms': (res) => res.timings.duration < 100,
  });

  sleep(1);
}
