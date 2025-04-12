.PHONY: local-up local-down prod-up prod-down restart-local restart-prod

# 공통 명령어
local-up:
	docker compose -f docker-compose.yml -f docker-compose.override.yml up -d

local-down:
	docker compose -f docker-compose.yml -f docker-compose.override.yml down

prod-up:
	docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d

prod-down:
	docker compose -f docker-compose.yml -f docker-compose.prod.yml down

restart-local: local-down local-up
restart-prod: prod-down prod-up
