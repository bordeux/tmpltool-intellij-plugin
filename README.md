# Tmpltool IntelliJ Plugin

Syntax highlighting and language support for [tmpltool](https://github.com/bordeux/tmpltool) template files (`.tmpltool`) in all JetBrains IDEs.

## Features

- **Syntax highlighting** for Jinja2-like template syntax
- **Base language detection** from double extensions (e.g., `.yaml.tmpltool` → YAML highlighting + template highlighting)
- **Template tag support:**
  - Expression tags: `{{ variable }}`
  - Statement tags: `{% if condition %}`
  - Comment tags: `{# comment #}`
- **Bracket matching** for template delimiters
- **Block commenting** with `{# #}`
- **Customizable color scheme**

## Supported IDEs

This plugin works in all JetBrains IDEs:

- IntelliJ IDEA (Community & Ultimate)
- PhpStorm
- WebStorm
- PyCharm
- GoLand
- RubyMine
- CLion
- Rider
- DataGrip
- Android Studio

## Supported File Patterns

| Pattern | Base Language |
|---------|---------------|
| `*.yaml.tmpltool`, `*.yml.tmpltool` | YAML |
| `*.json.tmpltool` | JSON |
| `*.xml.tmpltool` | XML |
| `*.html.tmpltool` | HTML |
| `*.md.tmpltool` | Markdown |
| `*.toml.tmpltool` | TOML |
| `*.sh.tmpltool` | Shell Script |
| `*.sql.tmpltool` | SQL |
| `*.tmpltool` | Plain Text |

## Development

### Prerequisites

- JDK 17+
- IntelliJ IDEA (for development)

### Build

```bash
./gradlew build
```

### Run in Sandbox IDE

```bash
# IntelliJ IDEA
./gradlew runIde

# WebStorm
./gradlew runIde -PplatformType=WS

# PhpStorm
./gradlew runIde -PplatformType=PS

# PyCharm
./gradlew runIde -PplatformType=PC
```

### Build Plugin Distribution

```bash
./gradlew buildPlugin
```

The plugin zip will be in `build/distributions/`.

## Installation

### From JetBrains Marketplace

1. Open your IDE
2. Go to **Settings** → **Plugins** → **Marketplace**
3. Search for "Tmpltool"
4. Click **Install**

### From ZIP

1. Download the plugin zip from [Releases](https://github.com/bordeux/tmpltool-intellij-plugin/releases)
2. Open your IDE
3. Go to **Settings** → **Plugins** → ⚙️ → **Install Plugin from Disk...**
4. Select the downloaded zip file

## License

MIT License - see [LICENSE](LICENSE) for details.

## Links

- [tmpltool](https://github.com/bordeux/tmpltool) - The template rendering CLI tool
- [JetBrains Marketplace](https://plugins.jetbrains.com/) - Plugin marketplace
