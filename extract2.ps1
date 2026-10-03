$ErrorActionPreference = 'Stop'
$base = 'D:\GitHub\keenetic-local-app'
$htmlDir = Join-Path $base 'docs\webui'

$files = Get-ChildItem -Path $htmlDir -Filter '*.html'
$outLines = New-Object System.Collections.Generic.List[string]

foreach ($f in $files) {
    $text = [System.IO.File]::ReadAllText($f.FullName, [System.Text.Encoding]::UTF8)
    $regex = [regex] '>([^<>]{2,90})<'
    $map = New-Object 'System.Collections.Generic.Dictionary[string,int]'
    foreach ($m in $regex.Matches($text)) {
        $s = $m.Groups[1].Value.Trim()
        if ($s -match '[\u0410-\u044F\u0401\u0451]') {
            $clean = $s -replace '\s+', ' '
            if ($clean.Length -ge 2 -and -not $map.ContainsKey($clean)) { $map[$clean] = 1 }
        }
    }
    $outLines.Add("")
    $outLines.Add("======== $($f.Name)  ($($map.Count) подписей) ========")
    foreach ($k in ($map.Keys | Sort-Object)) { $outLines.Add($k) }
}

$outFile = Join-Path $base 'webui_texts.txt'
[System.IO.File]::WriteAllLines($outFile, $outLines, (New-Object System.Text.UTF8Encoding($false)))
Write-Output ("Записан файл: " + $outFile + " (строк: " + $outLines.Count + ")")
