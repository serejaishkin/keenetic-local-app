$ErrorActionPreference = 'Stop'
$dir = 'D:\GitHub\keenetic-local-app\docs\webui'
$out = 'D:\GitHub\keenetic-local-app\webui_texts.txt'
$html = Get-ChildItem -Path $dir -Filter '*.html' | Where-Object { $_.Name -match 'интернет|Ethernet|Ethernet-кабелю' } | Select-Object -First 1
if (-not $html) { $html = Get-ChildItem -Path $dir -Filter '*.html' | Select-Object -First 1 }
$text = [System.IO.File]::ReadAllText($html.FullName, [System.Text.Encoding]::UTF8)
$matches = [regex]::Matches($text, '>([^<>]{2,90})<')
$map = New-Object 'System.Collections.Generic.Dictionary[string,int]'
foreach ($m in $matches) {
    $s = $m.Groups[1].Value.Trim()
    if ($s -match '[А-Яа-яЁё]') {
        $clean = $s -replace '\s+', ' '
        if ($clean.Length -ge 2 -and -not $map.ContainsKey($clean)) { $map[$clean] = 1 }
    }
}
$sorted = $map.Keys | Sort-Object
"Файл: $($html.Name)" | Out-File $out -Encoding utf8
"Всего русских подписей: $($sorted.Count)`n" | Out-File $out -Append -Encoding utf8
$sorted | Out-File $out -Append -Encoding utf8
Write-Output "Готово: $out ($($sorted.Count) подписей)"
