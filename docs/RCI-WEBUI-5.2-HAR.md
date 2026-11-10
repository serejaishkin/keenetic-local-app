# Веб-интерфейс KeeneticOS 5.2 Beta 1 (Netis N6 / KN-2311): что видно в HAR

Источник: HAR обхода веб-интерфейса (Chrome/Yandex) на роутере 5.02.B.1.0-3 «5.2 Beta 1», sandbox `preview`.
Секреты из HAR (пароли, PSK, ключи iapp, значения `proof`/`signature`) сюда намеренно НЕ скопированы.

## Главное: новая схема входа `x-ndw4-interactive`
Роутер в `WWW-Authenticate` предлагает ДВА метода:
- `x-ndw2-interactive realm="Keenetic KN-2311" challenge="..."` — старая (MD5 + SHA256 challenge). Приложение и скрипт использовали её, роутер её больше не принимает (401).
- `x-ndw4-interactive endpoint="/auth" data="<base64 JSON>"` — новая, её использует веб-интерфейс.

Обмен в веб-интерфейсе (3 POST на `/auth`, JSON):
1. `{"login":"admin","nonce":"<клиентский nonce, base64 16 байт>"}` → 401, `X-NDM-Data` (base64 JSON): `{"salt":"<base64 16 байт>","nonce":"<клиентский>@<серверная добавка>","iter":2,"memcost":8}`.
2. `{"login","nonce":"<расширенный>","proof":"<base64 64 байта>"}` → 401, `X-NDM-Data`: `{"signature":"<base64 64 байта>"}` (подпись сервера).
3. `{"login","nonce":"<расширенный>","signature-proof":"<base64 64 байта>"}` → 200 `{"ndsso":false,"method":"x-ndw2-interactive"}` + cookie сессии.
Размер 64 байта = SHA-512/BLAKE2b-512. В ядре (`libndmCorePack.so`) есть `Credentials::Hash::Ndw4` с `Iter`, `MemCost`, `Salt`, `StoredKey`, `ServerKey` — то есть SCRAM-подобная схема с memory-hard KDF (параметры `iter`, `memcost`). Точный алгоритм: см. раздел ниже / ghidra-декомпиляция.
Каждый запрос `/auth` на роутере выполняется ≈2 с (медленный CPU): таймауты клиента ≥ 15 с.

## Версия и компоненты (`show/version`)
`release 5.02.B.1.0-3`, `sandbox preview`, `title 5.2 Beta 1`, `arch mips`; в `ndw.features`: dual_image, usb_3_conf, wifi6, wpa3, hwnat, lte…; в `ndw.components`: acl, ddns, dns-filter, dns-https, dns-tls, mws, ntce, openvpn, opkg, ssh, sstp, trafficcontrol, transmission, tsmb, usbqmi и др. (облачных компонентов нет).

## Транспорт веб-интерфейса
Всё идёт `POST /rci/` с телом — массив вложенных команд, ответ — массив в том же порядке. Чтение: `{"show":{"sc":{...}}}`; запись: `{"interface":{"name":"Proxy0","description":"x"}}`; сохранение: `{"system":{"configuration":{"save":{}}}}`. Ответы на запись содержат `status:[{status,code,ident,message}]`.
`{"whoami":{}}` → id, login, `credentials-class`, `access.level`, lifetime (`deadline` 299 с, `lingering-deadline` 3599 с), endpoint (remote/local, `via: browser-direct`).

## Реальные формы ответов (пустые значения = на этом роутере не настроено)
- `show/sc/dns-proxy/filter/engine` → `{"engine": ""}`; `filter/profile` → `{}`; `filter/assign` → `{}`; `show/dns-proxy/filter/presets` → `{"version":17}`; `sc/dns-proxy/intercept` → `{"enable":false}`; `sc/dns-proxy/tls/upstream` и `https/upstream` → `[]`.
- `show/sc/service` → `{dhcp,dns-proxy,igmp-proxy,mws,http,ftp,cifs,telnet,ssh,ntp,mdns,torrent,udpxy,internet-checker: bool}`.
- `show/sc/ip/hotspot/policy` → `[{"interface":"Bridge0","access":"permit"}]`.
- `show/internet/status` → `{checked,enabled,reliable,gateway-accessible,dns-accessible,captive-accessible,internet,gateway:{interface,address,failures,accessible,excluded}}`.
- `show/device-list` → `{"host":[{mac,via,ip,ip6[],hostname,name,interface:{id,name,description},dhcp:{expires},registered,access,schedule,policy,priority,active,rxbytes,txbytes,link,uptime,first-seen,last-seen,traffic-shape:{rx,tx,mode,schedule}}]}`.
- `show/mws/wlan` → объекты по сегментам (`Home`, `Guest`): `band.<n>.enabled`, `access-point.<AP>.wps{configured,status,...}`.
- `show/sc/vpn-server` → `{interface,pool-range:{begin,size},multi-login,lockout-policy,lcp,config:{...,enable}}`.
- `show/system/mode` → `{active,selected,supported:"router, extender",hw_controlled,hw_locked}`.
- `show/sc/ntp/master` → `true`; `show/sc/ppe` → `[{"engine":"software"},{"engine":"hardware"}]`.
- `show/system/backup/status` → `{state:"NO_CONFIGURATION",password-set:false}`.
- `show/ip/hotspot/summary` с `{attribute:"sumbytes",count:5,detail:0}`; `show/system/rrd/cpu|memory` с `{attribute,detail}`; `show/interface/rrd` — массив `{attribute:"rxspeed|txspeed",name,detail}`.

## Команды записи, замеченные в HAR (формат реальный, версия 5.2 Beta)
- Прокси-интерфейс (SOCKS5-«Proxy»): `interface{name,description}`, `interface{name,ip:{global:{no:true}}}`, `interface{name,proxy:{protocol:{proto:"socks5"}}}`, `interface{name,proxy:{upstream:{host,port}}}`, `interface{name,authentication:{identity,password}}`, `interface{name,proxy:{connect:{no:true}}}`.
- Известное устройство: `known{host{mac,name}}`; доступ: `ip{hotspot{host{mac,permit:true,policy:{no:true},schedule:{no:true},conform:{no:true}}}}`; снять привязку по MAC: `interface{name:"Bridge0",mac:{band:{mac,no:true}}}`.
- Политика: `ip{policy{name:"Policy0",description,multipath:false}}`; привязка: `ip{hotspot{host{mac,permit:true,policy:"Policy0"}}}`.

## Как это влияет на приложение
1. Вход: нужна реализация ndw4 (в работе), старая схема на 5.2 не работает.
2. Снимок RCI: добавлены пути веб-интерфейса 5.2 (`DEFAULT_PATHS`).
3. Таймауты: ответы по ≈2 с — увеличить read/connect таймаут для входа.
