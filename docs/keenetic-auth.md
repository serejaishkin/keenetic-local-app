# Тестирование авторизации Keenetic

Этот документ объясняет, как воспроизвести web-интерактивную авторизацию Keenetic вручную и отладить клиента.

1) Быстрый тест (curl)

```bash
curl -i --max-time 5 http://<ROUTER_IP>/auth
```

Ожидаемые заголовки в ответе (пример):

- `X-NDM-Challenge`
- `X-NDM-Realm` или `WWW-Authenticate` с полем `realm="..."`
- `Set-Cookie` (сессионный cookie)

2) Скрипт для ручного входа

В репозитории есть `scripts/keenetic-auth.sh`. Запустите его и введите пароль.

```bash
./scripts/keenetic-auth.sh 192.168.3.1 admin
```

Скрипт автоматически получит challenge/realm/cookie, вычислит md5/sha256 и выполнит POST `/auth`.

3) Изменения в коде

- `KeeneticAuthInterceptor` (app/src/main/java/com/keenetic/local/api/) реализует последовательность запросов и логирует процесс.
- `RouterRepository` теперь логирует заголовки ответа от `GET /auth` для упрощения отладки.

4) Примечания по безопасности

Никогда не выкладывайте пароли в публичные чаты. Для CI/тестов используйте защищённые переменные.
