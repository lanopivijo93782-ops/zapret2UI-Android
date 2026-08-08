package com.asterlike.zapret2ui.engine

import android.content.Context
import android.content.Intent
import android.os.Build
import com.asterlike.zapret2ui.data.Preset
import com.asterlike.zapret2ui.utils.AppPaths
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Порт EngineService.cs на Android.
 * На Windows управлял процессом winws2.exe с job-object.
 * На Android управляет ZapretVpnService (TUN + DesyncProcessor) или native nfqws процессом (root).
 *
 * BuildArguments 1:1 с оригиналом — токены разворачиваются так же, чтобы стратегии были переносимы.
 */
class EngineService(private val context: Context) {

    private val _state = MutableStateFlow(EngineState.STOPPED)
    val state: StateFlow<EngineState> = _state

    private val _logLines = MutableStateFlow<List<String>>(emptyList())
    val logLines: StateFlow<List<String>> = _logLines

    var gameFilter: Boolean = false
    var bypassAllSites: Boolean = false
    var disableQuic: Boolean = false
    var coverTgProxy: Boolean = false
    var debugLog: Boolean = false

    var activePreset: Preset? = null
        private set

    enum class EngineState { STOPPED, RUNNING, STARTING, STOPPING }

    fun appendLog(line: String) {
        val list = _logLines.value.toMutableList()
        list.add(line)
        if (list.size > 3000) list.removeAt(0)
        _logLines.value = list
    }

    /** Порт EngineService.BuildArguments — разворачивает токены {WF_TCP} и т.д. */
    fun buildArguments(
        preset: Preset,
        hostlistPath: String? = null,
        gameFilter: Boolean = this.gameFilter,
        bypassAll: Boolean = this.bypassAllSites,
        disableQuic: Boolean = this.disableQuic,
        coverTgProxy: Boolean = this.coverTgProxy,
        debugLog: Boolean = this.debugLog,
        forLaunch: Boolean = false
    ): List<String> {
        val args = mutableListOf<String>()
        if (debugLog) args.add("--debug=1")

        // На Android нет Lua-инитов WinDivert, но оставляем для совместимости стратегий (игнорируются DesyncProcessor)
        // args.add("--lua-init=@...") — не нужно на Android, Kotlin desync

        val wfTcp = "--wf-tcp=80,443-65535" // на Android это TUN filter, не WinDivert
        val wfUdp = if (gameFilter) "--wf-udp=443-65535" else "--wf-udp=443,19294-19344,50000-65535"

        val hostlistArg = if (!hostlistPath.isNullOrBlank()) "--hostlist=$hostlistPath" else ""

        val ipsetFile = AppPaths.ipsetFile(context)
        val ipsetArg = if (ipsetFile.exists()) "--ipset=${ipsetFile.absolutePath}" else ""

        val excludeName = if (bypassAll) effectiveExcludeName(forLaunch) else "exclude"

        for (raw in preset.args) {
            var a = raw
                .replace("{FILES}", AppPaths.filesDir(context).absolutePath)
                .replace("{WF}", AppPaths.engineDir(context).absolutePath)
                .replace("{WF_TCP}", wfTcp)
                .replace("{WF_UDP}", wfUdp)
                .replace("{HOSTLIST}", hostlistArg)
                .replace("{EXCLUDE:exclude}", "{EXCLUDE:$excludeName}")
                .replace("{IPSET}", ipsetArg)

            a = expandNamedHostlists(a)
            a = expandNamedExcludes(a)
            a = expandNamedIpsets(a)

            if (a.isEmpty()) continue
            args.add(a)
        }

        val scoped = if (bypassAll) args else scopeCatchAllToTargets(args)
        val result = if (disableQuic) forceQuicDrop(scoped) else scoped
        if (coverTgProxy) appendTgProxyCoverage(result)
        return result
    }

    fun previewCommandLine(preset: Preset, hostlistPath: String? = null): String {
        val sb = StringBuilder("zapret2")
        for (a in buildArguments(preset, hostlistPath)) {
            sb.append(if (' ' in a) " \"$a\"" else " $a")
        }
        return sb.toString()
    }

    fun start(preset: Preset, hostlistPath: String? = null) {
        if (_state.value == EngineState.RUNNING || _state.value == EngineState.STARTING) {
            throw IllegalStateException("Движок уже запущен")
        }
        _state.value = EngineState.STARTING
        val args = buildArguments(preset, hostlistPath, forLaunch = true)
        appendLog("=== Запущен пресет «${preset.name}» (аргументов: ${args.size}) ===")
        if (debugLog) appendLog("CMD: ${previewCommandLine(preset, hostlistPath)}")

        // Запускаем VPNService — он поднимет TUN и применит desync
        val intent = Intent(context, com.asterlike.zapret2ui.vpn.ZapretVpnService::class.java).apply {
            action = com.asterlike.zapret2ui.vpn.ZapretVpnService.ACTION_START
            putStringArrayListExtra("args", ArrayList(args))
            putExtra("presetName", preset.name)
        }
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent)
        else context.startService(intent)

        activePreset = preset
        _state.value = EngineState.RUNNING
        appendLog("TUN поднят, desync активен")
    }

    fun stop() {
        if (_state.value == EngineState.STOPPED) return
        _state.value = EngineState.STOPPING
        val intent = Intent(context, com.asterlike.zapret2ui.vpn.ZapretVpnService::class.java).apply {
            action = com.asterlike.zapret2ui.vpn.ZapretVpnService.ACTION_STOP
        }
        context.startService(intent)
        activePreset = null
        _state.value = EngineState.STOPPED
        appendLog("=== Движок остановлен ===")
    }

    // ---- helpers порт 1:1 с C# ----

    private fun expandNamedHostlists(a: String): String {
        var s = a
        val marker = "{HOSTLIST:"
        while (true) {
            val i = s.indexOf(marker); if (i < 0) break
            val end = s.indexOf('}', i + marker.length); if (end < 0) break
            val name = s.substring(i + marker.length, end)
            val path = File(AppPaths.listsDir(context), "$name.txt")
            val repl = if (path.exists()) "--hostlist=${path.absolutePath}" else ""
            s = s.substring(0, i) + repl + s.substring(end + 1)
        }
        return s
    }
    private fun expandNamedExcludes(a: String): String {
        var s = a
        val marker = "{EXCLUDE:"
        while (true) {
            val i = s.indexOf(marker); if (i < 0) break
            val end = s.indexOf('}', i + marker.length); if (end < 0) break
            val name = s.substring(i + marker.length, end)
            val path = File(AppPaths.listsDir(context), "$name.txt")
            val repl = if (path.exists()) "--hostlist-exclude=${path.absolutePath}" else ""
            s = s.substring(0, i) + repl + s.substring(end + 1)
        }
        return s
    }
    private fun expandNamedIpsets(a: String): String {
        var s = a
        val marker = "{IPSET:"
        while (true) {
            val i = s.indexOf(marker); if (i < 0) break
            val end = s.indexOf('}', i + marker.length); if (end < 0) break
            val name = s.substring(i + marker.length, end)
            val path = AppPaths.ipsetFile(context, name)
            val repl = if (path.exists()) "--ipset=${path.absolutePath}" else ""
            s = s.substring(0, i) + repl + s.substring(end + 1)
        }
        return s
    }

    private fun effectiveExcludeName(write: Boolean): String {
        return try {
            val targetsPath = File(AppPaths.listsDir(context), "targets.txt")
            val excludePath = File(AppPaths.listsDir(context), "exclude.txt")
            if (!targetsPath.exists() || !excludePath.exists()) return "exclude"
            val targets = targetsPath.readLines().map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
            if (targets.isEmpty()) return "exclude"
            val effPath = File(AppPaths.listsDir(context), "exclude-eff.txt")
            if (write) {
                val kept = excludePath.readLines().filter { line ->
                    val t = line.trim().lowercase(); t.isEmpty() || t !in targets
                }
                effPath.writeText(kept.joinToString("\n"))
                return "exclude-eff"
            }
            if (effPath.exists()) "exclude-eff" else "exclude"
        } catch (_: Exception) { "exclude" }
    }

    private fun appendTgProxyCoverage(args: MutableList<String>) {
        val fronts = File(AppPaths.listsDir(context), "tgproxy-fronts.txt")
        if (!fronts.exists()) return
        args.add("--new")
        args.add("--filter-tcp=443-65535"); args.add("--filter-l7=tls")
        args.add("--hostlist=${fronts.absolutePath}")
        args.add("--dpi-desync=hostfakesplit:host=www.google.com:fooling=md5sig")
    }

    private fun forceQuicDrop(args: List<String>): MutableList<String> {
        val result = mutableListOf<String>()
        val profile = mutableListOf<String>()
        fun flush() {
            if (profile.isEmpty()) return
            val isQuic = profile.any { it.startsWith("--filter-l7=") && "quic" in it }
            if (isQuic) {
                var added = false
                for (a in profile) {
                    if (a.startsWith("--dpi-desync=") || a.startsWith("--lua-desync=")) {
                        if (!added) { result.add("--dpi-desync=drop"); added = true }
                    } else result.add(a)
                }
                if (!added) result.add("--dpi-desync=drop")
            } else result.addAll(profile)
            profile.clear()
        }
        for (a in args) {
            if (a == "--new") flush()
            profile.add(a)
        }
        flush()
        return result
    }

    private fun scopeCatchAllToTargets(args: List<String>): MutableList<String> {
        val targetsPath = File(AppPaths.listsDir(context), "targets.txt")
        val hasTargets = targetsPath.exists() && targetsPath.readLines().any { it.trim().isNotEmpty() }
        val result = mutableListOf<String>()
        val profile = mutableListOf<String>()
        var first = true
        fun flush() {
            if (profile.isEmpty()) return
            val exIdx = profile.indexOfFirst { it.startsWith("--hostlist-exclude=") }
            val scoped = profile.any { it.startsWith("--hostlist=") || it.startsWith("--ipset=") }
            val bareGlobal = !first && exIdx < 0 && !scoped && profile.any {
                it.startsWith("--filter-l7=") && it.substringAfter("=").split(',').any { p -> p.trim() in listOf("tls","quic","http") }
            }
            first = false
            if (exIdx < 0 && !bareGlobal) result.addAll(profile)
            else if (exIdx >= 0 && hasTargets) {
                profile[exIdx] = "--hostlist=${targetsPath.absolutePath}"
                result.addAll(profile)
            }
            // else drop
            profile.clear()
        }
        for (a in args) {
            if (a == "--new") flush()
            profile.add(a)
        }
        flush()
        return result
    }
}
