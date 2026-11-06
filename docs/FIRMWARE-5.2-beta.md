# Netis N6 5.2 Beta 1 (KeeneticOS 5.02.B.1.0-3) — что нового (по строкам в libndmCorePack/libndmMws, 5.1.3 → 5.2)
Источник — разбор образа `Netis_N6_5.2_Beta_1.bin`. Статус: 🔴 по строкам, на роутере не проверялось.

## Образ
Другой «порт»: Project Athens (`t.me/project_athens`), сертификат «Firmware Signing Certificate» (EC, подпись 70 байт, до 2036-10-07); модель KN-2311, devid 0x11238000 (тот же), ядро 4.9-ndm-5, rootfs 1513 файлов (было 1412), 61 libndm*.so. Компонент `libndmVirtualIpServer.so` убран; обновлены gnutls/nettle/libxml2/libz/ipoque_pace2 (DPI), CA-сертификаты, добавлен `/etc/uag.json` (разбор User-Agent).

## Токены доступа (новый пункт «генерация ключа доступа»)
- CLI: ветка `authentication token` — «generate a new trusted authentication token» и «delete custom trusted authentication token»; настройки `authentication token settings`; просмотр списка: «view a list of tokens» (`tokens`, `last-access`).
- Токены привязаны к пользователю (`added a %1token for user "%2"`, `removed a token for user "%1"`), бывают одноразовые (`one-time-token`).
- HTTP: заголовок `X-NDM-TOKEN` (был и в 5.1.3), в 5.2 добавлены `X-Ndm-Cloned-Session-Token`/`auth_x_session_token` для прокси.
- Предупреждение в логе: «obsoleted unauthenticated loopback RCI access will be removed soon, consider switching to access tokens» — доступ к RCI с localhost без авторизации будет убран. Тем, кто ходит на 127.0.0.1 без пароля (скрипты на роутере/Entware), нужно переходить на токены.
- Методы аутентификации (теги): mobile-app, browser-cloud, browser-direct, rmm-cloud, ndmp, one-time-token, vpn-oc, ipsec-*.
- **Подтверждено декомпиляцией Ghidra** (`libndmCorePack.so`, `docs/firmware/ghidra-auth-token-decompile.txt`):
  - Команды CLI: `authentication token generate [<user-data>] [one-time]`, `authentication token delete <id>`, `show authentication token`.
  - Генерация работает от имени текущего пользователя сессии: нужен уровень «пользователь» (иначе статус-ошибка, код 0x80e101f4) и тег `http` у пользователя (иначе 0x80e10258). Значение токена — случайная ASCII-строка 56 символов; тип TOKEN или ONE_TIME_TOKEN (флаг `one-time`). Токен возвращается в ответе команды (узел ответа).
  - Хранится как NdmaToken: id, соль, дайджест (токен целиком в роутере не хранится — показывается один раз), владелец, сервис, user-data, срок (`Expires`), `last-access`.
  - Допустимая длина значения при добавлении — 32…64 печатных ASCII-символа.
- Формат ответа (имена полей JSON в RCI) всё ещё неизвестен → снять через «Снимок ответов RCI» / сетевую вкладку веб-интерфейса при нажатии кнопки генерации.

## Прочее новое
- WPA3-SAE extended key: `mws wlan wpa sae ext-key` (+ enable/disable), в `libndmWifiMtk`.
- Группы объектов `object-group fqdn-keyword` (FQDN-ключевые слова для фильтрации).
- Команды `parse`, `probe`, `autocomplete a plaintext command` есть и в 5.1.3 (RCI-вызов автодополнения может заменить SSH-обход `?`; формат не проверен).

## Задачи для приложения (ROADMAP)
- M6.4 ⬜ экран «Токены доступа» (список/создание/удаление) — после снятия формата.
- M4.4 ⬜ WPA3-SAE ext-key в Wi-Fi; M3.4 ⬜ fqdn-keyword в группах объектов.
