package com.bordeux.tmpltool.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import java.io.File

/**
 * Application-level settings for tmpltool plugin.
 * Stores the path to the tmpltool binary.
 */
@Service(Service.Level.APP)
@State(
    name = "TmplSettings",
    storages = [Storage("tmpltool.xml")]
)
class TmplSettings : PersistentStateComponent<TmplSettings> {

    /**
     * Custom path to tmpltool binary. Empty string means auto-detect.
     */
    var tmpltoolPath: String = ""

    override fun getState(): TmplSettings = this

    override fun loadState(state: TmplSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    /**
     * Gets the effective tmpltool path - either custom or auto-detected.
     * Returns null if tmpltool cannot be found.
     */
    fun getEffectivePath(): String? {
        if (tmpltoolPath.isNotBlank()) {
            val customPath = File(tmpltoolPath)
            if (customPath.exists() && customPath.canExecute()) {
                return tmpltoolPath
            }
        }
        return detectTmpltoolPath()
    }

    /**
     * Auto-detect tmpltool binary in common locations.
     */
    private fun detectTmpltoolPath(): String? {
        // Try 'which' command on Unix-like systems
        val pathFromWhich = runCatching {
            val process = ProcessBuilder("which", "tmpltool")
                .redirectErrorStream(true)
                .start()
            val result = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            if (process.exitValue() == 0 && result.isNotEmpty() && File(result).exists()) {
                result
            } else {
                null
            }
        }.getOrNull()

        if (pathFromWhich != null) return pathFromWhich

        // Try 'where' command on Windows
        val pathFromWhere = runCatching {
            val process = ProcessBuilder("where", "tmpltool")
                .redirectErrorStream(true)
                .start()
            val result = process.inputStream.bufferedReader().readText().trim().lines().firstOrNull()
            process.waitFor()
            if (process.exitValue() == 0 && !result.isNullOrEmpty() && File(result).exists()) {
                result
            } else {
                null
            }
        }.getOrNull()

        if (pathFromWhere != null) return pathFromWhere

        // Check common installation paths
        val commonPaths = listOf(
            "/usr/local/bin/tmpltool",
            "/usr/bin/tmpltool",
            "/opt/homebrew/bin/tmpltool",
            "${System.getProperty("user.home")}/.cargo/bin/tmpltool",
            "${System.getProperty("user.home")}/.local/bin/tmpltool",
            "C:\\Program Files\\tmpltool\\tmpltool.exe",
            "C:\\Users\\${System.getProperty("user.name")}\\.cargo\\bin\\tmpltool.exe"
        )

        for (path in commonPaths) {
            val file = File(path)
            if (file.exists() && file.canExecute()) {
                return path
            }
        }

        return null
    }

    companion object {
        @JvmStatic
        fun getInstance(): TmplSettings {
            return ApplicationManager.getApplication().getService(TmplSettings::class.java)
        }
    }
}
