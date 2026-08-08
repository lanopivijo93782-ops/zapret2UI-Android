package com.asterlike.zapret2ui.data

import android.content.Context
import com.asterlike.zapret2ui.utils.AppPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class HostlistRepository(private val context: Context) {

    fun listsDir(): File = File(context.filesDir, "lists").also { it.mkdirs() }

    suspend fun listNames(): List<String> = withContext(Dispatchers.IO) {
        listsDir().listFiles()?.filter { it.extension == "txt" }?.map { it.nameWithoutExtension } ?: emptyList()
    }

    suspend fun read(name: String): String = withContext(Dispatchers.IO) {
        val f = File(listsDir(), "$name.txt")
        if (f.exists()) f.readText() else ""
    }

    suspend fun write(name: String, content: String) = withContext(Dispatchers.IO) {
        File(listsDir(), "$name.txt").writeText(content.trim() + "\n")
    }

    suspend fun delete(name: String) = withContext(Dispatchers.IO) {
        File(listsDir(), "$name.txt").delete()
    }

    /** Встроенные списки — discord/youtube/exclude, обновляются при запуске */
    suspend fun ensureBuiltIns() = withContext(Dispatchers.IO) {
        val dir = listsDir()
        if (!File(dir, "discord.txt").exists()) {
            File(dir, "discord.txt").writeText(builtInDiscord)
        }
        if (!File(dir, "youtube.txt").exists()) {
            File(dir, "youtube.txt").writeText(builtInYoutube)
        }
        if (!File(dir, "exclude.txt").exists()) {
            File(dir, "exclude.txt").writeText(builtInExclude)
        }
    }

    suspend fun aggregateTargets(targets: List<String>): File = withContext(Dispatchers.IO) {
        val f = File(listsDir(), "targets.txt")
        // нормализуем: по домену на строку, lowercase
        val lines = targets.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct().sorted()
        f.writeText(lines.joinToString("\n"))
        f
    }

    private val builtInDiscord = """
discord.com
discord.gg
discord.media
discordapp.com
discordapp.net
gateway.discord.gg
cdn.discordapp.com
canary.discord.com
updates.discord.com
""".trimIndent()

    private val builtInYoutube = """
youtube.com
youtu.be
ytimg.com
googlevideo.com
yt3.ggpht.com
youtube-nocookie.com
youtube-ui.l.google.com
jnn-pa.googleapis.com
""".trimIndent()

    private val builtInExclude = """
sberbank.ru
vtb.ru
alfabank.ru
gosuslugi.ru
nalog.gov.ru
cbr.ru
pochtabank.ru
tinkoff.ru
open.ru
psbank.ru
""".trimIndent()
}
