.PHONY: local-up local-down prod-up prod-down restart-local restart-prod stack-up stack-down stack-ps

# 공통 명령어
local-up:
	docker compose -f docker-compose.yml -f docker-compose.override.yml up -d

local-down:
	docker compose -f docker-compose.yml -f docker-compose.override.yml down

prod-up:
	docker compose -f docker-compose.yml -f docker-compose.prod.yml pull server
	docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d

prod-down:
	docker compose -f docker-compose.yml -f docker-compose.prod.yml down

restart-local: local-down local-up
restart-prod: prod-down prod-up

stack-up:
	docker swarm init || true
	@set -a; . .env; set +a; docker stack deploy -c docker-compose.yml -c docker-compose.prod.yml todo-stack

stack-down:
	docker swarm leave --force

stack-ps:
	docker stack ps todo-stack
