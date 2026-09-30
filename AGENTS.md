# Правила работы над keenetic-local-app

## Коммит и пуш — ДО сборки и тестов

Обязательный порядок для любых правок UI или логики:

1. Внести правки.
2. `git add` только целевые файлы (никогда `git add -A`).
3. `git commit` — до запуска сборки/тестов.
4. `git push origin main`.
5. Только потом `./gradlew.bat ...` и установка APK на устройство.

Причина: рабочее дерево уже один раз было молча перезаписано чужой версией
(см. коммит `e6d4310`, до него потеряны правки и HEAD откатился на пред-G4
состояние). Коммит+пуш до тестирования делает работу невосприимчивой к
перезаписи: `git restore .` не способен отменить уже запушенный коммит.

Дополнительно перед коммитом стоит убедиться, что `git status --short` не
показывает чужих правок в десятках файлов, и сделать бэкап при сомнениях.

## Сборка и тесты

```bash
./gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest \
  --tests com.keenetic.local.api.KeeneticRciRepositoryTest \
  :app:assembleDebug --console=plain --offline
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Ограничения предметной области

- DNS — один экран (`DnsFilterContent` внутри `DnsScreen`). Файлы
  `DnsFiltersScreen.kt` и `SettingsScreen.kt` не возвращать: это отклонённый
  вариант, лежит только в бэкапах.
- RCI-команды и сигнатуры существующих методов `RouterViewModel` не менять
  без необходимости: иначе ломается обратная совместимость с прошивкой.
- Детальные экраны используют `SectionScaffold` из
  `ui/components/SectionUi.kt`; корневые вкладки — собственную шапку.
  Не смешивать эти два подхода.

## Живой QA на роутере

- Устройство: `GE5TN7LRHYYLPVOJ`, разрешение 1080x2400, пакет
  `com.keenetic.local`.
- `adb` не в `PATH`:
  `export PATH="$PATH:/c/Users/serge/AppData/Local/Android/Sdk/platform-tools"`
- Для `uiautomator dump` и `adb pull` в Git Bash нужен
  `MSYS_NO_PATHCONV=1` и Windows-путь назначения.
- При QA не нажимать опасные действия: перезагрузку, форматирование,
  удаление, обновление прошивки, применение конфигурации, изменение рабочих
  настроек.

## Временные артефакты

Не коммитить: `buildlog.txt`, `extract2.ps1`, `extract_webui.ps1`, `nul`,
`out.log`, `webui_texts.txt`.

Резервные копии отклонённых вариантов:

- `C:\Users\serge\AppData\Local\Temp\opencode\tree-backup` — частичная копия
  чужого рефактора (8 файлов).
- `C:\Users\serge\AppData\Local\Temp\opencode\tree-divergent` — полная копия
  расходившегося рабочего дерева, снятого перед откатом к `0bda144`.
