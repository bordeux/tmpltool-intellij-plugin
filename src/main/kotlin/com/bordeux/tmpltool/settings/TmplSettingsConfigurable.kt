package com.bordeux.tmpltool.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.*
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Settings page for tmpltool plugin.
 * Available at: Settings > Tools > Tmpltool
 */
class TmplSettingsConfigurable : Configurable {

    private var panel: JPanel? = null
    private var pathField: TextFieldWithBrowseButton? = null
    private var statusLabel: JLabel? = null

    override fun getDisplayName(): String = "Tmpltool"

    override fun createComponent(): JComponent {
        val settings = TmplSettings.getInstance()

        panel = panel {
            group("Tmpltool Binary") {
                row("Path:") {
                    pathField = textFieldWithBrowseButton(
                        FileChooserDescriptorFactory.createSingleFileDescriptor()
                            .withTitle("Select tmpltool Binary")
                    ).columns(COLUMNS_LARGE)
                        .comment("Leave empty to auto-detect from PATH")
                        .component
                }

                row("Status:") {
                    statusLabel = label("").component
                }

                row {
                    button("Detect") {
                        pathField?.text = ""
                        updateStatus()
                    }
                    button("Test") {
                        updateStatus()
                    }
                }
            }

            group("Information") {
                row {
                    text("""
                        The tmpltool binary is used to provide accurate function completions and documentation.
                        <br/><br/>
                        Install tmpltool via:
                        <ul>
                            <li><code>cargo install tmpltool</code></li>
                            <li>Or download from <a href="https://github.com/bordeux/tmpltool/releases">GitHub releases</a></li>
                        </ul>
                    """.trimIndent())
                }
            }
        }

        // Initialize with current settings
        pathField?.text = settings.tmpltoolPath
        updateStatus()

        return panel!!
    }

    private fun updateStatus() {
        val settings = TmplSettings.getInstance()
        val tempPath = pathField?.text ?: ""

        // Temporarily set the path to test it
        val originalPath = settings.tmpltoolPath
        settings.tmpltoolPath = tempPath

        val effectivePath = settings.getEffectivePath()

        // Restore original path
        settings.tmpltoolPath = originalPath

        if (effectivePath != null) {
            // Try to get version
            val version = runCatching {
                val process = ProcessBuilder(effectivePath, "--version")
                    .redirectErrorStream(true)
                    .start()
                val output = process.inputStream.bufferedReader().readText().trim()
                process.waitFor()
                if (process.exitValue() == 0) output else null
            }.getOrNull()

            if (version != null) {
                statusLabel?.text = "<html><font color='green'>✓ Found: $effectivePath ($version)</font></html>"
            } else {
                statusLabel?.text = "<html><font color='orange'>⚠ Found but cannot execute: $effectivePath</font></html>"
            }
        } else {
            statusLabel?.text = "<html><font color='red'>✗ Not found. Please install tmpltool or specify the path.</font></html>"
        }
    }

    override fun isModified(): Boolean {
        val settings = TmplSettings.getInstance()
        return pathField?.text != settings.tmpltoolPath
    }

    override fun apply() {
        val settings = TmplSettings.getInstance()
        settings.tmpltoolPath = pathField?.text ?: ""

        // Reload the function registry with new settings
        com.bordeux.tmpltool.completion.TmplFunctionRegistry.reload()
    }

    override fun reset() {
        val settings = TmplSettings.getInstance()
        pathField?.text = settings.tmpltoolPath
        updateStatus()
    }

    override fun disposeUIResources() {
        panel = null
        pathField = null
        statusLabel = null
    }
}