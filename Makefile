.PHONY: dev run test pre-commit-install

dev:
	@if [ ! -f .env ]; then \
		echo "Error: .env file not found. Copy .env.example to .env and configure values."; \
		echo "  cp .env.example .env"; \
		exit 1; \
	fi
	docker compose up

run:
	@if [ ! -f .env ]; then \
		echo "Error: .env file not found. Copy .env.example to .env and configure values."; \
		echo "  cp .env.example .env"; \
		exit 1; \
	fi
	docker compose up -d db
	@echo "Starting backend..."
	export $$(grep -v '^#' .env | xargs) && ./mvnw spring-boot:run

test:
	./mvnw test
	./scripts/validate-vendor-stub-docs.sh

pre-commit-install:
	pre-commit install
