import http from 'k6/http';
import { check, sleep } from 'k6';

// 목록 캐시(todos) 무효화 범위 비교용 부하 테스트
// - readers: 사용자 READERS명이 각자 자기 할 일 목록 첫 페이지를 계속 조회 (캐시가 잘 맞아야 하는 쪽)
// - writer : 다른 사용자 1명(writer)이 초당 WRITE_RATE번 자기 할 일을 수정
// 전체 clear면 writer의 수정마다 readers 전원의 목록 캐시가 지워지고, 사용자별 evict면 writer 본인 것만 지워진다.
//
// 측정 순서
//   1) docker exec -it redis redis-cli CONFIG RESETSTAT
//   2) k6 run k6/cache_evict_test.js
//   3) docker exec -it redis redis-cli INFO stats | grep keyspace
//   적중률 = keyspace_hits / (keyspace_hits + keyspace_misses)
//   (keyspace 수치는 Redis 전체 기준이라, 측정 중에는 충전·구매 등 다른 기능을 쓰지 않는다)

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PASSWORD = __ENV.PASSWORD || 'loadtest-pw-1234!';
const READERS = Number(__ENV.READERS || 50);
const WRITE_RATE = Number(__ENV.WRITE_RATE || 2);       // writer 초당 수정 횟수
const READ_SLEEP = Number(__ENV.READ_SLEEP || 0.1);     // reader 조회 간격(초)
const DURATION = __ENV.DURATION || '30s';
const PAGE_SIZE = 5;
const TODOS_PER_USER = 3;

const HEADERS = { 'Content-Type': 'application/json' };

export const options = {
    setupTimeout: '180s',
    summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'],
    scenarios: {
        readers: {
            executor: 'constant-vus',
            exec: 'read',
            vus: READERS,
            duration: DURATION,
        },
        writer: {
            executor: 'constant-arrival-rate',
            exec: 'write',
            rate: WRITE_RATE,
            timeUnit: '1s',
            duration: DURATION,
            preAllocatedVUs: 2,
        },
    },
    thresholds: {
        checks: ['rate>0.99'],
        // 전체 checks만 보면 수정(30초에 약 60건)이 전부 실패해도 조회(약 1만 3천 건)에 묻혀 통과한다.
        // 수정이 실패하면 캐시 무효화가 안 일어나 적중률이 거짓으로 높게 나오므로, 요청 종류별로 따로 막는다.
        // (setup의 가입 요청은 name:setup 태그라 여기에 섞이지 않음)
        'http_req_failed{name:update}': ['rate<0.01'],
        'http_req_failed{name:list}': ['rate<0.01'],
        // 조회/수정 응답시간을 요약에 따로 찍기 위한 항목 (기준값은 사실상 통과용)
        'http_req_duration{name:list}': ['p(99)<60000'],
        'http_req_duration{name:update}': ['p(99)<60000'],
    },
};

function userId(i) {
    return `cache_user_${String(i).padStart(5, '0')}`;
}

function auth(token) {
    return { headers: { ...HEADERS, Authorization: `Bearer ${token}` } };
}

// 가입(이미 있으면 통과) + 로그인 → 토큰
function loginOrSignup(id) {
    const signup = http.post(`${BASE_URL}/api/v1/auth/signup`,
        JSON.stringify({ user_id: id, password: PASSWORD, email: `${id}@example.com` }),
        { headers: HEADERS, tags: { name: 'setup' } });
    const ok = signup.status >= 200 && signup.status < 300;
    const exists = signup.status === 400 && String(signup.body).includes('A004_USER_ALREADY_EXIST');
    if (!ok && !exists) throw new Error(`signup failed ${id}: ${signup.status} ${signup.body}`);

    const login = http.post(`${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({ user_id: id, password: PASSWORD }),
        { headers: HEADERS, tags: { name: 'setup' } });
    if (login.status !== 200) throw new Error(`login failed ${id}: ${login.status} ${login.body}`);
    return login.json().accessToken;
}

// 사용자마다 할 일을 TODOS_PER_USER개 이상 보장, 첫 할 일 id 반환
function ensureTodos(token) {
    const list = http.get(`${BASE_URL}/api/v1/todos?pageNumber=0&pageSize=${PAGE_SIZE}`, { ...auth(token), tags: { name: 'setup' } });
    if (list.status !== 200) throw new Error(`list failed: ${list.status} ${list.body}`);
    let todos = list.json().todoList;
    for (let i = todos.length; i < TODOS_PER_USER; i++) {
        const res = http.post(`${BASE_URL}/api/v1/todos`, JSON.stringify({ todo: `cache-test-${i}` }),
            { ...auth(token), tags: { name: 'setup' } });
        if (res.status !== 201) throw new Error(`create failed: ${res.status} ${res.body}`);
        todos = todos.concat([res.json()]);
    }
    return todos[0].id;
}

export function setup() {
    // writer는 0번, readers는 1..READERS번 (writer는 목록을 조회하지 않음 → readers의 적중률만 깔끔하게 보임)
    const writerToken = loginOrSignup(userId(0));
    const writerTodoId = ensureTodos(writerToken);

    const readerTokens = [];
    for (let i = 1; i <= READERS; i++) {
        const token = loginOrSignup(userId(i));
        ensureTodos(token);
        readerTokens.push(token);
    }
    // setup 중 할 일 생성으로 생긴 캐시 무효화를 정리하려고 잠깐 대기할 필요는 없음 (측정은 RESETSTAT 이후 수치로 봄)
    return { writerToken, writerTodoId, readerTokens };
}

// reader: VU마다 자기 사용자 고정
export function read(data) {
    const token = data.readerTokens[(__VU - 1) % data.readerTokens.length];
    const res = http.get(`${BASE_URL}/api/v1/todos?pageNumber=0&pageSize=${PAGE_SIZE}`, { ...auth(token), tags: { name: 'list' } });
    check(res, { 'list 200': (r) => r.status === 200 });
    sleep(READ_SLEEP);
}

// writer: 자기 할 일 하나를 계속 수정 (제목만 바꿈)
export function write(data) {
    const res = http.patch(`${BASE_URL}/api/v1/todos/${data.writerTodoId}`,
        JSON.stringify({ todo: `edited-${Date.now()}` }),
        { ...auth(data.writerToken), tags: { name: 'update' } });
    check(res, { 'update 200': (r) => r.status === 200 });
}
