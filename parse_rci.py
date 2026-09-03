import urllib.request, re

url = "https://raw.githubusercontent.com/serejaishkin/keenetic-local-app/main/downloads-claude/htdocs_/main-553997B.js"
req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
with urllib.request.urlopen(req) as resp:
    data = resp.read().decode("utf-8", errors="ignore")

# Let's find all path assignments in the JS bundle
# Common pattern: path:"..." or path: "..." or readPath: "..." or writePath: "..." or performPath: "..."
all_paths = set(re.findall(r'(?:path|readPath|writePath|performPath|toPath)\s*:\s*["\']([a-zA-Z0-9_.\-]+)["\']', data))
print(f"Direct RCI path variables count: {len(all_paths)}")

# Also string variables defined like: var xxx = "show...."
var_endpoints = set(re.findall(r'var\s+[a-zA-Z0-9_$]+\s*=\s*["\']((?:show|interface|ip|system|tools|crypto|user|service|media|vpn|mesh|mws|sc|ppe|dns|schedule|cloud|ndns|sms|storage|opkg|components|dsl|modem|thermal|led)\.[a-zA-Z0-9_\-\.]+)["\']', data))
print(f"Variable-assigned RCI endpoints: {len(var_endpoints)}")

combined = sorted(list(all_paths.union(var_endpoints)))
print(f"\nTotal verified RCI endpoint paths: {len(combined)}")

by_group = {}
for p in combined:
    group = p.split('.')[0]
    by_group.setdefault(group, []).append(p)

for grp, items in sorted(by_group.items()):
    print(f"\n=== {grp.upper()} ({len(items)} endpoints) ===")
    for item in sorted(items):
        print(" ", item)

