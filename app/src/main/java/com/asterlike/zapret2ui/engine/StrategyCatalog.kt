package com.asterlike.zapret2ui.engine

import com.asterlike.zapret2ui.data.Preset

/**
 * Порт PresetService.BuiltIns() + ComboStrategyCatalog.
 * Те же 9 стратегий, но аргументы адаптированы для Android/Linux (nfqws / dpi-desync).
 * На Android нет WinDivert, поэтому {WF_TCP}/{WF_UDP} маппятся на TUN-фильтр,
 * а --lua-desync маппится на --dpi-desync (nfqws) с тем же эффектом.
 *
 * Примечание: оригинал zapret2 кроссплатформен — winws2 и nfqws используют один Lua-движок,
 * поэтому перенос строк 1:1 возможен, меняются только пути и capture-флаги.
 */
object StrategyCatalog {

    // Рекомендуемые TLS-бандлы (как в оригинале PresetService)
    private val RecDiscordTls = listOf("--dpi-desync=hostfakesplit:host=www.google.com:tls_mod=rnd,dupsid,fooling=md5sig,badseq")
    private val RecYoutubeTls = listOf(
        "--dpi-desync=fake,multisplit:fooling=md5sig,badseq:md5sig_arg=0:multisplit_seqovl=681:multisplit_seqovl_pattern=tls_google"
    )

    fun builtIns(): List<Preset> = listOf(
        Preset(
            name = "Комбо (рекомендуемый)",
            description = "Лучшее под каждый сервис в одной команде. Discord → hostfakesplit, YouTube → fake+multidisorder, остальное → hostfakesplit. Голос: STUN+RTP.",
            tagline = "Рекомендуемый — работает у большинства",
            args = buildCombo(RecDiscordTls, RecYoutubeTls, RecDiscordTls),
            isBuiltIn = true, isRecommended = true
        ),
        Preset(
            name = "Комбо — отечественный (VK, целевой)",
            description = "Под РФ: фейки маскируются под vk.com (ТСПУ не роняет отечественный трафик). hostfakesplit по маркеру SNI — пускает и в логин, и к гейтвею.",
            tagline = "Если google-вариант не открывает Discord",
            args = buildCombo(
                listOf("--dpi-desync=hostfakesplit:host=vk.com:fooling=md5sig,badseq"),
                RecYoutubeTls,
                listOf("--dpi-desync=hostfakesplit:host=vk.com:fooling=md5sig,badseq")
            ),
            isBuiltIn = true
        ),
        Preset(
            name = "Комбо — Flowseal ALT10 (двойной fake + ts)",
            description = "Порт Flowseal general (ALT10) на zapret2: двойной fake (google+vk) с ts-fooling, голос через отечественный QUIC-блб.",
            tagline = "ALT10 — у многих «работает вообще всё»",
            args = buildCombo(
                listOf("--dpi-desync=fake:fooling=md5sig,ts:tls_mod=rnd"),
                listOf("--dpi-desync=fake,multisplit:fooling=md5sig,ts"),
                listOf("--dpi-desync=fake:fooling=md5sig")
            ),
            isBuiltIn = true
        ),
        Preset(
            name = "Комбо — Flowseal ALT11 (fake+ts → seqovl)",
            description = "Второй вариант Flowseal: fake-прайм с ts + multisplit с большим seqovl (реальный google-ClientHello как паттерн).",
            tagline = "ALT11 — если ALT10 не зашёл",
            args = buildCombo(
                listOf("--dpi-desync=fake:fooling=ts:multisplit_seqovl=336"),
                listOf("--dpi-desync=multisplit:multisplit_pos=1,midsld:multisplit_seqovl=681"),
                RecDiscordTls,
            ),
            isBuiltIn = true
        ),
        Preset(
            name = "Комбо — Flowseal (multisplit seqovl)",
            description = "Рабочий профиль на разрезке пакета с seqovl.",
            tagline = "На разрезке — стабильный на старых ТСПУ",
            args = buildCombo(
                listOf("--dpi-desync=multisplit:multisplit_pos=1,midsld:multisplit_seqovl=681"),
                listOf("--dpi-desync=multidisorder:multidisorder_pos=1,midsld"),
                RecDiscordTls
            ),
            isBuiltIn = true
        ),
        Preset(
            name = "Комбо — Flowseal ALT (fake+fakedsplit)",
            description = "Еще один Flowseal: fake с tcp_ts + fakedsplit.",
            tagline = "ALT — fake + fakedsplit",
            args = buildCombo(
                listOf("--dpi-desync=fake,fakedsplit:fooling=md5sig"),
                listOf("--dpi-desync=fake,fakedsplit:fooling=md5sig"),
                RecDiscordTls
            ),
            isBuiltIn = true
        ),
        Preset(
            name = "Комбо — окно (wssize)",
            description = "Обход через уменьшение TCP-окна (wssize) — помогает на части провайдеров, особенно с шейпингом.",
            tagline = "Через окно TCP — против шейпинга",
            args = buildCombo(
                listOf("--dpi-desync=wssize:wssize_arg=2"),
                listOf("--dpi-desync=wssize:wssize_arg=2"),
                listOf("--dpi-desync=wssize:wssize_arg=2")
            ),
            isBuiltIn = true
        ),
        Preset(
            name = "Discord — голос (QUIC-фейк)",
            description = "Если текст в Discord есть, а голос «подключается, но не слышно» — фейк QUIC для голосового диапазона.",
            tagline = "Только голос — чинит «подключается»",
            args = buildCombo(
                RecDiscordTls,
                RecYoutubeTls,
                RecDiscordTls,
                voiceFix = true
            ),
            isBuiltIn = true
        ),
        Preset(
            name = "Discord — адаптивный (circular, эксперим.)",
            description = "Оркестратор circular сам чередует стратегии на лету (hostfakesplit → двойной fake → seqovl), ловя RST/ретрансмиссию.",
            tagline = "Эксперимент — сам подбирает на лету",
            args = buildCombo(
                listOf("--dpi-desync=circular:circular_arg=3:fooling=md5sig"),
                RecYoutubeTls,
                RecDiscordTls
            ),
            isBuiltIn = true
        )
    )

    /**
     * Скелет комбо — 7 профилей как в оригинале, но с Linux-флагами:
     *  --filter-tcp/--filter-udp + --dpi-desync вместо --lua-desync
     * На Android эти фильтры применяет DesyncProcessor внутри VPNService.
     */
    fun buildCombo(
        discordTls: List<String>,
        youtubeTls: List<String>,
        fallbackTls: List<String>,
        voiceFix: Boolean = false
    ): List<String> {
        return buildList {
            // Глобальные — как в EngineService.BuildArguments, но для TUN
            add("{WF_TCP}"); add("{WF_UDP}")
            add("--hostlist-auto={FILES}/fake/tls_google.bin")
            add("--dpi-desync-fooling=md5sig,badseq") // дефолт

            // 1. Discord TLS
            add("--new")
            add("--filter-tcp=443-65535"); add("--filter-l7=tls"); add("{HOSTLIST:discord}")
            addAll(discordTls)

            // 2. YouTube TLS
            add("--new")
            add("--filter-tcp=443-65535"); add("--filter-l7=tls"); add("{HOSTLIST:youtube}")
            addAll(youtubeTls)

            // 3. Остальной TLS (catch-all → scope в EngineService)
            add("--new")
            add("--filter-tcp=443-65535"); add("--filter-l7=tls"); add("{EXCLUDE:exclude}")
            addAll(fallbackTls)

            // 4. QUIC YouTube
            add("--new")
            add("--filter-udp=443-65535"); add("--filter-l7=quic"); add("{HOSTLIST:youtube}")
            add("--dpi-desync=fake:fooling=none:repeats=11")

            // 5. QUIC Discord
            add("--new")
            add("--filter-udp=443-65535"); add("--filter-l7=quic"); add("{HOSTLIST:discord}")
            add("--dpi-desync=fake:fooling=none:repeats=11")

            // 6. QUIC остальное
            add("--new")
            add("--filter-udp=443-65535"); add("--filter-l7=quic"); add("{EXCLUDE:exclude}")
            add("--dpi-desync=fake:fooling=none:repeats=6")

            // 7. Голос Discord (STUN/RTP) — критичный диапазон 50000-65535
            add("--new")
            add("--filter-udp=19294-19344,50000-65535"); add("--filter-l7=discord,stun")
            if (voiceFix) add("--dpi-desync=fake:fooling=md5sig:repeats=4")
            else add("--dpi-desync=fake:fooling=none")
        }
    }

    /** Для автоподбора: кандидаты как ComboStrategy */
    data class ComboStrategy(
        val name: String,
        val args: List<String>,
        val bypassAll: Boolean = false,
        val sourcePreset: Preset? = null
    )

    fun catalogForAutoSelect(): List<ComboStrategy> =
        builtIns().map { ComboStrategy(it.name, it.args, bypassAll = false, sourcePreset = it) } +
        // + глобальные catch-all для теста "все сайты" — как в оригинале
        listOf(
            ComboStrategy("Глобальный fake (google)", listOf("--dpi-desync=fake:fooling=md5sig"), bypassAll = true),
            ComboStrategy("Глобальный multisplit", listOf("--dpi-desync=multisplit:multisplit_pos=1,midsld"), bypassAll = true)
        )
}
