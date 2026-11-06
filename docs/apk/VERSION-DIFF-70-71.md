# Оригинальное приложение: 7.0 (360 Beta) → 7.1 (com.keenetic.kn, versionCode 362)
Пакет приложения сменился на `com.keenetic.kn`, код `com.ndmsystems.*` прежний. Файлов: 3588 → 3591 (+24 новых, 21 убран).
Команд RCI (уникальных цепочек): 420 → 405. Новых путей RCI: один. Остальное — шум извлечения.

## Новое
- **IntelliQoS: ограничение скорости по интерфейсу (upstream rate-limit)** — локальная функция, берём в наше приложение:
  - чтение: `GET /rci/show/rc/ntce/upstream/rate-limit` (массив; поля `interface`, `input.rate`, `output.rate`);
  - запись: `ntce upstream rate-limit {interface, input:{rate}, output:{rate}}` + `system configuration save`.
  - Формат — по декомпилятору (🔴), проверять на роутере.
- **MCP-токены** (`networks/subscreen/bots/mcp`): `POST /client/mcp` через CoAP/облако (поля: access_token, cid, cloud_access, device_access, router_exec, expires_at, model) — только облако, не берём.
- Хранилище секретов (`LazySecretsStorage`) и `LegalModel.LegalMcp` — служебное.

## Убрано
- `POST /rci/components/check-update`; ветки для lte/nr5g/umts/operator и профиля ping-check в сборке команд интерфейса (скорее рефакторинг, а не удаление функций).

## Вывод
Функциональных изменений RCI мало; для паритета достаточно добавить `ntce upstream rate-limit`. Структура экранов — `SCREENS-71.md` (195 классов в 48 группах: applications — afp/dlna/smb/torrents/ipsec/l2tp/pptp/sstp/openConnect/wireguardServer/udpProxy/privateCloud/statistic; devices; networks — bots/networkSettings/technicalsupport/warranty; connectedDevices; familyProfile; events; usb; stat; wifiSystem; userprofile).
