#!/usr/bin/env python3
"""Авто-снимок ответов RCI. ТОЛЬКО GET /rci/show/... — ничего на роутере не меняет.
Запуск:  python3 rci_snapshot.py [--host 192.168.1.1] [--user admin] [--no-mask]
Пароль: переменная KEENETIC_PASS или запрос в консоли. Без внешних зависимостей.
Работает на ПК, в Termux, на Orange Pi. Результат: rci_snapshot_<время>.json
Вход: стандартный challenge-вход Keenetic (GET /auth -> 401 -> POST /auth). Формат взят из
известного поведения веб-интерфейса, на вашем роутере не проверен: при ошибке входа пришлите вывод.
"""
import argparse, getpass, hashlib, http.cookiejar, json, os, re, time, urllib.request, urllib.error

PATHS = ["version","system","clock/date","last-change","interface","internet/status","ip/hotspot",
"ip/policy","ip/static","ip/route","ip/dhcp/bindings","ip/dhcp/pool","ipv6/addresses","ipv6/prefixes",
"ipv6/route","upnp/redirect","associations","mws","mws/wlan","site-survey","ping-check","schedule",
"dns-proxy","dns-proxy/filter/presets","dns-proxy/filter/profiles","skydns","safedns","nextdns",
"skydns/userinfo","safedns/userinfo","rc/skydns","rc/safedns","rc/nextdns","skydns/profiles/profiles",
"safedns/profiles/profiles","nextdns/profiles/profiles","ndss","ndns","acme","crypto","vpn-server",
"sstp-server","oc-server","ntce/summary","ntce/status","tags","authentication/token",
"rc/ntce/upstream/rate-limit","usb","media","torrent","cifs","afp","ntp/status","eula","dpn"]
SECRET = re.compile(r"(?i)(pass|secret|private|psk|token|cookie|auth-key|^key$|-key$|^sid$)")

def mask(v):
    if isinstance(v, dict):
        return {k: ("***" if SECRET.search(k) and isinstance(x, str) and x not in ("", "true", "false") else mask(x)) for k, x in v.items()}
    if isinstance(v, list): return [mask(x) for x in v]
    return v

def main():
    a = argparse.ArgumentParser()
    a.add_argument("--host", default="192.168.1.1"); a.add_argument("--user", default="admin")
    a.add_argument("--no-mask", action="store_true"); a.add_argument("--paths", help="файл с доп. путями, по одному в строке")
    a = a.parse_args()
    pw = os.environ.get("KEENETIC_PASS") or getpass.getpass("Пароль: ")
    base = f"http://{a.host}"
    op = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
    try: op.open(base + "/auth", timeout=10)
    except urllib.error.HTTPError as e:
        if e.code != 401: raise
        ch, realm = e.headers.get("X-NDM-Challenge", ""), e.headers.get("X-NDM-Realm", "")
        md5 = hashlib.md5(f"{a.user}:{realm}:{pw}".encode()).hexdigest()
        sha = hashlib.sha256((ch + md5).encode()).hexdigest()
        req = urllib.request.Request(base + "/auth", json.dumps({"login": a.user, "password": sha}).encode(), {"Content-Type": "application/json"})
        op.open(req, timeout=10)
    paths = list(PATHS)
    if a.paths: paths += [l.strip().removeprefix("show ").strip("/").replace(" ", "/") for l in open(a.paths) if l.strip()]
    res = {}
    for p in dict.fromkeys(paths):
        try:
            raw = op.open(base + "/rci/show/" + p, timeout=15).read()
            body = json.loads(raw or b"null"); ok = True
        except Exception as e:
            body, ok = str(e), False
        res["show/" + p] = {"ok": ok, "response": body if (a.no_mask or not ok) else mask(body)}
        print(("OK  " if ok else "ERR ") + p)
    out = f"rci_snapshot_{time.strftime('%Y%m%d_%H%M%S')}.json"
    json.dump({"meta": {"host": a.host, "masked": not a.no_mask, "created": time.ctime()}, "results": res}, open(out, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print("Сохранено:", out)
main()
