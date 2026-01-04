package com.bordeux.tmpltool.injection

import com.intellij.lang.Language
import com.intellij.openapi.vfs.VirtualFile

/**
 * Detects the base language from a .tmpltool file's double extension.
 * For example: docker-compose.yaml.tmpltool -> YAML
 */
object TmplBaseLanguageProvider {

    private val extensionToLanguage = mapOf(
        "yaml" to "yaml",
        "yml" to "yaml",
        "json" to "JSON",
        "xml" to "XML",
        "html" to "HTML",
        "htm" to "HTML",
        "xhtml" to "XHTML",
        "md" to "Markdown",
        "markdown" to "Markdown",
        "toml" to "TOML",
        "ini" to "Ini",
        "conf" to "Ini",
        "cfg" to "Ini",
        "properties" to "Properties",
        "sh" to "Shell Script",
        "bash" to "Shell Script",
        "zsh" to "Shell Script",
        "sql" to "SQL",
        "js" to "JavaScript",
        "ts" to "TypeScript",
        "css" to "CSS",
        "scss" to "SCSS",
        "less" to "LESS",
        "py" to "Python",
        "rb" to "Ruby",
        "php" to "PHP",
        "java" to "JAVA",
        "kt" to "Kotlin",
        "go" to "Go",
        "rs" to "Rust",
        "c" to "ObjectiveC",
        "cpp" to "ObjectiveC",
        "h" to "ObjectiveC",
        "tf" to "HCL",
        "hcl" to "HCL",
        "graphql" to "GraphQL",
        "gql" to "GraphQL",
        "env" to "Env",
    )

    /**
     * Gets the base language for a .tmpltool file based on its double extension.
     * Returns null if no base language can be determined.
     */
    fun getBaseLanguage(file: VirtualFile?): Language? {
        if (file == null) return null

        val name = file.name
        if (!name.endsWith(".tmpltool")) return null

        // Extract the second-to-last extension
        // e.g., "docker-compose.yaml.tmpltool" -> "yaml"
        val withoutTmpl = name.removeSuffix(".tmpltool")
        val baseExtension = withoutTmpl.substringAfterLast('.', "")

        if (baseExtension.isEmpty() || baseExtension == withoutTmpl) {
            return null
        }

        val languageId = extensionToLanguage[baseExtension.lowercase()] ?: return null

        return Language.findLanguageByID(languageId)
            ?: Language.getRegisteredLanguages().find {
                it.displayName.equals(languageId, ignoreCase = true)
            }
    }

    /**
     * Gets the base extension without .tmpltool
     */
    fun getBaseExtension(file: VirtualFile?): String? {
        if (file == null) return null

        val name = file.name
        if (!name.endsWith(".tmpltool")) return null

        val withoutTmpl = name.removeSuffix(".tmpltool")
        val baseExtension = withoutTmpl.substringAfterLast('.', "")

        return if (baseExtension.isNotEmpty() && baseExtension != withoutTmpl) {
            baseExtension
        } else {
            null
        }
    }
}
