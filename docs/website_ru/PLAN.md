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
- [x] `DashboardScreen.kt` ← «Системный монитор.html»: статус, CPU/RAM, сеть.
  Карточка «Устройство» сведена к короткому блоку со ссылкой на `SystemMonitorScreen`
  вместо дублирования спецификации.
- [x] `DevicesScreen.kt` + `DeviceListDetailedScreen.kt` ← «Списки клиентов.html»:
  карточка клиента = строка таблицы сайта (`ClientRow`: имя, IP, MAC, политика,
  фикс. IP, лимит скорости, диапазон, скорость, блокировка), тап открывает диалог
  параметров. Действия (WoL, блокировка, удаление) убраны из строки в диалог,
  удаление — через `ConfirmDialog`. `DeviceListDetailedScreen.kt` делит список на
  «Подключены» / «Не в сети».
- [x] `SystemMonitorScreen.kt`, `TrafficMonitorScreen.kt`, `NetworkMonitorScreen.kt`:
  блоки как на сайте, conntrack/ARP причёсаны в ряды «подпись — значение».

### G2. Интернет
- [x] `InternetScreen.kt` + `InternetDetailedScreen.kt` ← «...по Ethernet-кабелю.html»:
  карточка подключения = строка сайта (имя, тип, IP, шлюз, DNS). Подключения
  сгруппированы (проводные / сотовая / публичные сети / прочие), каждый параметр —
  `EditableRow` с формой: IP-адрес (DHCP/статика), параметры подключения
  (описание, MTU, hostname, приоритет, режим MAC), PPPoE (логин, пароль, сервис,
  метод авторизации). Порты LAN — `SwitchRow` с `setPortUp`.
- [x] `SystemAdvancedScreen.kt` ← «Настройки системы.html»: имя хоста, NTP (свитч,
  сервер, часовой пояс), индикаторы LED (свитч + выбор режима), состояние,
  резервная копия. Всё редактирование — через `FormDialog`/`OptionPickerDialog`.
- [x] `OtherConnectionsScreen.kt` ← «Другие подключения.html»: карточка подключения —
  состояние, IP, сервер, свитч включения и кнопка настроек; тип подключения и
  перечисляемые параметры выбираются через `OptionPickerDialog`.
- [x] `MobileScreen.kt` ← «...через сотовую сеть.html»: статус модема карточкой,
  режим работы — `OptionPickerDialog`, APN/USSD — `FormDialog`.
- [x] `MobileTrafficScreen.kt` ← «Квота мобильного трафика.html»: лимит, SMS и сброс
  счётчика — карточки, поля — `EditableRow` → `FormDialog`, свитчи пишут сразу.
- [x] `WifiRepeaterScreen.kt` ← «...публичную_соседнюю сеть Wi-Fi.html»: WISP-строка,
  список найденных сетей и состояние станции; ключ списка — `bssid` (поля `mac`
  в модели нет).
- [x] `VpnServersScreen.kt`, `VpnAdvancedScreen.kt` ← «Приложения.html» (VPN-часть):
  выбор протокола чипами, карточка протокола с `SwitchRow`/`EditableRow`;
  VPN-сервер и WireGuard-пиры — read-only карточки.
- [x] `StaticRoutesScreen.kt` ← «Маршрутизация.html»: вкладки IPv4/IPv6/DNS чипами,
  маршруты — карточки с `InfoRow`/`SwitchRow`, редакторы на `FormDialog`,
  выбор интерфейса и списка доменов — `OptionPickerDialog`.
- [x] `FirewallScreen.kt` ← «Межсетевой экран.html», `PortForwardingScreen.kt` ←
  «Переадресация портов.html», `UpnpScreen.kt`: правила — ряды + диалог редактора;
  `Ipv6Screen.kt`, `ObjectGroupScreen.kt`, `DdnsScreen.kt`, `ContentFilterScreen.kt`
  переведены на общие карточки, `FirewallScreen`/`UpnpScreen` без свитчей
  включения (в модели нет read/write-состояния).
- [x] `DnsScreen.kt` ← раздел «DNS» на KN-2311: одна шапка, карточки «Перехват
  DNS-запросов» / «DNS-серверы» (DoH, DoT, обычные) / «Фильтрация DNS-запросов»,
  добавление и правка через диалог, удаление с подтверждением.
  `DnsFiltersScreen.kt` и DNS-карточки из `SettingsScreen.kt` удалены как дубли,
  маршрут `dns_filters` убран. Добавлены `updateDohServer`/`updateDotServer`
  (одна запись вместо delete+add), plain-DNS теперь читается из того же узла,
  в который пишется (`ip/name-server`).

### G3. Wi-Fi
- [ ] `WiFiScreen.kt` ← «Сети Wi-Fi.html»: сеть = карточка с SSID/шифрованием/клиентами
- [ ] `WifiSystemScreen.kt` ← «Общие параметры Wi-Fi» (тексты из locale): радиомодули
  и точки доступа — отдельные аккордеоны
- [ ] `WifiAclScreen.kt` ← «Контроль доступа к беспроводной сети.html»
- [ ] `WpsScreen.kt`, `WiFiMonitorScreen.kt` ← «Монитор Wi-Fi.html», `MwsScreen.kt` ←
  «Mesh Wi-Fi-система.html»

### G4. Система и сервисы
- [x] `SystemAdvancedScreen.kt` ← «Настройки системы.html» (сделано в G2, отмечено здесь)
- [ ] `FirmwareScreen.kt` ← «Настройки системы.html» (обновление прошивки)
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
- 🔄 Этап G (вёрстка «как на сайте»): G0/G1/G2 закрыты, открыт G3 (Wi-Fi).
- ⏳ Проверка каждого экрана Этапа G — скриншотом на подключённом телефоне.
  DNS проверен на живых данных: 7 DoH-серверов, 3 DoT, перехват выключен,
  пресеты/профили фильтрации пустые, диалог добавления обычного сервера открывается.
- ✅ G0: создан общий UI-kit `ui/components/SectionUi.kt` (`SectionScaffold`, `SectionCard`,
  `InfoRow`, `EditableRow`, `SwitchRow`, `SubGroupHeader`, `RowDivider`, `EmptyHint`,
  `ConfirmDialog`, `DialogForm`, `FormDialog`, `OptionPickerDialog`). Устранена двойная
  шапка: `MainActivity` рисует `TopAppBar` только на корневых вкладках, детальные разделы —
  свою шапку через `SectionScaffold`.
- ✅ G1: `DashboardScreen.kt`, `DevicesScreen.kt`, `DeviceListDetailedScreen.kt`,
  `SystemMonitorScreen.kt`, `TrafficMonitorScreen.kt`, `NetworkMonitorScreen.kt`
  переведены на общие компоненты, дубли заголовков и дубли карточек убраны.
- ✅ G2: переведены на общие компоненты `InternetScreen.kt`, `InternetDetailedScreen.kt`,
  `SystemAdvancedScreen.kt`, `OtherConnectionsScreen.kt`, `MobileScreen.kt`,
  `MobileTrafficScreen.kt`, `WifiRepeaterScreen.kt`, `VpnServersScreen.kt`,
  `VpnAdvancedScreen.kt`, `StaticRoutesScreen.kt`, `PortForwardingScreen.kt`,
  `FirewallScreen.kt`, `UpnpScreen.kt`, `Ipv6Screen.kt`, `ObjectGroupScreen.kt`,
  `DdnsScreen.kt`, `ContentFilterScreen.kt`; `DropdownMenu`/`ExposedDropdownMenuBox`
  и кнопки «Сохранить настройки» заменены на `EditableRow` → `FormDialog`
  и `OptionPickerDialog`.
- ✅ `DnsScreen.kt` снова собирается из чистого checkout: блок фильтрации возвращён
  как `DnsFilterContent.kt` (`DnsSectionHeader`, `dnsFilterItems`, `DnsFilterUiState`),
  его карточки переименованы в `DnsContentPresetCard`/`DnsContentProfileCard`,
  чтобы не конфликтовать с одноимёнными composable из параллельной работы.
- ✅ Проверено в отдельном worktree на чистом HEAD: `compileDebugKotlin`, unit-тесты
  RCI и `assembleDebug` — BUILD SUCCESSFUL без незакоммиченных файлов.
- ⏳ Сборка и unit-тесты зелёные; APK собран. Проверка на телефоне отложена (устройство
  отключено). Брандмаур Dr.Web блокировал localhost-соединение с Gradle daemon — с его
  отключением сборка проходит, временных правок в `gradle.properties` не осталось.
