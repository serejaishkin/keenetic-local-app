# Выгруженный веб-интерфейс Keenetic KN-2311 (эталон для переноса)

Каталог `docs/website_ru/` — замороженный словарь выгруженной веб-морды KeeneticOS
(страницы сохранены в `docs/webui/*.html`, тексты извлечены один раз и лежат рядом).

## Как пользоваться
- `webui_texts.txt` — все уникальные русские подписи с каждой страницы (алфавитный порядок на страницу).
  **Не переизвлекать из HTML** — источник правды здесь. Если нужна конкретная страница — искать в этом файле по маркеру `======== <имя>.html`.
- Соответствие «страница сайта ↔ экран приложения» — таблица ниже.

## Карта соответствия (страница сайта → Kotlin-экран)

| Страница веб-морды (файл в docs/webui/) | Экран приложения | Куда добавлено |
|---|---|---|
| Подключения к интернету по Ethernet-кабелю | InternetScreen.kt | PPPoE/PPTP/L2TP, DHCP/IP, MTU, MAC, приоритеты, WISP, диагностика |
| Другие подключения | OtherConnectionsScreen.kt | VPN, PPTP/L2TP/OpenVPN/WireGuard/ZeroTier, приоритеты |
| Подключения по сотовой сети | MobileScreen.kt | статус модема, оператор, APN, AT-команды, TTL, USSD |
| Подключение к интернету через публичную/соседнюю сеть Wi-Fi | WifiRepeaterScreen.kt | WISP 2.4/5 ГГц, SSID, BSSID, статус, сканирование, пароль |
| Доменное имя | DdnsScreen.kt | DDNS-профили, провайдеры |
| Интернет-фильтры | ContentFilterScreen.kt | профили фильтрации |
| DNS | DnsScreen.kt / SettingsScreen.kt | вкладки «Серверы» / «Фильтры», DoH/DoT, перехват DNS |
| Сети Wi-Fi | WiFiScreen.kt | SSID, каналы, мощность, шифрование, клиенты |
| Монитор Wi-Fi | WiFiMonitorScreen.kt | сканирование эфира, сигналы, клиенты по диапазонам |
| Контроль доступа к беспроводной сети | WifiAclScreen.kt | 3 режима (открыть/закрыть/список) |
| Общие параметры Wi-Fi | WifiSystemScreen.kt | страна, мощность, Tethering, Wi-Fi 6 |
| WPS / Mesh Wi-Fi-система | WpsScreen.kt / MwsScreen.kt | WPS-режимы, узлы сети |
| Диагностика | DiagnosticsScreen.kt | Ping / Traceroute / DNS |
| Системный монитор | SystemMonitorScreen.kt | CPU/RAM, версия, серийник, интерфейсы, клиенты |
| Монитор трафика | TrafficMonitorScreen.kt | скорости интерфейсов, топ-5 клиентов |
| Настройки системы | SettingsScreen.kt / FirmwareScreen.kt | автообновление, канал, перезагрузка, расписание, пользователи |
| Менеджер пакетов OPKG | OpkgScreen.kt | диск пакетов, initrc |
| Приоритеты подключений (IntelliQoS) | PrioritiesScreen.kt / IntelliQosScreen.kt | классы трафика, приоритеты |
| Маршрутизация | StaticRoutesScreen.kt | статические маршруты, reject/hide |
| Межсетевой экран | FirewallScreen.kt | правила, статусы включено/выключено |
| Переадресация портов | PortForwardingScreen.kt / UpnpScreen.kt | правила NAT, UPnP |
| Списки клиентов | DevicesScreen.kt | блокировка, лимит, политика, WoL, удаление |
| Клиенты в Wi-Fi-мониторе | DevicesScreen.kt / WiFiMonitorScreen.kt | онлайн/оффлайн, MAC/IP |
| Сегменты локальной сети | LanSegmentsScreen.kt | сегменты, DHCP-пулы, изоляция |
| Пользователи и доступ | UserAccountsScreen.kt | учётки, права, смена пароля |
| Накопители и устройства | UsbStorageScreen.kt | диски, безопасное извлечение, форматирование |
| Приложения | CloudScreen.kt, MediaServerScreen.kt, SmbScreen.kt, ComponentsScreen.kt | облако, DLNA, SMB, компоненты |
| Квота мобильного трафика | MobileTrafficScreen.kt | лимиты и статистика по сети |
| Кабельная диагностика | CableDiagnosticsScreen.kt | длина/состояние кабелей |

Все страницы `docs/webui/*.html` проверены по `webui_texts.txt`; недостающие части
добавляются только если есть поле в модели API (KeeneticRestApi.kt / RouterViewModel.kt).

## Как добавить новый блок по эталону
1. Найти подписи нужной страницы в `webui_texts.txt` (маркер `======== ...`).
2. Найти существующее поле в модели (grep по RouterViewModel.kt / KeeneticRestApi.kt),
   НЕ выдумывать несуществующие поля (это ломало сборку).
3. Добавить блок, скомпилировать `.\gradlew.bat :app:compileDebugKotlin`.
</content>