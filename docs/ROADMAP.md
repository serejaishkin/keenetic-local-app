# ROADMAP — Keenetic Local (карта разработки)
Обновлено: 2026-10-17. Разбор прошивки (kn_port) в карту не входит.
Источник новых задач: разбор оригинального Android-приложения Keenetic 7.0 (`Keenetic_70__360__Beta.apk`): 131 путь `/rci/...` + менеджеры функций. Список путей — `docs/apk/original-app-rci-endpoints.txt`, команды — `docs/apk/APK-COMMANDS.md`.

Маркировка: ✅ есть в приложении · 🟡 частично · ⬜ нет · 🚫 не делаем (только облако).
Формат команд — только из HAR/SSH с реального роутера (правило из API-REFERENCE.md); из APK берём пути и порядок вызовов, а не формат ответов.

## Что уже есть (по экранам в коде)
Dashboard ✅ · Wi-Fi ✅ · Devices ✅ · DNS-фильтры 🟡 · Firewall ✅ · Port forwarding ✅ · Static routes ✅ · LAN segments ✅ · VPN (advanced) 🟡 · Internet/WAN ✅ · Firmware 🟡 · Services/WebServices ✅ · USB devices/storage ✅ · System logs ✅ · Diagnostics ✅ · Terminal (SSH) ✅ · User accounts ✅ · Mobile ✅ · Apps ✅

## M0. Карта и инфраструктура
- M0.1 ✅ этот файл.
- M0.2 ⬜ скрипт `tools/rci_diff.py`: список путей оригинала vs `rci/…` в коде → таблица пробелов. Каркас карты нужно перепроверить: приложение строит команды вложенными Map, прямое сравнение строк занижает покрытие.
- M0.3 ⬜ в API-REFERENCE.md завести раздел «Пути из оригинала, формат не подтверждён» (🔴) и переводить в 🟢 только по HAR.

## M1. DNS-фильтры и защита (приоритет: ходовая функция, ничего не требует от облака)
- M1.1 🟡 профили публичных DNS (AdGuard, Cloudflare, Quad9, Яндекс) — источник `dns-presets.json` из прошивки; DoH/DoT серверы (`/rci/dns-proxy`, `show dns-proxy`).
- M1.2 ⬜ SkyDNS / SafeDNS / NextDNS: `show skydns|safedns|nextdns`, привязка устройств к профилям (в оригинале — отдельные менеджеры PaidDns/PublicDns). Внимание: компонентов может не быть в порте.
- M1.3 ⬜ расписание и привязка профиля к сегменту/устройству (`DnsAndScheduleParser` уже есть).

## M2. Удалённый доступ без облака (замена KeenDNS/облачного приложения)
- M2.1 ⬜ WireGuard: сервер/клиент, импорт QR/конфига (`WireguardManager` оригинала, `interface/Wireguard*`).
- M2.2 ⬜ мастер «Удалённый доступ»: проверка внешнего IP/DDNS (`ip/http`, `show ndns` — только чтение), пошаговая настройка VPN для телефона.
- M2.3 🚫 KeenDNS (`ndns/book-name|check-name|drop-name|get-booked|get-update`) — требует серверов Keenetic.
- M2.4 ⬜ ACME/SSL (`show acme`, `ip/http/ssl/acme`) для своего домена.

## M3. Устройства и клиенты
- M3.1 ⬜ Wake-on-LAN (`ip/hotspot/wake`), автоматическая регистрация (`ip/hotspot/auto-register`).
- M3.2 ⬜ статические адреса DHCP (`ip/dhcp/host`), traffic-shape для неизвестных (`ip/traffic-shape/unknown-host`).
- M3.3 ⬜ политики/теги (`show tags`), расписания устройств (`ScheduleManager`, `show schedule`).

## M4. Сеть
- M4.0 ⬜ IntelliQoS upstream rate-limit (новое в оригинале 7.1): `show rc ntce upstream rate-limit`, запись `ntce upstream rate-limit`. См. docs/apk/VERSION-DIFF-70-71.md.
- M4.1 ⬜ IPv6 (`show ipv6 …`), PPPoE/IPoE (`/rci/pppoe`, `interface/ipoe`), DSL/USB-DSL.
- M4.2 ⬜ Wi-Fi: site-survey (парсер есть), настройка каналов/VHT, гостевая сеть, ping-check.
- M4.3 ⬜ mesh (MWS): `show mws`, `mws/acquire|member|revisit|zone`. Проверить, есть ли на KN-2311/Netis.

## M5. Медиа и накопители
- M5.1 ⬜ DLNA (`dlna/rescan`), торрент-клиент (`torrent/rpc`, `show torrent`), SMB/AFP (`show cifs|afp`), `system/eject`, `interface/usb/power-cycle`.

## M6. Система
- M6.1 ⬜ журнал: `system/log` (фильтры), `show last-change`, `show clock`, `show eula|dpn` не нужны.
- M6.2 ⬜ пароль пользователя (`user/password`), `whoami`, `erase`, `system/configuration` (save/reset) с подтверждениями.
- M6.3 🟡 компоненты: `components/list|check-update|install|remove|commit`. Список показывать можно; установка в порте идёт через NDSS (см. отдельный разбор) — отметить в UI как «требует сервера Keenetic».

## M7. Качество
- M7.1 ⬜ тесты парсеров на HAR-фикстурах; M7.2 ⬜ WebView-обёртка / мобильная вёрстка веб-панели (по необходимости); M7.3 ⬜ демо-режим для новых экранов.

## R. Исследования (не блокируют релиз)
- R1 Coala (CoAP+Curve25519/AES-GCM) — прямое подключение без облака, если на роутере есть coalagent. Сначала проверить на реальном роутере, есть ли UDP-ответ на discovery (токен `eb21926ad2e765a7`). Данных пока нет.
- R2 Снять формат ответов по реальному роутеру для каждого ⬜-раздела (HAR или `show … ` по SSH).

## Порядок работ
1) M0.2 → 2) M1.1–M1.2 → 3) M2.1–M2.2 → 4) M3 → 5) M6 → 6) M4/M5 → R.
Каждая подзадача: HAR/вывод с роутера → парсер + тест → экран → запись в API-REFERENCE.md → коммит. Сначала коммит и push, потом сборка (правило AGENTS.md).

## Источники форматов ответов (добавлено)
1. реальные пары (были в HAR; новые снимать экраном «Снимок ответов RCI») запрос→ответ из ваших HAR (235 уникальных команд; значения вида пароль/ключ/токен замаскированы `***`, списки обрезаны до 6 элементов). Статус 🟢.
2. `docs/apk/RESPONSE-MODELS.md` — схемы полей из 239 Gson-моделей оригинального приложения (1076 полей: имена JSON и типы). Статус 🔴 — схема без значений, подтверждать образцом.
3. Нет ни там, ни там (нужен вывод с роутера): skydns/safedns/nextdns, wireguard, acme, afp, ntp, tags, dpn/eula. Снять: `show <команда>` по SSH или `curl -u admin:… http://<ip>/rci/show/<путь>`.
