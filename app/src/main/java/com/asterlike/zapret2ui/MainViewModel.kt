package com.asterlike.zapret2ui

import android.app.Application
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.asterlike.zapret2ui.data.*
import com.asterlike.zapret2ui.diagnostics.DiagnosticsService
import com.asterlike.zapret2ui.diagnostics.DpiCheckService
import com.asterlike.zapret2ui.engine.*
import com.asterlike.zapret2ui.utils.NetworkFingerprint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Порт MainViewModel.cs — единая VM: стратегии, хостлисты, лог, диагностика, VPN.
 * Использует StateFlow вместо ObservableObject, Compose вместо WPF bindings.
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val context = app.applicationContext
    private val settingsRepo = SettingsRepository(context)
    private val presetRepo = PresetRepository(context)
    private val hostlistRepo = HostlistRepository(context)

    val engine = EngineService(context)
    val diagnostics = DiagnosticsService()
    val dpiCheck = DpiCheckService()
    val autoSelect = AutoSelectService(context)
    val generator = StrategyGenerator(context)

    // ---- Settings ----
    val settings: StateFlow<AppSettings> = settingsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    // ---- Presets ----
    private val _presets = MutableStateFlow<List<Preset>>(StrategyCatalog.builtIns())
    val presets: StateFlow<List<Preset>> = _presets
    val selectedPreset = MutableStateFlow<Preset?>(StrategyCatalog.builtIns().firstOrNull { it.isRecommended })

    // ---- Hostlists ----
    val hostlists = MutableStateFlow<List<String>>(emptyList())
    val selectedHostlist = MutableStateFlow<String?>(null)
    val hostlistContent = MutableStateFlow("")

    // ---- Diagnostics ----
    val diagRows = MutableStateFlow(diagnostics.buildRows())
    val isDiagnosing = MutableStateFlow(false)
    val dpiResults = MutableStateFlow<List<Pair<String, DpiCheckService.Verdict>>>(emptyList())
    val isDpiChecking = MutableStateFlow(false)

    // ---- AutoSelect / Generator ----
    val isAutoSelecting = MutableStateFlow(false)
    val autoStatus = MutableStateFlow("")
    val autoScores = MutableStateFlow<List<AutoScore>>(emptyList())
    val isGenerating = MutableStateFlow(false)

    // ---- Log ----
    val logLines: StateFlow<List<String>> = engine.logLines
    val proxyLog = MutableStateFlow<List<String>>(emptyList())

    // ---- Engine State ----
    val engineState: StateFlow<EngineService.EngineState> = engine.state
    val isRunning: StateFlow<Boolean> = engine.state.map { it == EngineService.EngineState.RUNNING }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    val canStart: StateFlow<Boolean> = engine.state.map { it == EngineService.EngineState.STOPPED }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    // ---- UI ----
    val simpleMode = settings.map { it.simpleMode }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)
    val showWelcome = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            // Load hostlists
            hostlists.value = hostlistRepo.listNames()
            // Load presets (built-ins + user)
            presetRepo.allFlow().collect { all -> _presets.value = all }
        }
        viewModelScope.launch {
            settings.collect { s ->
                engine.gameFilter = s.gameFilter
                engine.bypassAllSites = s.bypassAllSites
                engine.disableQuic = s.disableQuic
                engine.coverTgProxy = s.tgProxyCoverage
                engine.debugLog = s.debugLog
                s.activePresetName?.let { name ->
                    selectedPreset.value = _presets.value.find { it.name == name } ?: selectedPreset.value
                }
            }
        }
    }

    // ---- Engine control ----
    fun toggleEngine() {
        viewModelScope.launch {
            if (engine.state.value == EngineService.EngineState.RUNNING) {
                engine.stop()
            } else {
                // Проверяем VPN permission
                val preset = selectedPreset.value ?: StrategyCatalog.builtIns().first()
                val intent = VpnService.prepare(context)
                if (intent != null) {
                    // Нужно разрешение — сигнализируем UI
                    _vpnPermissionNeeded.value = intent
                    return@launch
                }
                engine.start(preset)
                // Сохраняем выбор + память по сетям
                val fp = NetworkFingerprint.current(context)
                settingsRepo.update { it.copy(activePresetName = preset.name, networkStrategies = it.networkStrategies + (fp to preset.name)) }
            }
        }
    }

    private val _vpnPermissionNeeded = MutableStateFlow<Intent?>(null)
    val vpnPermissionNeeded: StateFlow<Intent?> = _vpnPermissionNeeded
    fun clearVpnPermission() { _vpnPermissionNeeded.value = null }

    fun applyPreset(preset: Preset) {
        selectedPreset.value = preset
        viewModelScope.launch {
            settingsRepo.update { it.copy(activePresetName = preset.name) }
            if (engine.state.value == EngineService.EngineState.RUNNING) {
                engine.stop()
                // небольшая задержка перед перезапуском
                kotlinx.coroutines.delay(500)
                engine.start(preset)
            }
        }
    }

    fun duplicatePreset(preset: Preset) {
        viewModelScope.launch { presetRepo.addUser(preset.clone().copy(name = preset.name + " (копия)")) }
    }

    fun deletePreset(preset: Preset) {
        viewModelScope.launch { presetRepo.deleteUser(preset) }
    }

    // ---- Hostlists ----
    fun loadHostlist(name: String) {
        viewModelScope.launch {
            selectedHostlist.value = name
            hostlistContent.value = hostlistRepo.read(name)
        }
    }
    fun saveHostlist() {
        viewModelScope.launch {
            val name = selectedHostlist.value ?: return@launch
            hostlistRepo.write(name, hostlistContent.value)
            hostlists.value = hostlistRepo.listNames()
        }
    }
    fun newHostlist(name: String) {
        viewModelScope.launch {
            hostlistRepo.write(name, "# $name\n")
            hostlists.value = hostlistRepo.listNames()
            selectedHostlist.value = name
        }
    }
    fun deleteHostlist(name: String) {
        viewModelScope.launch {
            hostlistRepo.delete(name)
            hostlists.value = hostlistRepo.listNames()
            if (selectedHostlist.value == name) selectedHostlist.value = null
        }
    }

    // ---- Diagnostics ----
    fun runDiagnostics() {
        viewModelScope.launch {
            isDiagnosing.value = true
            try {
                diagnostics.runBasic(diagRows.value) { /* onUpdate */ }
            } finally { isDiagnosing.value = false }
        }
    }

    fun runDpiCheck() {
        viewModelScope.launch {
            isDpiChecking.value = true
            try {
                dpiResults.value = dpiCheck.check()
            } finally { isDpiChecking.value = false }
        }
    }

    // ---- AutoSelect ----
    fun runAutoSelect() {
        viewModelScope.launch {
            isAutoSelecting.value = true
            autoStatus.value = "Запуск автоподбора…"
            try {
                val candidates = StrategyCatalog.catalogForAutoSelect()
                val hosts = diagnostics.defaultTargets.map { it.first }
                val result = autoSelect.run(candidates, hosts, engine,
                    onCandidateStarted = { name -> autoStatus.value = "Пробую: $name" },
                    onScoreReady = { score -> autoScores.value = autoScores.value + score }
                )
                result?.let { (strategy, score) ->
                    autoStatus.value = "Лучшая: ${strategy.name} — ${score.detail}"
                    // Предлагаем применить
                } ?: run { autoStatus.value = "Не удалось подобрать" }
            } catch (e: Exception) {
                autoStatus.value = "Ошибка: ${e.message}"
            } finally { isAutoSelecting.value = false }
        }
    }

    fun generateStrategy() {
        viewModelScope.launch {
            isGenerating.value = true
            autoStatus.value = "Генерация…"
            try {
                val presets = generator.generate(engine, autoSelect) { s -> autoStatus.value = s }
                // Сохраняем топ-3
                presetRepo.replaceAutoLeaderboard(presets)
                _presets.value = presetRepo.getAll()
                autoStatus.value = "Сгенерировано ${presets.size} стратегий"
            } finally { isGenerating.value = false }
        }
    }

    fun applyAutoScore(score: AutoScore) {
        val preset = score.strategy?.let { AutoSelectService.toPreset(it, "Авто") } ?: return
        viewModelScope.launch { presetRepo.addUser(preset); applyPreset(preset) }
    }
}
