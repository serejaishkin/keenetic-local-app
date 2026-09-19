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
| 📱 **Устройства** | Все клиенты hotspot с MAC, IP, hostname; блокировка/разблокировка |
| 🖥️ **Терминал** | SSH-выполнение команд (в разработке) |
| ⚙️ **Настройки** | IP роутера, логин, пароль, автовход |

### Ключевые особенности

- ⚡ **Batch-запросы** — система, интерфейсы и клиенты загружаются одним HTTP-запросом вместо трёх
- 🔒 **Автологин** — сохранение учётных данных в DataStore с автоматическим подключением
- 🎨 **Material 3** — современный UI на Jetpack Compose с динамическими цветами
- 🏠 **Локально** — никаких облаков, всё общение идёт напрямую с роутером по IP

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

### ✅ Сделано

- [x] Авторизация (Challenge-Response, привязка к сессии)
- [x] Batch-запросы (`show system`, `show interface`, `show ip hotspot`, …)
- [x] Экран статуса (CPU, RAM, uptime)
- [x] Экран Wi-Fi: точки доступа, SSID, пароль, каналы, мощность, изоляция
- [x] Экран устройств (hotspot clients) и блокировка клиентов по MAC
- [x] Перезагрузка роутера (RCI + SSH fallback через JSch)
- [x] Автообнаружение роутера в сети (шлюз + подсказки IP)
- [x] Тёмная тема (приложение тёмное по умолчанию)
- [x] Дублирование основных разделов веб-интерфейса Keenetic (KN-2311)
- [x] DDNS, торрент-клиент, OPKG, SMB/DLNA, USB-накопители
- [x] DNS / Secure DNS: DoH (7 серверов), DoT (4 сервера), перехват, plain DNS
- [x] LAN-порты: статус, скорость, дуплекс (из `show/interface`)
- [x] Переадресация портов (NAT), статическая маршрутизация, LAN-сегменты
- [x] Мониторинг: Wi-Fi, трафик, сеть (conntrack/ARP), системный монитор
- [x] Пользователи и доступ, смена пароля администратора
- [x] Конфигурация (RCI / CLI): running-config, инспектор, команды NDM
- [x] Журнал событий, диагностика сети (ping/traceroute/DNS), диагностика кабеля
- [x] VPN: серверы (PPTP/L2TP/SSTP/OpenConnect/WireGuard/IKEv2), клиенты, переключение
- [x] Приоритеты подключений и приоритеты IntelliQoS
- [x] SSH-терминал командный (JSch): исполнение команд, вывод в лог
- [x] Резервная копия: создание (`system.backup`) и скачивание в Downloads
- [x] Включение/выключение LAN-портов (`interface.{port}.up`, проверено на KN-2311)
- [x] WPS: PBC-кнопка, авто-PIN, статусы WPS по WLAN/диапазонам (`show/mws/wlan`)
- [x] Mesh (MWS): включение/выключение WLAN 2.4/5 ГГц (`mws.wlan[{id,enable}]`)
- [x] Установка/удаление компонентов через RCI (`components.component`, карточка в SSH/SNMP)

### 🚧 В работе / частично

- [ ] SSH-терминал: сейчас командный (выполнение команд сразу, вывод в лог), нужен интерактивный PTY
- [ ] Резервная копия: создание и скачивание работают (RCI `system.backup` + `downloadRaw`); восстановление не поддерживается прошивкой (`POST /backup` → 405, `GET /backup` отдаёт HTML)
- [ ] WPS: PBC-подключение, авто-PIN и включение WLAN (mesh) работают; ручной ввод PIN клиента не проверен на живом железе
- [ ] Установка компонентов SMB/DLNA: карточка в «SSH/SNMP» есть, но загрузка пакетов через OPKG может требовать ручных шагов

### ⚠️ Не поддерживается API роутера (KN-2311, fw 5.01.C.4.0-1)

Эти разделы показывают пояснение «раздел не поддерживается» вместо пустого экрана:

- **Межсетевой экран** — `show/ip/rule` отсутствует
- **UPnP / NAT-PMP** — `show/upnp/redirect` отсутствует
- **Контроль доступа Wi-Fi (MAC ACL)** — `show/sc/interface/mac.access-list` отсутствует
- **SMB/CIFS** — компонент не установлен (управление только через OPKG)
- **Мобильный интернет** — `show/mobile`, `show/sim` отсутствуют (нет USB-модема)

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
