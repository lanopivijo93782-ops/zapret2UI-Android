<div align="center">

<img src="app/src/main/res/drawable/ic_launcher.png" width="96" alt="Zapret2UI Android"/>

# Zapret2UI — Android

**Обход блокировок в один клик — теперь на Android.**
Прямой форк [Asterlike/zapret2UI](https://github.com/Asterlike/zapret2UI) (Windows/WPF) для Android 8.0+.

<p>
  <img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white"/>
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white"/>
  <img src="https://img.shields.io/badge/VPNService-TUN%20(no%20root)-34D399?style=for-the-badge"/>
</p>

<p>
  <a href="https://github.com/Asterlike/zapret2UI-Android/releases/latest"><img alt="Скачать APK" src="https://img.shields.io/badge/Скачать%20APK-8B7FF5?style=for-the-badge&logo=android&logoColor=white"/></a>
  <a href="https://github.com/Asterlike/zapret2UI"><img alt="Оригинал" src="https://img.shields.io/badge/Оригинал-Windows-12151c?style=for-the-badge&logo=windows&logoColor=8B7FF5"/></a>
</p>

Одна кнопка возвращает доступ к **Discord, YouTube и Telegram** — без ручной настройки, без сторонних VPN-серверов. Desync делается **на устройстве**, через локальный TUN.

> ⚠️ Для исследовательских целей и восстановления доступа к легальным ресурсам. Используйте по законам своей страны.

<img src="docs/screenshot-home.png" width="780" alt="Главный экран"/>

</div>

---

## Что это

Прямой порт логики `zapret2UI` на Android:

| Десктоп (оригинал) | Android (этот форк) |
|---|---|
| `winws2.exe` + WinDivert (драйвер ядра) | `ZapretVpnService` — `VpnService` TUN + `DesyncProcessor` (Kotlin) |
| WPF + MVVM | Jetpack Compose + Material 3 + MVVM (StateFlow) |
| `settings.json` / `presets.json` в `%LOCALAPPDATA%` | `DataStore` (JSON) + `files/lists/` |
| WinDivert `--wf-tcp-out=80,443-65535` | `Builder.addAddress/addRoute` TUN 10.111.222.1/32 |
| `--lua-desync=hostfakesplit/fake/multisplit` | `DesyncProcessor` — те же 9 стратегий, парсит `Args` 1:1 |
| Telegram proxy — `TgProxyCore` C# | `TgProxyCore` Kotlin — `ServerSocket 127.0.0.1:1443` → WS-TLS → Cloudflare |
| TCP timestamps `netsh` | `VpnService.protect()` + `setsockopt` (автоматически) |

**Трафик не уходит на чужой VPN-сервер** — TUN локальный, пакеты проходят через `DesyncProcessor` на устройстве и уходят через `protect()` сокет напрямую к сайту.

---

## Возможности (паритет с оригиналом)

- ✅ **Одна кнопка** — поднятие TUN + применение пресета
- ✅ **9 встроенных стратегий** — те же комбо (рекомендуемый, VK-целевой, ALT10/ALT11, Flowseal multisplit, wssize, голос, circular) — см. `StrategyCatalog.kt`
- ✅ **Автоподбор** — перебор каталога с `NetProbe` (TLS 1.2/1.3 + HTTPS) и выбор лучшего (weighted scoring как в оригинале)
- ✅ **Генерация личной стратегии** — комбинация лучших Discord+YouTube компонентов
- ✅ **Память по сетям** — отпечаток `BSSID/транспорт` → стратегия (локально, `DataStore`)
- ✅ **Хостлисты** — `discord.txt` / `youtube.txt` / `exclude.txt` + свои списки, токены `{HOSTLIST:name}`, `{IPSET}`, `{EXCLUDE:}`
- ✅ **IPSet** — сбор подсетей Discord (DoH → CIDR)
- ✅ **Три режима обхода**: только списки (по умолчанию, безопасно), все сайты, игровой фильтр (UDP 443-65535), QUIC off
- ✅ **Telegram прокси** — отдельный `ForegroundService`, без root и без VPN, `tg://proxy?server=127.0.0.1&port=1443&secret=dd…`
- ✅ **Диагностика** — таблица по сервисам + проверка DPI (RST/заморозка/чисто) + лимит по объёму TCP 16-20
- ✅ **Журнал** — две вкладки (Движок / Telegram), 3000 строк, `--debug=1`
- ✅ **Уведомление** — foreground notification с кнопкой «Выключить»
- ✅ **Автозапуск** — `BOOT_COMPLETED` + автозапуск обхода/прокси

---

## Быстрый старт

1. Скачайте `Zapret2UI-Android-v1.0.0-debug.apk` из **Releases** (или соберите сами — см. ниже).
2. Установите (разрешите «Неизвестные источники»).
3. Откройте → нажмите **«Включить обход»** → согласитесь на создание VPN-подключения.
   > Android покажет системный диалог «Запрос на подключение VPN» — это локальный TUN, трафик не уходит вовне.
4. Проверьте Discord / YouTube. Если не открылось: **Диагностика → Подобрать**, затем **Сгенерировать**.
5. Для Telegram: вкладка **Telegram** → включите тумблер → **«Открыть в Telegram»**.

### Требования

- Android 8.0 (API 26) – 15 (API 35), arm64-v8a / armeabi-v7a / x86_64
- Не требует root (с root — опция «Использовать nfqws напрямую»)
- ~15 МБ APK, ~40 МБ установленный

---

## Сборка APK

### Android Studio (рекомендуется)

```bash
git clone https://github.com/YOUR_USER/zapret2UI-Android
cd zapret2UI-Android
# Откройте в Android Studio Hedgehog+ (JDK 17)
# Build → Make Project → Build → Build Bundle(s) / APK(s)
```

APK лежит в `app/build/outputs/apk/debug/app-debug.apk`.

### Командная строка

```bash
./gradlew assembleDebug          # debug APK
./gradlew assembleRelease        # release (подписан debug-ключом, для Play — замените keystore)
```

CI: `.github/workflows/build-apk.yml` собирает APK на каждый push — забирайте из **Actions → Build APK → Artifacts**.

### Без Android SDK (проверка)

```bash
# Проверка синтаксиса Kotlin (без сборки):
./gradlew tasks
```

---

## Архитектура

```
app/src/main/java/com/asterlike/zapret2ui/
├── MainActivity.kt / MainViewModel.kt — единая VM (порт MainViewModel.cs)
├── ZapretApplication.kt
├── data/
│   ├── AppSettings.kt            ← порт AppSettings.cs
│   ├── Preset.kt                 ← порт Preset.cs
│   ├── SettingsRepository.kt     ← DataStore вместо settings.json
│   ├── PresetRepository.kt       ← порт PresetService.cs
│   └── HostlistRepository.kt     ← порт HostlistService.cs
├── engine/
│   ├── StrategyCatalog.kt        ← 9 комбо как в PresetService.BuiltIns()
│   ├── EngineService.kt          ← порт EngineService.cs (BuildArguments 1:1)
│   ├── AutoSelectService.kt      ← порт AutoSelectService.cs
│   ├── StrategyGenerator.kt      ← порт StrategyGenerator
│   └── NativeBinaries.kt         ← updater (SHA256 как в оригинале)
├── vpn/
│   ├── ZapretVpnService.kt       ← замена WinDivert — VpnService TUN
│   ├── DesyncProcessor.kt        ← ядро desync (hostfakesplit/fake/multisplit/drop)
│   └── TunPacketHandler.kt
├── proxy/
│   ├── TgProxyCore.kt            ← порт TgProxyCore.cs
│   └── TgProxyService.kt         ← ForegroundService 127.0.0.1:1443
├── diagnostics/
│   ├── NetProbe.kt               ← порт NetProbe.cs (TLS 1.2/1.3 + HTTPS)
│   ├── DiagnosticsService.kt
│   └── DpiCheckService.kt        ← DPI + volume-limit (TCP 16-20)
├── utils/
│   ├── NetworkFingerprint.kt     ← порт NetworkFingerprint.cs
│   ├── AppPaths.kt
│   └── BootReceiver.kt
└── ui/
    ├── theme/Theme.kt            ← палитра из Themes/Theme.xaml
    ├── navigation/NavGraph.kt
    └── screens/{Home,Strategies,Hostlists,Diagnostics,Log,Telegram,Settings}.kt
```

Токены стратегий совместимы: `{WF_TCP}`/`{WF_UDP}`/`{HOSTLIST}`/`{EXCLUDE:}`/`{IPSET}` разворачиваются так же, поэтому пресеты из десктопа работают на Android (и наоборот).

---

## Стратегии (те же 9)

| Стратегия | Для чего |
|---|---|
| **Комбо (рекомендуемый)** | hostfakesplit (Discord) + fake+multidisorder (YouTube) — начинать с него |
| **Комбо — отечественный (VK)** | Фейки под `vk.com` — если google-варианты «зелёные, но Discord не открывает» |
| **Flowseal ALT10** | Двойной fake + ts — у многих «работает всё» |
| **Flowseal ALT11** | fake+ts → seqovl |
| **Flowseal multisplit** | Разрезка seqovl |
| **Flowseal ALT** | fake+fakedsplit |
| **Комбо — окно (wssize)** | Через уменьшение TCP-окна |
| **Discord — голос (QUIC-фейк)** | «Текст есть, голос подключается» |
| **Discord — адаптивный (circular)** | Оркестратор сам чередует приёмы |

---

## Разрешения

- `INTERNET`, `ACCESS_NETWORK_STATE` — проверка доступности
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` — VPN + прокси
- `RECEIVE_BOOT_COMPLETED` — автозапуск
- `POST_NOTIFICATIONS` (Android 13+) — уведомление «Работает»
- `BIND_VPN_SERVICE` — на `ZapretVpnService` (обязательно)

Никаких `READ_SMS`, `CAMERA`, `LOCATION` (BSSID читается только если уже дано).

---

## Файлы на устройстве

```
/data/data/com.asterlike.zapret2ui/files/
├── lists/
│   ├── discord.txt
│   ├── youtube.txt
│   ├── exclude.txt
│   └── ipset-discord.txt
├── engine/
│   ├── bin/dpi-desync      (если скачан)
│   └── installed_version.txt
└── logs/
```
Настройки — в `DataStore` (`/data/data/.../datastore/`), не в plain JSON.

---

## Благодарности

- [Asterlike/zapret2UI](https://github.com/Asterlike/zapret2UI) — оригинал, UI/UX и 9 стратегий
- [bol-van/zapret2](https://github.com/bol-van/zapret2) — движок `zapret2` / `nfqws` + Lua-логика
- [Flowseal/zapret-discord-youtube](https://github.com/Flowseal/zapret-discord-youtube) — стратегии и `tg-ws-proxy`
- [PowerTunnel](https://github.com/krlvm/PowerTunnel) / [ByeDPIAndroid](https://github.com/dovecoteescapee/ByeDPIAndroid) — идеи TUN-реализации

Лицензия — **MIT**, как у оригинала. Движок `zapret2` — по своей лицензии.

---

## FAQ

**Нужен ли root?** Нет. VPNService достаточно. С root можно включить nfqws напрямую (быстрее, но нужен `su`).

**Будет ли работать на мобильном интернете?** Да. Память по сетям запоминает стратегию отдельно для Wi-Fi и мобильного.

**Почему TUN, а не root + iptables?** TUN работает без root на всех устройствах и не требует перезаписи `iptables` (которую режет SELinux).

**Можно ли импортировать пресеты с десктопа?** Да — скопируйте `presets.json` → через «Стратегии → Импорт» (в разработке) или вручную добавьте `Args` — токены совместимы.

**Где взять APK?** `Releases` → `Zapret2UI-Android-v1.0.0.apk` или `Actions` → последний `Build APK`.

