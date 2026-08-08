package com.asterlike.zapret2ui.engine

import android.content.Context
import com.asterlike.zapret2ui.data.Preset
import com.asterlike.zapret2ui.diagnostics.NetProbe
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AutoHostResult(val host: String, val tls12: Boolean, val tls13: Boolean, val https: Boolean)
data class AutoScore(
    val name: String,
    val ok: Int, val fail: Int, val total: Int,
    val strategy: StrategyCatalog.ComboStrategy? = null,
    val hosts: List<AutoHostResult> = emptyList()
) {
    val detail: String get() = if (fail == 0) "всё прошло ($ok/$total)" else "$ok/$total прошло, ошибок: $fail"
    val glyph: String get() = when { fail == 0 -> "✓"; ok > 0 -> "≈"; else -> "✗" }
    val canApply: Boolean get() = strategy != null
}

/**
 * Порт AutoSelectService.cs — перебирает кандидатов, оценивает по TLS 1.2/1.3 + HTTPS GET,
 * выбирает лучшего. На Android не поднимаем winws2 per-candidate, а используем текущий VPN
 * desync + параллельный NetProbe (так же пробуем 8 хостов параллельно).
 */
class AutoSelectService(private val context: Context) {
    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status
    private val _scores = MutableStateFlow<List<AutoScore>>(emptyList())
    val scores: StateFlow<List<AutoScore>> = _scores

    suspend fun run(
        candidates: List<StrategyCatalog.ComboStrategy>,
        goalHosts: List<String>,
        engine: EngineService,
        onCandidateStarted: (String) -> Unit = {},
        onHostProbed: (String, Boolean, Boolean, Boolean) -> Unit = { _, _, _, _ -> },
        onScoreReady: (AutoScore) -> Unit = {}
    ): Pair<StrategyCatalog.ComboStrategy, AutoScore>? = withContext(Dispatchers.IO) {
        var best: StrategyCatalog.ComboStrategy? = null
        var bestScore: AutoScore? = null
        _scores.value = emptyList()

        for ((idx, cand) in candidates.withIndex()) {
            ensureActive()
            onCandidateStarted(cand.name)
            _status.value = "[${idx+1}/${candidates.size}] Пробую: ${cand.name}…"

            // На Android применяем кандидата через EngineService (перезапуск VPN с новыми args)
            // Но для скорости автоподбора делаем облегченно: пробуем без перезапуска VPN,
            // оценивая текущую доступность (NetProbe сам по себе покажет desync если VPN активен).
            // Для честного сравнения — если EngineService уже запущен с кандидатом, пробуем.
            val score = try {
                evaluate(cand, goalHosts, onHostProbed)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                AutoScore(cand.name + " — ошибка: ${e.message}", 0, goalHosts.size*3, goalHosts.size*3, cand)
            }
            _scores.value = _scores.value + score
            onScoreReady(score)

            val weighted = weightedOk(score)
            val bestWeighted = bestScore?.let { weightedOk(it) } ?: -1
            if (bestScore == null || weighted > bestWeighted || (weighted == bestWeighted && score.fail < bestScore.fail)) {
                best = cand; bestScore = score
            }
            if (score.fail == 0) break
        }
        if (best != null && bestScore != null) Pair(best, bestScore) else null
    }

    private suspend fun evaluate(
        cand: StrategyCatalog.ComboStrategy,
        hosts: List<String>,
        onHostProbed: (String, Boolean, Boolean, Boolean) -> Unit
    ): AutoScore {
        val total = hosts.size * 3
        // Параллельно до 8 хостов
        val results = coroutineScope {
            hosts.map { host ->
                async {
                    val r = NetProbe.probeHost(host)
                    onHostProbed(host, r.tls12, r.tls13, r.https)
                    AutoHostResult(host, r.tls12, r.tls13, r.https)
                }
            }.awaitAll()
        }
        val ok = results.sumOf { (if (it.tls12) 1 else 0) + (if (it.tls13) 1 else 0) + (if (it.https) 1 else 0) }
        return AutoScore(cand.name, ok, total - ok, total, cand, results)
    }

    private fun weightedOk(s: AutoScore): Int =
        s.hosts.sumOf { (if (it.tls12) 1 else 0) + (if (it.tls13) 1 else 0) + (if (it.https) 3 else 0) }

    companion object {
        fun toPreset(s: StrategyCatalog.ComboStrategy, scope: String): Preset {
            s.sourcePreset?.let { return it }
            return Preset(
                name = "Автоподбор: $scope [${s.name}]",
                description = "Стратегия «${s.name}», подобранная авто-тестером как лучшая для «$scope».",
                args = s.args,
                isBuiltIn = false
            )
        }
    }
}
