<div align="center">

# 📡 Keenetic Local

**Неофициальное Android-приложение для управления роутерами Keenetic**

<p>
  <img src="https://img.shields.io/badge/platform-Android-green?style=flat-square&logo=android" alt="Platform">
  <img src="https://img.shields.io/badge/language-Kotlin-blue?style=flat-square&logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-6200EE?style=flat-square&logo=jetpack-compose" alt="Compose">
</p>

<p>
  <a href="#-функции">Функции</a> •
  <a href="#-скриншоты">Скриншоты</a> •
  <a href="#-сборка">Сборка</a> •
  <a href="#-архитектура">Архитектура</a>
</p>

</div>

---

## 🚀 О проекте

**Keenetic Local** — это нативное Android-приложение, которое позволяет управлять роутерами Keenetic напрямую через локальный REST API (`/rci/`), без облака и без привязки к аккаунту Keenetic DNS.

Приложение работает полностью в локальной сети: авторизация по HTTP Basic Auth, парсинг JSON-ответов роутера, управление Wi-Fi точками доступа, просмотр подключённых устройств и мониторинг состояния системы — всё в одном интерфейсе с Material Design 3.

---

## ✨ Функции

| Экран | Возможности |
|-------|-------------|
| 📊 **Статус** | Модель роутера, загрузка CPU, использование RAM, uptime, hostname |
| 📶 **Wi-Fi** | Список точек доступа (2.4 / 5 GHz), SSID, шифрование, включение/выключение |
| 📱 **Устройства** | Все клиенты hotspot с MAC, IP, hostname; блокировка/разблокировка (в разработке) |
| 🖥️ **Терминал** | SSH-выполнение команд (в разработке) |
| ⚙️ **Настройки** | IP роутера, логин, пароль, автовход |

### Ключевые особенности

- ⚡ **Batch-запросы** — система, интерфейсы и клиенты загружаются одним HTTP-запросом вместо трёх
- 🔒 **Автологин** — сохранение учётных данных в DataStore с автоматическим подключением
- 🎨 **Material 3** — современный UI на Jetpack Compose с динамическими цветами
- 🏠 **Локально** — никаких облаков, всё общение идёт напрямую с роутером по IP

---

## 📸 Скриншоты

<div align="center">
  <table>
    <tr>
      <td align="center"><b>Статус роутера</b></td>
      <td align="center"><b>Wi-Fi сети</b></td>
      <td align="center"><b>Устройства</b></td>
    </tr>
    <tr>
      <td><img src="docs/screenshots/status.png" width="240" alt="Dashboard"/></td>
      <td><img src="docs/screenshots/wifi.png" width="240" alt="Wi-Fi"/></td>
      <td><img src="docs/screenshots/devices.png" width="240" alt="Devices"/></td>
    </tr>
  </table>
</div>

> 💡 *Скриншоты актуальны для текущей ветки `feature/auth-debug`.*

---

## 🛠️ Технологии

- **Kotlin** — 100% Kotlin Coroutines + Flow
- **Jetpack Compose** — декларативный UI
- **Material Design 3** — компоненты и темы
- **Retrofit 2** + **OkHttp** — REST API роутера
- **Gson** — парсинг JSON-ответов Keenetic RCI
- **DataStore** — хранение настроек (IP, логин, пароль, флаги)
- **MVVM** — `ViewModel` + `StateFlow`
- **CookieJar** — сохранение сессии авторизации роутера

---

## 📋 Требования

- Android 8.0+ (API 26)
- Роутер Keenetic с прошивкой, поддерживающей RCI API (`/rci/show/system`, `/rci/show/interface`, `/rci/show/ip/hotspot`)
- Доступ к роутеру по локальной сети (IP + пароль админки)

---

## 🔧 Сборка

```bash
# Клонируй репозиторий
git clone https://github.com/serejaishkin/keenetic-local-app.git
cd keenetic-local-app

# Собери debug APK
./gradlew assembleDebug

# Установи на устройство
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Gradle + JDK

Проект использует Gradle Wrapper. Убедись, что установлен JDK 17+:

```bash
java -version
```

---

## 🏗️ Архитектура

```
com.keenetic.local
├── api/                    # Retrofit, репозиторий, модели данных
│   ├── KeeneticApi.kt      # Интерфейс REST API (batch + отдельные запросы)
│   ├── RouterRepository.kt # Инициализация Retrofit, cookie-jar, авторизация
│   └── RouterModels.kt     # SystemInfo, Client, InterfaceInfo, WifiNetwork
├── data/
│   └── DataStoreManager.kt # Хранение IP, логина, пароля, флагов
├── discovery/
│   └── AutoDiscovery.kt    # Поиск роутеров в локальной сети
├── ui/
│   ├── RouterViewModel.kt  # MVVM: парсинг JSON, batch-загрузка, действия
│   └── screens/
│       ├── DashboardScreen.kt
│       ├── WiFiScreen.kt
│       ├── DevicesScreen.kt
│       ├── TerminalScreen.kt
│       └── SettingsScreen.kt
└── MainActivity.kt         # Compose Navigation + BottomBar
```

### Поток данных

```
Пользователь → Compose UI → ViewModel → Repository → Retrofit → Keenetic Router
                    ↑            ↓
              StateFlow    DataStore (настройки)
```

---

## 🔐 API роутера

Приложение использует внутренний RCI API Keenetic:

```http
POST /rci/ HTTP/1.1
Content-Type: application/json

[
  {"show": {"system": {}}},
  {"show": {"interface": {"details": "yes"}}},
  {"show": {"ip": {"hotspot": {}}}}
]
```

Ответ — массив из 3 JSON-объектов, которые парсятся в модели приложения.

---

## 🗺️ Roadmap

- [x] Авторизация по HTTP Basic Auth
- [x] Batch-запросы (`show system`, `show interface`, `show ip hotspot`)
- [x] Экран статуса (CPU, RAM, uptime)
- [x] Экран Wi-Fi с точками доступа
- [x] Экран устройств (hotspot clients)
- [x] Блокировка клиентов по MAC
- [ ] Перезагрузка роутера из приложения
- [ ] SSH-терминал
- [ ] Тёмная тема
- [ ] Автообнаружение роутера в сети
- [ ] Редактирование SSID и пароля Wi-Fi
- [ ] Резервное копирование настроек роутера

---

## 🤝 Участие в проекте

Проект открыт для PR. Если нашёл баг или хочешь новую фичу — создавай Issue.

---

## 📄 Лицензия

MIT License — свободное использование, модификация и распространение.

---

<div align="center">

**Сделано с ❤️ для локальных сетей**

</div>
