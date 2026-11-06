#!/usr/bin/env bash
# Кладёт секреты из Doppler в файлы, чтобы Android Studio собирала проект
# без плагинов и лишних телодвижений.
#
#   ./scripts/sync-secrets.sh            # конфиг по умолчанию (dev)
#   ./scripts/sync-secrets.sh prd        # другой конфиг
#
# Идемпотентно: можно гонять сколько угодно раз.
set -euo pipefail
cd "$(dirname "$0")/.."

CONFIG="${1:-}"

need() { command -v "$1" >/dev/null 2>&1 || { echo "нет '$1'. Поставь: $2" >&2; exit 1; }; }
need doppler "brew install dopplerhq/cli/doppler"
need jq "brew install jq"

if ! doppler configure get token --plain >/dev/null 2>&1; then
  echo "Doppler не залогинен. Выполни: doppler login && doppler setup" >&2
  exit 1
fi

ARGS=(--no-file --format json --silent)
[ -n "$CONFIG" ] && ARGS+=(-c "$CONFIG")

SECRETS="$(doppler secrets download "${ARGS[@]}")"

write_b64() { # $1 = ключ секрета, $2 = путь
  local value
  value="$(jq -r --arg k "$1" '.[$k] // empty' <<<"$SECRETS")"
  if [ -z "$value" ]; then
    echo "  skip  $1 (нет в Doppler)"
    return
  fi
  printf '%s' "$value" | base64 -d > "$2"
  echo "  ok    $1 → $2"
}

echo "Doppler → файлы:"
write_b64 GOOGLE_SERVICES_JSON_B64 app/google-services.json
write_b64 KEYSTORE_B64            app/release.keystore

echo "Готово. Android Studio больше ничего не требует."
