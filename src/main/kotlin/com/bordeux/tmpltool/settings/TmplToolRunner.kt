package com.bordeux.tmpltool.settings

import com.intellij.openapi.diagnostic.Logger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

/**
 * Utility to execute tmpltool CLI and parse its output.
 */
object TmplToolRunner {

    private val LOG = Logger.getInstance(TmplToolRunner::class.java)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Raw data class matching the JSON structure from `tmpltool --ide json`
     */
    @Serializable
    data class RawFunctionData(
        val name: String,
        val category: String,
        val description: String,
        val arguments: List<RawArgumentData>,
        @SerialName("return_type")
        val returnType: String,
        val examples: List<String>,
        val syntax: SyntaxInfo
    )

    @Serializable
    data class RawArgumentData(
        val name: String,
        @SerialName("arg_type")
        val argType: String,
        val required: Boolean,
        val default: String? = null,
        val description: String
    )

    @Serializable
    data class SyntaxInfo(
        val function: Boolean,
        val filter: Boolean,
        @SerialName("is_test")
        val isTest: Boolean
    )

    /**
     * Result of running tmpltool
     */
    sealed class RunResult {
        data class Success(val functions: List<RawFunctionData>) : RunResult()
        data class Error(val message: String) : RunResult()
    }

    /**
     * Execute `tmpltool --ide json` and parse the result.
     */
    fun fetchFunctionData(): RunResult {
        val tmpltoolPath = TmplSettings.getInstance().getEffectivePath()
            ?: return RunResult.Error("tmpltool binary not found")

        return try {
            val process = ProcessBuilder(tmpltoolPath, "--ide", "json")
                .redirectErrorStream(false)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val errorOutput = process.errorStream.bufferedReader().readText()

            val completed = process.waitFor(30, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                return RunResult.Error("tmpltool timed out after 30 seconds")
            }

            if (process.exitValue() != 0) {
                return RunResult.Error("tmpltool exited with code ${process.exitValue()}: $errorOutput")
            }

            if (output.isBlank()) {
                return RunResult.Error("tmpltool returned empty output")
            }

            val functions: List<RawFunctionData> = json.decodeFromString(output)

            LOG.info("Loaded ${functions.size} functions from tmpltool")
            RunResult.Success(functions)

        } catch (e: Exception) {
            LOG.warn("Failed to run tmpltool", e)
            RunResult.Error("Failed to run tmpltool: ${e.message}")
        }
    }
}