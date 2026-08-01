# Keenetic RCI API — сводный справочник (для Copilot/любого AI-ассистента)

Это единый источник правды по реальному API роутера, собранный за много
итераций HAR-дампов и живых тестов на роутере KN-2311 (KeeneticOS 4.3).
**Не гадай формат новых команд** — либо ищи здесь, либо проси у автора
проекта HAR/SSH-вывод, прежде чем писать код.

## Как вообще работает API

Два способа достучаться до роутера:

1. **REST**: `POST http://<ip>/rci/` с телом — JSON-массив команд. Каждая
   команда — вложенный объект, повторяющий дерево CLI-команд Keenetic.
   Используется через `KeeneticRestApi.executeRci(commands: List<Map<String,Any>>)`
   в проекте. Ответ — JSON-массив результатов по каждой команде.
2. **SSH**: обычный CLI (`show ...`), доступен через `KeeneticSshClient.execute(command)`.
   Некоторые `show`-команды через SSH отдают **человекочитаемый текст с
   отступами**, не JSON (например `show device-list`) — тогда нужен
   отдельный текстовый парсер (см. `DeviceListParser.kt` как образец).
   Другие `show`-команды через REST отдают чистый JSON даже если через SSH
   выглядят иначе.

**Обязательное правило**: почти все `set`-команды должны завершаться:
```json
{"system":{"configuration":{"save":{}}}}
```
иначе изменения не переживут перезагрузку роутера.

## Степень доверия (используй ту же маркировку в новых находках)
- 🟢 Подтверждено HAR с реального роутера или прямым тестом
- 🟡 Подтверждено только документацией/сторонним проектом, не HAR
- 🔴 Предположение по аналогии, не проверено

---

## Аутентификация
🟢 Challenge/response, см. `RouterRepository.login()`. Реализовано и работает.

## Чтение данных (show)

| Команда | Транспорт | Формат | Даёт |
|---|---|---|---|
| `GET /rci/show/system` | REST | JSON | Версия ОС, CPU, память, аптайм |
| `GET /rci/show/interface` | REST | JSON (объект по id) | Все интерфейсы разом |
| `GET /rci/show/ip/hotspot` | REST | JSON `{"host":[...]}`  | Подключённые клиенты (только онлайн) |
| `GET /rci/show/ip/dhcp/bindings` | REST | JSON 🟡 | DHCP-резервации (вкл. офлайн) |
| `GET /rci/show/associations` | REST | JSON 🟡 | Wi-Fi клиенты с трафиком rx/tx |
| `GET /rci/show/ip/policy` | REST | JSON 🔴 | Список политик (предположительно) |
| `show device-list` | **SSH** | Текст с отступами | ПОЛНЫЙ список устройств (вкл. офлайн) + реальный трафик rx/tx + policy + priority на хосте - лучше, чем show/ip/hotspot |
| `show interface <id> stat` | **SSH** | JSON (даже через SSH!) | rxbytes/txbytes/rxspeed/txspeed - живая скорость |
| `show wans` | **SSH** | JSON (даже через SSH!) | Официальный список активных WAN + резервных (wbk) |
| `show lans` | **SSH** | JSON | Статус по каждому Wi-Fi диапазону на каждом сегменте |
| `show site-survey` (REST: `{"show":{"site-survey":{"name":"WifiMasterX"}}}`) | REST | JSON `ap_cell[]` | Сканирование соседних сетей |
| `{"show":{"sc":{"vpn-server":{}}}}` | REST | JSON (вложенно, искать рекурсивно) | Статус VPN-сервера |
| `{"show":{"sc":{"schedule":{}}}}` | REST | JSON `{name:{description,action[]}}` | Список расписаний |
| `{"show":{"sc":{"ip":{"name-server":{}}}}}` | REST | JSON | Текущие DNS-серверы |
| `show ip policy` (полный, с mark/route4/route6) | SSH | текст | Детали политик маршрутизации |
| `show vpn-server`, `show ip dhcp` (простой CLI) | SSH | ПУСТО на нашей прошивке | Не сработало, не использовать |

## Изменение данных (set) - все требуют system.configuration.save

### Включение/выключение интерфейса (Wi-Fi сеть целиком, WAN, VPN/Proxy)
🟢 Через `mws.wlan` для Wi-Fi сетей (Home/Guest - НЕ для raw AccessPoint id!):
```json
{"mws":{"wlan":{"id":"Home","enable":true}}}
```
🟢 Через `interface.up` для остальных интерфейсов (WAN, VPN/Proxy, WifiStation):
```json
{"interface":{"up":true,"name":"GigabitEthernet0/Vlan4"}}
```
⚠️ Не путать - для Wi-Fi сетей ИМЕННО mws.wlan, raw interface.up на
AccessPoint id не подтверждён и может не работать.

### Пароль/SSID/WPS/изоляция клиентов Wi-Fi сети
🟢 Тот же mws.wlan, частичный патч (можно слать только нужные поля):
```json
{"mws":{"wlan":{"id":"Home","ssid":{"name":"..."},"wpa":{"psk":"..."},"wps":{"enable":bool},"peer-isolation":bool}}}
```

### Блокировка/разблокировка клиента
🟢
```json
{"ip":{"hotspot":{"host":{"mac":"...","access":"permit"|"deny"}}}}
```

### Политика маршрутизации для устройства
🟢 (тот же узел, что и access выше, просто другое поле)
```json
{"ip":{"hotspot":{"host":{"mac":"...","policy":"Policy0"}}}}
```

### Расписание для устройства
🔴 По аналогии с полем policy (структурно похоже, отдельно НЕ подтверждено HAR):
```json
{"ip":{"hotspot":{"host":{"mac":"...","schedule":"schedule0"}}}}
```

### Переименование устройства
🟢
```json
{"known":{"host":{"name":"...","mac":"..."}}}
```

### Создание расписания
🟢
```json
{"schedule":{"name":"schedule0","description":"...","action":[{"action":"start","hour":"22","min":"0","dow":"1"},{"action":"stop","hour":"7","min":"0","dow":"1"}]}}
```
dow: 0=вс...6=сб (обычная конвенция, не проверена отдельно).

### DNS-over-HTTPS
🟢 Список заменяется целиком (сначала "no":true, потом новый список):
```json
[{"dns-proxy":{"https":{"upstream":[{"no":true},{"url":"https://...","hash":"","domain":""}]}}}]
```

### IntelliQoS (приоритезация трафика)
🟢
```json
[{"ntce":{"qos":{"category":[{"category":"calling","priority":1}, ...]}}},
 {"ntce":{"qos":{"enable":true}}},
 {"service":{"ntce":true}}]
```

### Менеджер пакетов (opkg)
🟢
```json
{"opkg":{"disk":{"disk":"OPKG:/","no":false},"initrc":{"path":"1","no":false}}}
```
Плюс `{"user":[{"name":"admin","tag":{"tag":"opt"}}]}`.

### Торрент-клиент
🟢
```json
{"service":{"torrent":true}}
```
Настройка: `{"torrent":{"directory":"OPKG:","rpc-port":{"port":8090,"public":false},"peer-port":51413}}`

### Wi-Fi в режиме клиента (WifiStation, мост/повторитель)
🟢 Полная последовательность на `WifiMaster{0|1}/WifiStation0`:
```
ip.address {no:true, dhcp:true}
description <ssid>
ssid <ssid>
encryption {enable:{no:false}, wpa:{no:true}, wpa2:{no:false}, owe:{no:true}, wpa3:{no:true}}
authentication {wpa-psk:{psk:<pass>}}
up: true
```
Отключение - обратная последовательность с no:true.

### Назначение LAN-портов (интернет/IPTV/VoIP отдельно)
🟢 ⚠️ РИСКОВАННО - ошибка отключает интернет на порту:
```json
{"interface":{"ipoe":{"inet-port":"3","iptv-port":"","voip-port":""},"name":"GigabitEthernet0/Vlan4"}}
```

### Удаление WAN-подключения
🟢
```json
[{"interface":{"ipoe":{"no":true},"name":"..."}},{"interface":{"no":true,"name":"PPPoE0"}}]
```

### Создание L2TP-подключения (полная последовательность)
🟢 Пошагово на новом интерфейсе "L2TP0":
```
ip.address {no:true}  (на старом, если был)
interface (создание/выбор)
connect.via: <родительский интерфейс>
role: ["inet"]
peer: <адрес сервера>
authentication: {identity, password, pap/chap/mschap/mschap-v2: false...}
ipcp.address: true
ip.tcp.adjust-mss.pmtu: true
ip.mtu: "1500"
```

### Правила доступа/файрвол (access-list)
🟢 Но это `/firewall`, НЕ `/portForwarding` (разные страницы сайта!):
```json
[{"access-list":[{"acl":"...","permit":{"index":0,"action":"permit","source":"0.0.0.0","source-mask":"0.0.0.0","destination":"0.0.0.0","destination-mask":"0.0.0.0","protocol":"tcp",...}}]},
 {"interface":{"ip":{"access-group":[{"acl":"...","direction":"in"}]},"name":"..."}}]
```

### Переадресация портов (`/portForwarding`)
❌ НЕ подтверждено - на роутере автора не было создано ни одного правила на
момент снятия HAR. Нужен новый HAR именно момента создания правила.

### Создание нового LAN-сегмента с нуля
❌ Высокий риск, НЕ делать без отдельного HAR именно создания (не
редактирования существующего). Полная форма редактирования УЖЕ существующего
сегмента подтверждена (IP/маска, DHCP пул, NAT, IPv6, политика по
умолчанию, ограничение скорости, mDNS-ретрансляция) - см. `keenetic-web-ui-structure.md`.

---

## Структура веб-интерфейса (для дизайна навигации)
Полная структура (5 групп + все подстраницы с реальными URL) - см. отдельный
файл `keenetic-web-ui-structure.md`, извлечена из реального DOM веб-морды
(`<ndw-menu>`), не догадка.

## Дизайн-система проекта
- Цвета: объект `KeeneticColors` (Primary, Accent, Error, Surface, TextPrimary, TextSecondary, Divider)
- Карточки: `Card` + `CardDefaults.cardColors(containerColor = KeeneticColors.Surface)` + `elevation = 2.dp`
- Рискованные действия (reboot, выключение WAN, назначение портов) - ВСЕГДА подтверждающий `AlertDialog`, не голый тумблер
- Неизвестное текущее состояние на роутере (WPS, изоляция клиентов и т.п.) -
  tri-state паттерн (Не менять / Вкл / Выкл), никогда не слать булево
  значение по умолчанию вслепую - см. `TriStateRow` в `WiFiScreen.kt`
- Большие формы (назначение портов, DoH) - сворачиваемые карточки
  (`expanded` state + кнопка "Настроить"/"Скрыть"), не занимают место, пока не нужны
- Все экраны со скроллящимся контентом - обязательно `LazyColumn` или
  `Column.verticalScroll(rememberScrollState())` - была реальная регрессия
  из-за забытого скролла

## Правила для любого AI-ассистента, работающего над этим проектом
1. Перед новой командой - искать в этом файле. Не нашёл - просить HAR/SSH-вывод у автора, не гадать.
2. Каждое изменение - отдельный коммит с понятным сообщением.
3. `app/build/`, `.gradle/` не должны попадать в git (см. `.gitignore`).
4. Не переписывать целиком работающие файлы без необходимости - точечные правки.
5. Перед рискованными командами (см. пометки ⚠️ выше) - обязательное подтверждение в UI, тестировать на некритичном порту/устройстве.
