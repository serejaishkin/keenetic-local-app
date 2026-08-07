#!/usr/bin/env bash
# Раскладывает 14 финальных файлов по местам и делает один коммит.
# Запускать из корня репозитория keenetic-local-app, после того как
# скачал все 14 .kt файлов в одну папку.

set -euo pipefail

SRC_DIR="${1:-.}"
REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

mkdir -p app/src/main/java/com/keenetic/local/ui/screens/common

declare -A FILES=(
  ["MainActivity.kt"]="app/src/main/java/com/keenetic/local/MainActivity.kt"
  ["KeeneticRestApi.kt"]="app/src/main/java/com/keenetic/local/api/KeeneticRestApi.kt"
  ["RouterViewModel.kt"]="app/src/main/java/com/keenetic/local/ui/RouterViewModel.kt"
  ["DashboardScreen.kt"]="app/src/main/java/com/keenetic/local/ui/screens/DashboardScreen.kt"
  ["DevicesScreen.kt"]="app/src/main/java/com/keenetic/local/ui/screens/DevicesScreen.kt"
  ["DnsFiltersScreen.kt"]="app/src/main/java/com/keenetic/local/ui/screens/DnsFiltersScreen.kt"
  ["FirewallScreen.kt"]="app/src/main/java/com/keenetic/local/ui/screens/FirewallScreen.kt"
  ["MobileScreen.kt"]="app/src/main/java/com/keenetic/local/ui/screens/MobileScreen.kt"
  ["PortForwardingScreen.kt"]="app/src/main/java/com/keenetic/local/ui/screens/PortForwardingScreen.kt"
  ["SettingsScreen.kt"]="app/src/main/java/com/keenetic/local/ui/screens/SettingsScreen.kt"
  ["StaticRoutesScreen.kt"]="app/src/main/java/com/keenetic/local/ui/screens/StaticRoutesScreen.kt"
  ["VpnAdvancedScreen.kt"]="app/src/main/java/com/keenetic/local/ui/screens/VpnAdvancedScreen.kt"
  ["WiFiScreen.kt"]="app/src/main/java/com/keenetic/local/ui/screens/WiFiScreen.kt"
  ["RawJsonCard.kt"]="app/src/main/java/com/keenetic/local/ui/screens/common/RawJsonCard.kt"
)

MISSING=0
for src in "${!FILES[@]}"; do
  if [[ ! -f "$SRC_DIR/$src" ]]; then
    echo "НЕ НАЙДЕН: $SRC_DIR/$src"
    MISSING=1
  fi
done
if [[ "$MISSING" == "1" ]]; then
  echo "Останов: положи все 14 файлов в $SRC_DIR (или укажи папку первым аргументом)."
  exit 1
fi

for src in "${!FILES[@]}"; do
  dest="${FILES[$src]}"
  cp -v "$SRC_DIR/$src" "$dest"
done

git add -A
git commit -m "feat: firewall (реальный формат из HAR), расписания, дашборд, тап-по-устройству, скан Wi-Fi, автообновление, offline-баннер, ApiCallState

Полная замена файлов вместо патчей - предыдущие патчи перестали
накладываться из-за дрейфа между версиями файлов в этой сессии.
Откат: git revert --no-edit \$(git rev-parse HEAD)"

echo
echo "Готово. Коммит: $(git rev-parse --short HEAD)"
echo "Откат: git revert --no-edit $(git rev-parse HEAD) && git push"
