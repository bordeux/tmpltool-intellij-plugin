# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.

## Project Overview

This is an **IntelliJ Platform plugin** that provides IDE support for [tmpltool](https://github.com/bordeux/tmpltool) template files (`.tmpltool` extension).

**tmpltool** is a Rust-based CLI tool that renders Jinja2-compatible templates with environment variables and 50+ built-in functions.

This plugin works in **all JetBrains IDEs**: IntelliJ IDEA, PhpStorm, WebStorm, PyCharm, GoLand, RubyMine, CLion, Rider, DataGrip, Android Studio.

## Key Design Decisions

### File Extension: `.tmpltool` (not `.tmpl`)
We use `.tmpltool` to avoid conflicts with:
- Go templates (`text/template`, `html/template`)
- Helm charts
- Consul Template
- Other tools that use the generic `.tmpl` extension

### Double Extension Pattern
Files like `docker-compose.yaml.tmpltool` get:
1. **YAML syntax highlighting** for the base content
2. **Template syntax highlighting** for `{{ }}`, `{% %}`, `{# #}` regions

This is implemented via `TmplFileViewProvider` (dual PSI tree) and `TmplTemplateHighlighterProvider` (layered highlighting).

## Architecture

```
src/main/kotlin/com/bordeux/tmpltool/
├── TmplLanguage.kt                 # Language definition singleton
├── TmplFileType.kt                 # .tmpltool file type registration
├── TmplIcons.kt                    # Plugin icons
├── TmplFileViewProvider.kt         # Dual PSI tree (template + base language)
├── TmplFileViewProviderFactory.kt  # Creates view providers
├── TmplBraceMatcher.kt             # Bracket matching {{ }}, {% %}, {# #}
├── TmplCommenter.kt                # Block commenting with {# #}
├── TmplTemplateContextType.kt      # Live template context
│
├── lexer/
│   ├── TmplLexer.kt                # Hand-written lexer for template syntax
│   └── TmplTokenTypes.kt           # Token definitions (EXPR_START, KEYWORD, etc.)
│
├── parser/
│   ├── TmplParser.kt               # Minimal parser (consumes all tokens)
│   ├── TmplParserDefinition.kt     # Parser definition for IntelliJ
│   ├── TmplFile.kt                 # PSI file implementation
│   └── TmplPsiElement.kt           # Generic PSI element
│
├── highlighter/
│   ├── TmplSyntaxHighlighter.kt        # Token-to-color mapping
│   ├── TmplSyntaxHighlighterFactory.kt # Factory for syntax highlighter
│   ├── TmplColorSettingsPage.kt        # Settings > Editor > Color Scheme
│   └── TmplTemplateHighlighterProvider.kt # Layered highlighting (base + template)
│
├── injection/
│   └── TmplBaseLanguageProvider.kt # Detects base language from extension
│
├── completion/
│   ├── TmplFunctionRegistry.kt     # Dynamic function loading from CLI
│   ├── TmplCompletionContributor.kt # Code completion provider
│   ├── TmplDocumentationProvider.kt # Hover documentation
│   └── TmplParameterInfoHandler.kt # Parameter hints (Ctrl+P)
│
├── settings/
│   ├── TmplSettings.kt             # Application settings (tmpltool path)
│   ├── TmplSettingsConfigurable.kt # Settings UI (Tools > Tmpltool)
│   └── TmplToolRunner.kt           # Execute tmpltool CLI and parse JSON
│
├── annotator/
│   └── TmplAnnotator.kt            # Error highlighting (unclosed tags)
│
└── reference/
    └── TmplReferenceContributor.kt # Navigation for {% include "file" %}
```

## Common Commands

```bash
# Build plugin
./gradlew build

# Run in sandbox IDE (IntelliJ IDEA)
./gradlew runIde

# Run in other IDEs
./gradlew runIde -PplatformType=WS   # WebStorm
./gradlew runIde -PplatformType=PS   # PhpStorm
./gradlew runIde -PplatformType=PC   # PyCharm

# Build distribution ZIP
./gradlew buildPlugin
# Output: build/distributions/tmpltool-intellij-plugin-*.zip

# Clean build
./gradlew clean build
```

## Plugin Features

### Phase 1: Basic File Type Support ✅
- `.tmpltool` file type with custom icon
- Base language detection from double extensions
- Syntax highlighting for `{{ }}`, `{% %}`, `{# #}`

### Phase 2: Language Injection ✅
- Layered highlighting (YAML/JSON + template syntax)
- `TmplOuterElementType` for base language regions
- `TmplFileViewProvider` for dual PSI trees

### Phase 3: Code Completion ✅
- 50+ function completions with icons by category
- Documentation popups on hover
- Auto-insert parentheses after function selection
- Parameter info hints (Ctrl+P inside function calls)

### Phase 4: Advanced Features ✅
- 15+ live templates (`if`, `for`, `env`, `uuid`, etc.)
- Error highlighting for unclosed tags
- Ctrl+Click navigation for `{% include "file" %}`

## Key Files to Understand

| File | Purpose |
|------|---------|
| `plugin.xml` | Plugin registration, extension points |
| `TmplLexer.kt` | Tokenizes template syntax (hand-written, not JFlex) |
| `TmplFunctionRegistry.kt` | Dynamic function loading from `tmpltool --ide json` |
| `TmplSettings.kt` | Application settings for tmpltool binary path |
| `TmplToolRunner.kt` | Executes tmpltool CLI and parses JSON output |
| `TmplBaseLanguageProvider.kt` | Maps `.yaml.tmpltool` → YAML language |
| `TmplTemplateHighlighterProvider.kt` | Combines base + template highlighting |

## Template Syntax

```jinja2
{# Comment #}

{{ variable }}
{{ get_env(name="VAR", default="value") }}
{{ hash_sha256(value="secret") }}

{% if condition %}
  content
{% endif %}

{% for item in items %}
  {{ item }}
{% endfor %}

{% include "partial.tmpltool" %}
```

## Function Categories in Registry

- **Environment**: `get_env`
- **Hash**: `hash_md5`, `hash_sha256`, etc.
- **Filesystem**: `read_file`, `glob`, `file_exists`
- **Data Parsing**: `parse_json`, `parse_yaml`, `parse_toml`
- **Validation**: `is_email`, `is_url`, `is_ipv4`, `is_uuid`, etc.
- **DateTime**: `now`, `format_date`
- **Random**: `random_int`, `random_string`, `random_choice`
- **UUID**: `uuid_v4`, `uuid_v7`
- **Network**: `get_interfaces`, `resolve_dns`, `cidr_contains`
- **Encoding**: `base64_encode`, `hex_encode`, etc.

## Dynamic Function Loading

Functions are loaded dynamically from `tmpltool --ide json` when the tmpltool binary is available. This ensures the plugin always has up-to-date function definitions matching the installed tmpltool version.

### How it works:
1. On first access, `TmplFunctionRegistry` calls `tmpltool --ide json`
2. JSON output is parsed into `TmplFunction` objects with parameters, types, examples, and syntax info
3. If tmpltool is not found, fallback definitions are used (basic set of common functions)

### Configuration (Settings > Tools > Tmpltool):
- **Path**: Custom path to tmpltool binary (leave empty for auto-detection)
- **Auto-detection**: Searches PATH, common installation directories (`/usr/local/bin`, `~/.cargo/bin`, etc.)
- **Test button**: Verifies the binary and shows version

### Adding to fallback definitions

If tmpltool CLI is not available, the plugin uses built-in fallback definitions. To add to these:

```kotlin
// In TmplFunctionRegistry.kt fallbackFunctions list:
TmplFunction(
    name = "my_function",
    category = "Category",
    description = "What it does",
    params = listOf(
        FunctionParam("arg1", "string", description = "First argument"),
        FunctionParam("arg2", "int", required = false, default = "0")
    ),
    returnType = "string",
    examples = listOf("""{{ my_function(arg1="value") }}"""),
    isFunction = true,
    isFilter = false,
    isTest = false
)
```

Rebuild and test: `./gradlew runIde`

## Publishing

1. Build: `./gradlew buildPlugin`
2. Go to [JetBrains Marketplace](https://plugins.jetbrains.com/)
3. Upload `build/distributions/tmpltool-intellij-plugin-*.zip`

## Related Projects

- [tmpltool](https://github.com/bordeux/tmpltool) - The Rust CLI tool this plugin supports
- [MiniJinja](https://github.com/mitsuhiko/minijinja) - Template engine used by tmpltool

## Development Notes

- **Kotlin + Gradle** (Kotlin DSL)
- **IntelliJ Platform SDK 2024.1+**
- **JDK 17** required
- Uses `org.jetbrains.intellij.platform` Gradle plugin (v2.2.1)

## Debugging Tips

1. **Lexer issues**: Add print statements in `TmplLexer.kt`, check token stream
2. **Highlighting issues**: Check `TmplSyntaxHighlighter.getTokenHighlights()`
3. **Completion not working**: Verify `isInTemplateContext()` in completion provider
4. **Parameter info not showing**: Check `TmplParameterInfoHandler.findFunctionCall()` and function lookup in registry
5. **Base language not detected**: Check `TmplBaseLanguageProvider.getBaseLanguage()`

Run `./gradlew runIde` and use **Tools > Internal Actions > View PSI Structure** to debug PSI tree.


## Commits

**Commit message format:** This project uses [Conventional Commits](https://www.conventionalcommits.org/):
- `feat: description` - New feature (minor version bump)
- `fix: description` - Bug fix (patch version bump)
- `feat!: description` - Breaking change (major version bump)
- `docs:`, `refactor:`, `perf:` - Other changes (patch bump)
- `style:`, `test:`, `chore:`, `ci:` - No version bump

**Important:** Do not include Claude model references (e.g., "Co-Authored-By: Claude") in commit messages. Keep commits clean and professional.
