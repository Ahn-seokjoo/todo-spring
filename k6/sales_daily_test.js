import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PASSWORD = __ENV.PASSWORD || 'loadtest-pw-1234!';
const SELLER_START = Number(__ENV.SELLER_START || 100); // 100~199번: 시드 기준시각(2026-10-14 00:00) 기준 9/14~10/13 구간에 판매자마다 APPROVED 21건
const SELLER_COUNT = Number(__ENV.SELLER_COUNT || 100);
const VUS = Number(__ENV.VUS || 10);
const DURATION = __ENV.DURATION || '30s';
const FROM = __ENV.FROM || '2026-09-14';
const TO = __ENV.TO || '2026-10-14';
// 비교할 API: daily(원본 todo_purchase 집계) 또는 daily-stats(선계산 통계 테이블)
const ENDPOINT = __ENV.ENDPOINT || 'daily';

export const options = {
    setupTimeout: '180s',
    summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'],
    scenarios: {
        read: { executor: 'constant-vus', vus: VUS, duration: DURATION },
    },
    thresholds: {
        checks: ['rate>0.99'],
        // setup(가입/로그인) 요청을 빼고 조회 요청만의 응답시간을 요약에 따로 찍기 위한 항목
        [`http_req_duration{name:${ENDPOINT}}`]: ['p(99)<60000'],
    },
};

// seed 판매자 가입(이미 있으면 통과) + 로그인 → 토큰 목록
export function setup() {
    const headers = { 'Content-Type': 'application/json' };
    const tokens = [];
    for (let i = 0; i < SELLER_COUNT; i++) {
        const userId = `seed_seller_${String(SELLER_START + i).padStart(5, '0')}`;
        const signupBody = JSON.stringify({ user_id: userId, password: PASSWORD, email: `${userId}@example.com` });
        const loginBody = JSON.stringify({ user_id: userId, password: PASSWORD });

        const signupRes = http.post(`${BASE_URL}/api/v1/auth/signup`, signupBody, { headers, tags: { name: 'setup_signup' } });
        // 이미 가입된 계정(A004_USER_ALREADY_EXIST)만 허용하고, 그 밖의 가입 실패는 원인을 바로 드러내도록 중단
        const signupOk = signupRes.status >= 200 && signupRes.status < 300;
        const alreadyExists = signupRes.status === 400 && String(signupRes.body).includes('A004_USER_ALREADY_EXIST');
        if (!signupOk && !alreadyExists) {
            throw new Error(`signup failed ${userId}: ${signupRes.status} ${signupRes.body}`);
        }
        const res = http.post(`${BASE_URL}/api/v1/auth/login`, loginBody, { headers, tags: { name: 'setup_login' } });
        if (res.status !== 200) throw new Error(`login failed ${userId}: ${res.status} ${res.body}`);
        tokens.push(res.json().accessToken);
    }
    return { tokens };
}

// 무작위 판매자로 30일 통계 조회 (sleep 없음: 이 VU 수로 낼 수 있는 최대 처리량 측정)
export default function (data) {
    const token = data.tokens[Math.floor(Math.random() * data.tokens.length)];
    const res = http.get(`${BASE_URL}/api/v1/sales/${ENDPOINT}?from=${FROM}&to=${TO}`, {
        headers: { Authorization: `Bearer ${token}` },
        tags: { name: ENDPOINT },
    });
    check(res, {
        'status 200': (r) => r.status === 200,
        'non-empty': (r) => r.status === 200 && r.json().length > 0,
    });
}
