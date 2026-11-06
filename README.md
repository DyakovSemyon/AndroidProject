# AndroidProject
Создание приложения, которое позволит контролировать работу участников проекта.

## Сборка

Секреты (Firebase, keystore) не лежат в репозитории — они в Doppler.
Подробный гайд: [docs/SECRETS.md](docs/SECRETS.md).

### 1. Установка допплер в терминале

Для Windows
```powershell
winget install -e --id ezwinports.make
winget install Doppler.doppler
```
Для Arch Linux
```bash
yay -S doppler-cli-bin
```

Для MacOS
```bash
brew install dopplerhq/cli/doppler
```

### 2. Запуск

```bash
doppler login && doppler setup     # project: coflow, config: dev
make secrets                       # разложить файлы для Android Studio
make build                         # сборка
```
