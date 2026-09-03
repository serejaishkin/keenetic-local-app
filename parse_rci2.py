import urllib.request, re

url = "https://raw.githubusercontent.com/serejaishkin/keenetic-local-app/main/downloads-claude/htdocs_/main-553997B.js"
req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
with urllib.request.urlopen(req) as resp:
    data = resp.read().decode("utf-8", errors="ignore")

# Find all string literals that match:
# "show.xxx" or "interface.xxx" or "ip.xxx"
# In Keenetic CLI / RCI, commands are lower-case, with hyphens or underscores
pattern = re.compile(r'["\']((?:show|interface|ip|system|tools|crypto|user|service|media|vpn|mesh|mws|ndns|sms|storage|opkg|components|dsl|modem|thermal|led|torrent)\.[a-z0-9][a-z0-9_\-\.]+)["\']')

matches = set(pattern.findall(data))

# Also in Angular templates or i18n, translations might look like "show.title". Let's inspect locale.ru.json to know which ones are translation keys vs RCI commands!
locale_url = "https://raw.githubusercontent.com/serejaishkin/keenetic-local-app/main/downloads-claude/htdocs_/assets/language/locale.ru.json"
req_loc = urllib.request.Request(locale_url, headers={"User-Agent": "Mozilla/5.0"})
import json
try:
    with urllib.request.urlopen(req_loc) as resp_loc:
        loc_data = json.loads(resp_loc.read().decode("utf-8"))
        # Flatten all keys in locale.ru.json
        def flatten(d, prefix=""):
            keys = set()
            for k, v in d.items():
                curr = f"{prefix}.{k}" if prefix else k
                keys.add(curr)
                if isinstance(v, dict):
                    keys.update(flatten(v, curr))
            return keys
        i18n_keys = flatten(loc_data)
        print(f"Total i18n keys in locale.ru.json: {len(i18n_keys)}")
except Exception as e:
    i18n_keys = set()
    print("Could not load locale:", e)

# Filter out i18n keys! The rest are real RCI commands!
actual_rci_commands = set()
for m in matches:
    if m not in i18n_keys:
        actual_rci_commands.add(m)

print(f"Total matches in main.js: {len(matches)}")
print(f"Total pure RCI commands after subtracting i18n: {len(actual_rci_commands)}")

# Group by category
grouped = {}
for cmd in sorted(actual_rci_commands):
    cat = cmd.split('.')[0]
    grouped.setdefault(cat, []).append(cmd)

for cat, cmds in sorted(grouped.items()):
    print(f"\n==================== [{cat.upper()}] ({len(cmds)}) ====================")
    for c in cmds[:30]:
        print(" ", c)
    if len(cmds) > 30:
        print(f"  ... and {len(cmds) - 30} more")

