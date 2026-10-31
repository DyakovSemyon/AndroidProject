.PHONY: help secrets build test lint clean

help:
	@grep -E '^[a-z-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  %-10s %s\n", $$1, $$2}'

secrets: ## Скачать секреты из Doppler в локальные файлы
	@./scripts/sync-secrets.sh

build: ## Debug-сборка (секреты берутся из Doppler автоматически)
	@doppler run -- ./gradlew assembleDebug

test: ## Юнит-тесты
	@doppler run -- ./gradlew test

lint: ## Android Lint
	@doppler run -- ./gradlew lint

ci: ## То же, что делает GitHub Actions
	@doppler run -- ./gradlew build --no-daemon

clean: ## Очистка
	@./gradlew clean
