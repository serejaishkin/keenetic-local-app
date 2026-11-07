# Форматы RCI из сторонних проектов (DNS-маршрутизация, FQDN-группы, политики, токены)

Источники (код читал, на своём роутере НЕ проверял):
- hoaxisr/awg-manager — Go, `internal/ndms/{command,query,transport}`; в комментариях стенды KeeneticOS 5.01.C.1, 5.02.A.11, 5.02.B.1.
- Ground-Zerro/HydraRoute (Neo) — C, `Neo/source/src/rci.c`.
- astronaut808/hrbridge — API-обёртка над HydraRoute Neo.

Статус: 🟡 подтверждено кодом других проектов, не нами. Перед включением записи сверить `show`-ответом со своего роутера (снимок RCI).

## Транспорт
- Запись: `POST /rci/` (HTTP), тело — вложенный JSON-объект команды или массив. Чтение: `GET /rci/show/...`.
- Сохранение конфигурации: `{"system":{"configuration":{"save":{}}}}` (≈4–10 с). `show/sc/...` показывает сохранённый вид, `show/rc/...` — рабочий.
- Токен (KeeneticOS 5.2+): заголовок **`X-NDMA-TKN: <токен>`** (не `X-NDM-TOKEN`, как было в догадке по прошивке).
  - Без токена на 5.2 запрос проходит, но в журнале предупреждение «unauthenticated loopback RCI access will be removed soon».
  - Отказ по токену: HTTP 403, заголовок `X-Detail` содержит `0x2312` («not identified») или `0x1218` («token required»).
  - Неверный токен — попытка в lockout роутера.
- Токены выпускаются CLI (`ndmc -c "<команда>"`, локально, без авторизации):
  - `authentication token generate <описание>` → в выводе строка `value: <токен>`
  - `show authentication token` → блоки с полями `id:`, `user-data:` (описание), …
  - `authentication token delete <id>`
  - Токен выдаётся на admin, бессрочный, переживает перезагрузку, но `id` после ребута перенумеровывается: свои токены искать по `user-data`.
  - После генерации HydraRoute делает `system configuration save`.
- Версия: токены есть с 5.02; на 5.01 и ниже команды нет.

## DNS-маршрутизация (KeeneticOS 5, на OS4 эндпоинта нет)
Чтение: `GET /rci/show/sc/dns-proxy/route` и `/show/rc/dns-proxy/route`.
Ответ — массив `{group, interface, auto, reject, index, disable}` либо объект, где ключ — имя группы; пустой — `[]`.
`index` — стабильный хэш содержимого строки.

Добавить/перезаписать маршрут (порядок строк = приоритет):
```json
{"dns-proxy":{"route":[{"group":"NAME","interface":"Wireguard0","auto":true,"reject":true}]}}
```
`reject` только при true; `disable:true` ставит выключенной.
Снести: `{"dns-proxy":{"route":[{"group":"NAME","interface":"Wireguard0","no":true}]}}`
Вкл/выкл по index (двойное отрицание: `no:false` = отключить, `no:true` = включить):
```json
{"dns-proxy":{"route":{"disable":{"index":"<index>","no":false}}}}
```
Переставить: элементы с командой `rule` и позицией с нуля (подробности в `command/dnsroutes.go::MoveRoutes`).
Сносы и записи слать одним POST-батчем: порядок применения элементов по очереди, иначе окно без DNS-маршрутизации.

## FQDN-группы
Чтение: `GET /rci/show/rc/object-group/fqdn` → `{ "имя": {"include":[{"address":"a.com"}], "exclude":[{"address":"b.com"}]} }`.
Правка:
```json
{"object-group":{"fqdn":{"NAME":{"include":[{"address":"x.com"},{"address":"old.com","no":true}],"exclude":[]}}}}
```
Удалить группу: `{"object-group":{"fqdn":{"NAME":{"no":true}}}}`.

## Политики доступа и привязка устройств
- Создать: `{"ip":{"policy":{"NAME":{"description":"..."}}}}`
- Разрешить интерфейс: `{"ip":{"policy":{"NAME":{"permit":{"global":true,"interface":"Wireguard0","order":0}}}}}`
- Автономная (standalone): `{"ip":{"policy":{"NAME":{"standalone":true}}}}` (`{"no":true}` — снять).
- Привязать устройство: `{"ip":{"hotspot":{"host":{"mac":"aa:..","policy":"NAME"}}}}`; отвязать — `"policy":{"no":true}`.
- Чтение: `/show/ip/policy`, `/show/rc/ip/policy`, `/show/rc/ip/hotspot`-аналоги, `/show/rc/ip/{route,static,nat,host,http}`.

## Прочее
- `/show/rc/dns-proxy` — конфиг DNS-прокси целиком (DoT/DoH ищут подстрокой `dot|tls|doh|https`).
- `/show/ndns`, `/show/dns-proxy`, `/show/running-config`.
- `GET /rci/show/ip/policy/<name>/mark` — метка политики (HydraRoute).

## Что это закрывает в нашем роадмапе
- M1 (DNS): экран «DNS-маршрутизация» (группы FQDN → интерфейс/политика) можно строить на этих форматах; фильтры SkyDNS/SafeDNS/NextDNS по-прежнему ждут снимка.
- M6.4 (токены): команды и заголовок известны; формат `show authentication token` — блоки `id:/user-data:/value:` в CLI; JSON-вид ждёт снимка.
