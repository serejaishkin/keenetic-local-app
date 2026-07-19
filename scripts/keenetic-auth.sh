#!/usr/bin/env bash
# Simple helper to reproduce the interactive auth from Keenetic web UI.
# Usage: ./scripts/keenetic-auth.sh [IP] [USER]
IP=${1:-192.168.3.1}
USER=${2:-admin}
read -s -p "Password: " PASS
echo
TMP=$(mktemp)
curl -s -D "$TMP" -o /dev/null "http://$IP/auth"
challenge=$(grep -i '^X-NDM-Challenge:' "$TMP" | awk '{print $2}' | tr -d '\r')
realm=$(grep -i '^X-NDM-Realm:' "$TMP" | cut -d' ' -f2- | tr -d '\r')
setcookie=$(grep -i '^Set-Cookie:' "$TMP" | sed -n '1s/Set-Cookie: //Ip')
cookie=$(echo "$setcookie" | cut -d';' -f1)
md5=$(printf "%s:%s:%s" "$USER" "$realm" "$PASS" | md5sum | awk '{print $1}')
sha=$(printf "%s%s" "$challenge" "$md5" | sha256sum | awk '{print $1}')
echo "Realm: $realm"
echo "Challenge: $challenge"
echo "Cookie: $cookie"
echo "MD5: $md5"
echo "SHA256: $sha"
curl -i -s -H "Content-Type: application/json" -H "Cookie: $cookie" \
  -d "{\"login\":\"$USER\",\"password\":\"$sha\"}" "http://$IP/auth"
