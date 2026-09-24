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

## Состояние
- ✅ docs/website_ru/ создан (README + webui_texts.txt, 17 страниц)
- ✅ InternetScreen.kt — PPPoE/PPTP/L2TP, DHCP/статик, MTU, MAC, приоритеты, метод подлинности
- ✅ DNS — вкладки «Серверы/Фильтры» в DnsScreen.kt
- 📍 В работе: экран Интернет (сверка с Ethernet-страницей)
