.PHONY: dev

dev:
	@if [ ! -f .env ]; then \
		echo "Error: .env file not found. Copy .env.example to .env and configure values."; \
		echo "  cp .env.example .env"; \
		exit 1; \
	fi
	docker compose up
