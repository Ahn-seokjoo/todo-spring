import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = 'http://localhost:8081';

const USER_ID = __ENV.USER_ID;
const PASSWORD = __ENV.PASSWORD;

export const options = {
    stages: [
        { duration: '1m', target: 50 },
        { duration: '2m', target: 50 },
        { duration: '1m', target: 0 }
    ]
}

export function setup() {
  const payload = JSON.stringify({
    userId: USER_ID,
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

  check(loginRes, {
    'login status is 200': (res) => res.status === 200,
    'received access token': (res) => !!res.json().accessToken,
  });

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
  });

  sleep(1);
}
