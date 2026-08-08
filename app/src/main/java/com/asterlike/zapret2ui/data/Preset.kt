package com.asterlike.zapret2ui.data

import kotlinx.serialization.Serializable

/**
 * Порт оригинального Preset.cs на Android/Kotlin.
 * Хранит аргументы zapret (nfqws/dpi-desync) с токенами, разворачиваемыми EngineService.
 *
 * Токены:
 *  {WF_TCP} {WF_UDP} — ширина захвата (на Android не WinDivert, а TUN filter)
 *  {HOSTLIST} {HOSTLIST:name} — хостлист
 *  {EXCLUDE:name} {IPSET} {IPSET:name}
 *  {FILES} — путь к файлам-блбам
 */
@Serializable
data class Preset(
    val name: String = "",
    val description: String = "",
    val tagline: String = "",
    val args: List<String> = emptyList(),
    val usesHostlist: Boolean = true,
    val isBuiltIn: Boolean = false,
    val isRecommended: Boolean = false,
    val isGenerated: Boolean = false,
    val isAutoLeaderboard: Boolean = false
) {
    val groupTitle: String get() = when {
        !isBuiltIn && isAutoLeaderboard -> "★ Лучшие из последней генерации"
        !isBuiltIn && isGenerated -> "✨ Сгенерировано автоподбором"
        !isBuiltIn -> "Личные"
        else -> "Основные (Discord / YouTube)"
    }
    val hasTagline: Boolean get() = tagline.isNotEmpty()
    val primaryLabel: String get() = if (hasTagline) tagline else name
    val secondaryLabel: String get() = if (hasTagline) name else description

    fun clone(): Preset = copy(isBuiltIn = false)
}

enum class EngineState { STOPPED, RUNNING, STARTING, STOPPING }

@Serializable
data class ReleaseInfo(
    val tagName: String,
    val assets: List<ReleaseAsset> = emptyList()
)
@Serializable
data class ReleaseAsset(val name: String, val browserDownloadUrl: String)
