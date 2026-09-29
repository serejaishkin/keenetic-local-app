# Дорожная карта переноса веб-морды Keenetic KN-2311 в приложение

Метод: проходим выгруженные страницы сайта (docs/website_ru/webui_texts.txt) ПО ПОРЯДКУ.
Для каждой страницы: → открываем соответствующий Kotlin-экран → сверяем (чего нет) →
добавляем недостающее из эталона → компилируем. После всех страниц — полная сборка + APK + тест.

## Очерёдность страниц (по файлам webui)

### Этап A — подключения
1. Подключения к интернету по Ethernet-кабелю → `InternetScreen.kt`
2. Другие подключения → `OtherConnectionsScreen.kt`
3. Подключения к интернету через сотовую сеть → `MobileScreen.kt`
4. Подключения по Ethernet-кабелю (интернет) → (внутри InternetScreen)

### Этап B — Wi-Fi
5. Сети Wi-Fi → `WiFiScreen.kt`
6. Монитор Wi-Fi → `WiFiMonitorScreen.kt`
7. Контроль доступа к беспроводной сети → `WiFiAccessScreen.kt`
8. WPS → `WpsScreen.kt`
9. Wi-Fi-система (Mesh) → `WiFiMeshScreen.kt` (проверить наличие)

### Этап C — DNS и фильтры
10. Доменное имя (DDNS) → `DdnsScreen.kt`
11. Интернет-фильтры → `DnsFiltersScreen.kt`
12. Настройка DNS → `DnsScreen.kt` (вкладки Серверы/Фильтры)

### Этап D — сети и правила
13. Сегменты локальной сети → (проверить, возможно в WiFiScreen)
14. Маршрутизация → `RoutingScreen.kt`
15. Межсетевой экран → `FirewallScreen.kt`
16. Переадресация портов → `PortForwardingScreen.kt`
17. Приоритеты подключений → `PrioritiesScreen.kt` (проверить)

### Этап E — служебные
18. Системный монитор → `SystemMonitorScreen.kt`
19. Монитор трафика → `TrafficMonitorScreen.kt`
20. Диагностика → `DiagnosticsScreen.kt`
21. Настройки системы → `SettingsScreen.kt`

## Критичное правило
- В ЭКРАН добавляем ТОЛЬКО поля, которые есть в модели API (проверять перед каждой правкой:
  grep по `InterfaceMapper.kt` / `KeeneticConfigParser.kt` / `RouterViewModel.kt`).
- Каждый квант → `:app:compileDebugKotlin` → BUILD SUCCESSFUL обязателен.
- Использовать только латиницу в bash-командах (кириллица ломает консоль).
- Источник правды — `docs/website_ru/webui_texts.txt`, НЕ переизвлекать из HTML.

## Этап G — вёрстка разделов «как на сайте», удобно с телефона (NEW)

Задача: содержимое каждого раздела привести к виду соответствующей страницы
веб-конфигуратора (те же блоки, поля, порядок, подписи из `locale.ru.json`),
но в мобильной вёрстке. RCI-команды НЕ менять — уже сверены на этапах A–F.

### G0. Общие мобильные правила (применяются ко всем экранам)
- Один заголовок экрана с одной кнопкой «назад» (убрать двойные шапки вида
  «DNS-фильтры» + «DNS / Secure DNS»).
- Блоки сайта → карточки-секции; длинные списки → `LazyColumn`, таблицы сайта →
  вертикальные ряды «подпись — значение».
- Редактирование — через диалог/нижний шит с кнопками Сохранить/Отмена
  (как на сайте), а не кучей полей на экране.
- Тап-зоны минимум 48dp; свитчи/кнопки справа, как в веб-морде.
- Пустые/неподдержанные данные — текст-заглушка, а не нули («—», «нет данных»).
- После каждого экрана: compile + скриншот на телефоне, сверка с эталоном HTML.

### G1. Главная и устройства
- [ ] `DashboardScreen.kt` ← «Системный монитор.html»: статус, CPU/RAM, сеть
- [ ] `DevicesScreen.kt` + `DeviceListDetailedScreen.kt` ← «Списки клиентов.html»:
  карточка клиента как строка таблицы сайта, действия — в диалоге
- [ ] `SystemMonitorScreen.kt`, `TrafficMonitorScreen.kt`, `NetworkMonitorScreen.kt`:
  блоки как на сайте, conntrack/ARP уже есть — причесать ряды

### G2. Интернет
- [ ] `InternetScreen.kt` + `InternetDetailedScreen.kt` ← «...по Ethernet-кабелю.html»:
  карточка подключения = строка сайта (имя, тип, IP, шлюз, DNS), настройки — диалог
- [ ] `OtherConnectionsScreen.kt` ← «Другие подключения.html»
- [ ] `MobileScreen.kt` ← «...через сотовую сеть.html»
- [ ] `MobileTrafficScreen.kt` ← «Квота мобильного трафика.html»
- [ ] `WifiRepeaterScreen.kt` ← «...публичную_соседнюю сеть Wi-Fi.html»
- [ ] `VpnServersScreen.kt`, `VpnAdvancedScreen.kt` ← «Приложения.html» (VPN-часть)
- [ ] `StaticRoutesScreen.kt` ← «Маршрутизация.html»: вкладки IPv4/IPv6/DNS как на сайте
- [ ] `FirewallScreen.kt` ← «Межсетевой экран.html», `PortForwardingScreen.kt` ←
  «Переадресация портов.html», `UpnpScreen.kt`: правила — ряды + диалог редактора
- [x] `DnsScreen.kt` ← раздел «DNS» на KN-2311: одна шапка, карточки «Перехват
  DNS-запросов» / «DNS-серверы» (DoH, DoT, обычные) / «Фильтрация DNS-запросов»,
  добавление и правка через диалог, удаление с подтверждением.
  `DnsFiltersScreen.kt` и DNS-карточки из `SettingsScreen.kt` удалены как дубли,
  маршрут `dns_filters` убран. Добавлены `updateDohServer`/`updateDotServer`
  (одна запись вместо delete+add), plain-DNS теперь читается из того же узла,
  в который пишется (`ip/name-server`).
- [ ] `ContentFilterScreen.kt`, `DdnsScreen.kt`, `ObjectGroupScreen.kt`,
  `Ipv6Screen.kt` ← «Доменное имя / Интернет-фильтры»: убрать двойные шапки,
  данные — рядами, правка — в диалоге

### G3. Wi-Fi
- [ ] `WiFiScreen.kt` ← «Сети Wi-Fi.html»: сеть = карточка с SSID/шифрованием/клиентами
- [ ] `WifiSystemScreen.kt` ← «Общие параметры Wi-Fi» (тексты из locale): радиомодули
  и точки доступа — отдельные аккордеоны
- [ ] `WifiAclScreen.kt` ← «Контроль доступа к беспроводной сети.html»
- [ ] `WpsScreen.kt`, `WiFiMonitorScreen.kt` ← «Монитор Wi-Fi.html», `MwsScreen.kt` ←
  «Mesh Wi-Fi-система.html»

### G4. Система и сервисы
- [ ] `SystemAdvancedScreen.kt` + `FirmwareScreen.kt` ← «Настройки системы.html»
- [ ] `DiagnosticsScreen.kt` + `CableDiagnosticsScreen.kt` + `SystemLogsScreen.kt` ←
  «Диагностика.html»
- [ ] `SshSnmpScreen.kt`, `UserAccountsScreen.kt` ← «Пользователи и доступ.html»
- [ ] `UsbStorageScreen.kt` + `FileBrowserScreen.kt` + `SmbScreen.kt` +
  `MediaServerScreen.kt` ← «Накопители и устройства.html»
- [ ] `TorrentDetailScreen.kt` + `ComponentsScreen.kt` + `OpkgScreen.kt` +
  `CloudScreen.kt` ← «Приложения.html»
- [ ] `PrioritiesScreen.kt` + `IntelliQosScreen.kt` ← «Приоритеты подключений.html» +
  «IntelliQoS.html»
- [ ] `LanSegmentsScreen.kt` ← «Сегменты локальной сети.html»
- [ ] `LoginScreen.kt`, `AllSectionsScreen.kt`, `ConfigurationScreen.kt`,
  `SshTerminalScreen.kt` — служебные, причесать последними

## Состояние
- ✅ Этапы A–F: RCI READ/WRITE сверены, сборка и unit-тесты зелёные, APK на телефоне,
  живые данные подтверждены (KN-2311, KeeneticOS 5.1.5, LIVE RCI).
- 🔄 Этап G (вёрстка «как на сайте»): открыт, переделан `DnsScreen.kt` (раздел «DNS»).
- ⏳ Проверка каждого экрана Этапа G — скриншотом на подключённом телефоне.
  DNS проверен на живых данных: 7 DoH-серверов, 3 DoT, перехват выключен,
  пресеты/профили фильтрации пустые, диалог добавления обычного сервера открывается.

## Состояние
