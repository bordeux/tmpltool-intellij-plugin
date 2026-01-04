import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.2.1"
}

group = "com.bordeux"
version = System.getenv("VERSION") ?: "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    intellijPlatform {
        intellijIdeaCommunity("2025.1")
        bundledPlugin("com.intellij.java")
    }
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginConfiguration {
        id = "com.bordeux.tmpltool"
        name = "Tmpltool Template Support"
        version = project.version.toString()
        description = """
            Syntax highlighting and language support for tmpltool template files (.tmpltool).

            Features:
            - Syntax highlighting for Jinja2-like template syntax
            - Base language detection from double extensions (e.g., .yaml.tmpltool → YAML + templates)
            - Code completion for tmpltool built-in functions
            - Support for {{ }}, {% %}, and {# #} syntax
        """.trimIndent()

        vendor {
            name = "Chris Bednarczyk"
            email = "tmpltool@bordeux.net"
            url = "https://github.com/bordeux/tmpltool"
        }

        ideaVersion {
            sinceBuild = "251"
            untilBuild = provider { null }
        }
    }

    publishing {
        token = providers.environmentVariable("JETBRAINS_TOKEN")
    }

    pluginVerification {
        ides {
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2025.1")
        }
    }
}

tasks {
    buildSearchableOptions {
        enabled = false
    }

    patchPluginXml {
        // Read change notes from CHANGELOG.md if available
        val changelog = file("CHANGELOG.md")
        if (changelog.exists()) {
            changeNotes = provider {
                changelog.readText()
                    .lines()
                    .dropWhile { !it.startsWith("## ") }
                    .takeWhile { it.isNotEmpty() || !it.startsWith("## ") }
                    .take(50) // Limit to recent changes
                    .joinToString("\n")
                    .let { "<pre>$it</pre>" }
            }
        } else {
            changeNotes = """
                <h2>${project.version}</h2>
                <ul>
                    <li>See <a href="https://github.com/bordeux/tmpltool-intellij-plugin/releases">GitHub Releases</a> for details</li>
                </ul>
            """.trimIndent()
        }
    }

    runIde {
        // Open examples folder as project when running sandbox IDE
        args = listOf(file("examples").absolutePath)
    }
}
