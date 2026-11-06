# Секреты: Doppler → GitHub Actions → Android Studio

> Правило №1: **в репозитории нет ни одного секрета.** `google-services.json`, keystore
> и ключи живут только в Doppler и попадают в сборку через переменные окружения.
> Правило №2: **в Doppler только single-line значения.** Файлы передаём в base64 —
> так они не ломают `$GITHUB_ENV`, `doppler run` и shell-кводраты.

---

## 1. Схема

```
                 ┌───────────────┐
                 │    Doppler    │   project: coflow
                 │  dev / prd    │   единственный источник правды
                 └───────┬───────┘
        ┌────────────────┴─────────────────┐
        │                                  │
  doppler run / sync-secrets.sh      DOPPLER_TOKEN (GitHub Secret)
        │                                  │
        ▼                                  ▼
  локальная машина                  GitHub Actions
  android studio                    ./gradlew build
```

Переменные окружения (все — base64 или обычные строки):

| Ключ | Что это |
|---|---|
| `GOOGLE_SERVICES_JSON_B64` | `base64 < app/google-services.json` |
| `KEYSTORE_B64` | `base64 < release.keystore` (нужен для release-сборки) |
| `KEYSTORE_PASSWORD` | пароль хранилища |
| `KEY_ALIAS` | алиас ключа (`coflow`) |
| `KEY_PASSWORD` | пароль ключа (если отличается) |

Gradle-таск `:app:materializeSecrets` раскладывает их в файлы **перед** сборкой.

---

## 2. Один раз: завести Doppler (5 минут)

```bash
brew install dopplerhq/cli/doppler
doppler login          # откроется браузер
```

В веб-панели (dashboard.doppler.com):

1. **Create project** → имя `coflow`.
2. Конфиги `dev` и `prd` создаются автоматически (free-план = 3 конфига).
3. В конфиге `dev` → **Add Secret**:

   ```bash
   # значение для GOOGLE_SERVICES_JSON_B64
   base64 < /Users/mordvn/Downloads/google-services.json | tr -d '\n'
   ```

   ```bash
   # значение для KEYSTORE_B64 (когда дойдёт до release)
   base64 < release.keystore | tr -d '\n'
   ```

4. То же самое продублировать в `prd` (для релизных сборок).

Локально в корне репозитория:

```bash
doppler setup          # читает .doppler.yaml → project coflow / config dev
```

---

## 3. Один раз: GitHub Actions (2 минуты)

1. Doppler → проект `coflow` → **Access** → **Service Tokens** → **Generate**.
   Имя `github-actions`, config `dev`, права **Read Only**.
   Скопировать токен (`dp.st.dev.xxxxx`).
2. GitHub → репозиторий → **Settings → Secrets and variables → Actions → New repository secret**:
   - Name: `DOPPLER_TOKEN`
   - Value: скопированный токен.
3. Всё. `.github/workflows/push.yml` сам делает `doppler secrets download --format json`
   и складывает результат в `$GITHUB_ENV`.

> Для релизного пайплайна — второй токен на конфиг `prd` (secret `DOPPLER_TOKEN_PRD`).

---

## 4. Каждый день

**Локально (рекомендуется):**

```bash
make build          # = doppler run -- ./gradlew assembleDebug
make test
```

**Локально в Android Studio** (IDE не знает про Doppler, поэтому файлы на диске):

```bash
make secrets        # ./scripts/sync-secrets.sh → app/google-services.json
```

**В CI:** ничего. Push проверяет реальные секреты из Doppler, а PR собирается
с mock-конфигом `.github/mock-google-services.json` и Firebase Emulator Suite —
поэтому PR из форка тоже проходит полную сборку и инструментальные тесты.

---

## 5. Ротация и онбординг

- Утёк `google-services.json`? Firebase Console → Project settings → новое приложение
  (или regenerate key) → обновить `GOOGLE_SERVICES_JSON_B64` в Doppler → CI подхватит сам.
- Новый человек в команде: `doppler login`, `git clone`, `doppler setup`, `make build`.
  Ему не нужно просить файлы в личку. Увольняется — убираешь его из Doppler-воркспейса,
  и он теряет доступ ко всем секретам сразу.

---

## 6. Что делать, если сломалось

| Симптом | Причина | Лечение |
|---|---|---|
| `google-services.json отсутствует, а переменные Doppler не заданы` | гоняешь `./gradlew` без `doppler run` | `make build` или `make secrets` |
| `DOPPLER_TOKEN недоступен — проверка реальных секретов пропущена` | PR из форка (секреты не отдаются) | норма; сборка идёт с mock-конфигом и эмуляторами |
| `Invalid base64` | в секрет попал перенос строки | `base64 < file \| tr -d '\n'` |
| Release падает на подписи | нет `KEYSTORE_B64`/паролей в конфиге | добавить в Doppler |

---

## 7. Почему именно так (для код-ревью)

- **12-factor**: конфиг в окружении, не в коде → одинаково локально и в CI.
- **base64 для файлов**: `$GITHUB_ENV` и `doppler run` падают на многострочных значениях.
- **Gradle-таск, а не «положи файл руками»**: любой `./gradlew` в CI или на новой машине
  работает из коробки, ошибка воспроизводима и понятна.
- **`inputs.property` на env**: configuration cache инвалидируется честно, сборка не
  хватает старый конфиг из кэша.
