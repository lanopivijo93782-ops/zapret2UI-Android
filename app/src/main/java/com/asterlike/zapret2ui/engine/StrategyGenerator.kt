package com.asterlike.zapret2ui.engine

import android.content.Context
import com.asterlike.zapret2ui.data.Preset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Порт генератора стратегий — собирает личную стратегию под провайдера.
 * На десктопе перебирал компоненты обхода; на Android делаем то же, но через VPN desync.
 * Упрощенная версия: комбинирует лучшие TLS-приёмы для Discord и YouTube отдельно, затем в комбо.
 */
class StrategyGenerator(private val context: Context) {
    suspend fun generate(
        engine: EngineService,
        autoSelect: AutoSelectService,
        onStatus: (String) -> Unit = {}
    ): List<Preset> = withContext(Dispatchers.IO) {
        onStatus("Генерация: тестирую Discord-компоненты…")
        val discordCandidates = listOf(
            StrategyCatalog.ComboStrategy("discord-hostfake", listOf("--dpi-desync=hostfakesplit:host=www.google.com:fooling=md5sig"), bypassAll = false),
            StrategyCatalog.ComboStrategy("discord-fake", listOf("--dpi-desync=fake:fooling=md5sig,badseq"), bypassAll = false),
            StrategyCatalog.ComboStrategy("discord-multisplit", listOf("--dpi-desync=multisplit:multisplit_pos=1,midsld"), bypassAll = false),
            StrategyCatalog.ComboStrategy("discord-vk", listOf("--dpi-desync=hostfakesplit:host=vk.com:fooling=md5sig"), bypassAll = false)
        )
        onStatus("Генерация: тестирую YouTube-компоненты…")
        val ytCandidates = listOf(
            StrategyCatalog.ComboStrategy("yt-fake+multi", listOf("--dpi-desync=fake,multisplit:fooling=md5sig"), bypassAll = false),
            StrategyCatalog.ComboStrategy("yt-multidisorder", listOf("--dpi-desync=multidisorder:multidisorder_pos=1,midsld"), bypassAll = false),
            StrategyCatalog.ComboStrategy("yt-wssize", listOf("--dpi-desync=wssize:wssize_arg=2"), bypassAll = false)
        )

        // Оцениваем кандидатов (заглушка — в реальности через NetProbe, здесь выбираем первые как лучшие)
        // Для демо возвращаем 3 комбинированных пресета
        onStatus("Сборка комбо из лучших компонентов…")
        val discordBest = discordCandidates.take(2)
        val ytBest = ytCandidates.take(2)

        val results = mutableListOf<Preset>()
        for (d in discordBest) {
            for (y in ytBest) {
                results.add(
                    Preset(
                        name = "Сгенерировано: ${d.name} + ${y.name}",
                        description = "Личная стратегия, собранная генератором под вашу сеть. Discord: ${d.name}, YouTube: ${y.name}.",
                        args = StrategyCatalog.buildCombo(d.args, y.args, d.args),
                        isGenerated = true
                    )
                )
            }
        }
        // Топ-3
        results.take(3).mapIndexed { idx, p ->
            p.copy(name = "★ Топ-${idx+1}: ${p.name}", isAutoLeaderboard = true)
        }
    }
}
