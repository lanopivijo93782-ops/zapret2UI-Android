package com.asterlike.zapret2ui.utils

import android.content.Context
import java.io.File

object AppPaths {
    fun engineDir(ctx: Context): File = File(ctx.filesDir, "engine").also { it.mkdirs() }
    fun listsDir(ctx: Context): File = File(ctx.filesDir, "lists").also { it.mkdirs() }
    fun logsDir(ctx: Context): File = File(ctx.filesDir, "logs").also { it.mkdirs() }
    fun tmpDir(ctx: Context): File = File(ctx.cacheDir, "tmp").also { it.mkdirs() }

    fun winwsFile(ctx: Context): File = File(engineDir(ctx), "zapret2")
    fun winwsBin(ctx: Context): File = File(engineDir(ctx), "bin/dpi-desync") // alternative binary
    fun versionFile(ctx: Context): File = File(engineDir(ctx), "installed_version.txt")
    fun luaDir(ctx: Context): File = File(engineDir(ctx), "lua")
    fun filesDir(ctx: Context): File = File(engineDir(ctx), "files")
    fun windivertFilterDir(ctx: Context): File = File(engineDir(ctx), "windivert.filter") // kept for token compat
    fun settingsFile(ctx: Context): File = File(ctx.filesDir, "settings.json") // legacy compat, now DataStore
    fun presetsFile(ctx: Context): File = File(ctx.filesDir, "presets.json")
    fun hostlistFile(ctx: Context, name: String): File = File(listsDir(ctx), "$name.txt")
    fun ipsetFile(ctx: Context, name: String = "discord"): File = File(listsDir(ctx), "ipset-$name.txt")
}
