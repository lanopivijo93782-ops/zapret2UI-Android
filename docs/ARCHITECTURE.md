# Архитектура Android-форка

## Соответствие с оригиналом

Оригинал: .NET 9 / WPF, WinDivert, winws2.exe
Форк: Kotlin / Compose, VpnService Tun, DesyncProcessor

| Оригинал (C#) | Форк (Kotlin) | Примечание |
|---|---|---|
| App.xaml.cs | ZapretApplication.kt | Точка входа, ensure hostlists |
| MainViewModel.cs (2100 строк) | MainViewModel.kt | StateFlow вместо INotifyPropertyChanged |
| EngineService.cs | engine/EngineService.kt | BuildArguments 1:1, управляет VpnService вместо Process |
| Preset.cs | data/Preset.kt | @Serializable, те же поля |
| PresetService.cs | data/PresetRepository.kt + engine/StrategyCatalog.kt | BuiltIns() → StrategyCatalog.builtIns() |
| HostlistService.cs | data/HostlistRepository.kt | lists/*.txt в filesDir |
| SettingsService.cs | data/SettingsRepository.kt | DataStore вместо atomic JSON |
| AppPaths.cs | utils/AppPaths.kt | filesDir/cacheDir |
| NetworkFingerprint.cs | utils/NetworkFingerprint.kt | BSSID/transport hash |
| AutoSelectService.cs | engine/AutoSelectService.kt | weightedOk та же формула |
| StrategyGenerator | engine/StrategyGenerator.kt | та же логика комбо |
| NetProbe.cs | diagnostics/NetProbe.kt | TLS 1.2/1.3 + HTTPS |
| DiagnosticsService.cs | diagnostics/DiagnosticsService.kt | |
| TgProxyCore.cs | proxy/TgProxyCore.kt | ServerSocket 127.0.0.1:1443 |
| TelegramProxyService.cs | proxy/TgProxyService.kt | ForegroundService |
| Engine winws2.exe | vpn/ZapretVpnService.kt + vpn/DesyncProcessor.kt | TUN + desync |
| TcpTimestampsService.cs | VpnService.protect() + setsockopt | Автоматически |

## DesyncProcessor

Порт zapret desync на Kotlin. Поддерживает:

- hostfakesplit — разрез по SNI + фейк с md5sig (отбрасывается сервером, но путает DPI)
- fake — вставка fake ClientHello
- multisplit/multidisorder — разрезка по midsld
- wssize — через setsockopt (не в пакете)
- drop — дроп QUIC
- circular — упрощено как hostfakesplit

SNI парсится из TLS ClientHello (как в zapret2), исключающие списки проверяются.

## VPN

ZapretVpnService создает TUN 10.111.222.1/32, route 0.0.0.0/0.
DesyncProcessor.processOutgoing() вызывается для каждого пакета.
Отправка через protect() сокеты (не через TUN loop).

Для продакшена рекомендуется заменить заглушку packetLoop на полноценный tun2socks (gVisor netstack).
