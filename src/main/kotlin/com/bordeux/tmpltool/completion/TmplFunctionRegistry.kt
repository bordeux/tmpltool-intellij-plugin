package com.bordeux.tmpltool.completion

import com.bordeux.tmpltool.settings.TmplToolRunner
import com.intellij.openapi.diagnostic.Logger

/**
 * Registry of all tmpltool built-in functions with their documentation.
 * Functions are loaded dynamically from `tmpltool --ide json` when available,
 * with a fallback to built-in definitions.
 */
object TmplFunctionRegistry {

    private val LOG = Logger.getInstance(TmplFunctionRegistry::class.java)

    data class FunctionParam(
        val name: String,
        val type: String,
        val required: Boolean = true,
        val default: String? = null,
        val description: String = ""
    )

    data class TmplFunction(
        val name: String,
        val category: String,
        val description: String,
        val params: List<FunctionParam> = emptyList(),
        val returnType: String = "string",
        val examples: List<String> = emptyList(),
        val isFunction: Boolean = true,
        val isFilter: Boolean = false,
        val isTest: Boolean = false
    ) {
        // Convenience property for backward compatibility
        val example: String get() = examples.firstOrNull() ?: ""
    }

    @Volatile
    private var _functions: List<TmplFunction> = emptyList()

    @Volatile
    private var _functionsByName: Map<String, TmplFunction> = emptyMap()

    @Volatile
    private var _functionsByCategory: Map<String, List<TmplFunction>> = emptyMap()

    @Volatile
    private var initialized = false

    val functions: List<TmplFunction>
        get() {
            ensureInitialized()
            return _functions
        }

    val functionsByName: Map<String, TmplFunction>
        get() {
            ensureInitialized()
            return _functionsByName
        }

    val functionsByCategory: Map<String, List<TmplFunction>>
        get() {
            ensureInitialized()
            return _functionsByCategory
        }

    private fun ensureInitialized() {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    loadFunctions()
                    initialized = true
                }
            }
        }
    }

    /**
     * Reload function definitions from tmpltool CLI.
     * Call this when settings change.
     */
    fun reload() {
        synchronized(this) {
            loadFunctions()
            initialized = true
        }
    }

    private fun loadFunctions() {
        val result = TmplToolRunner.fetchFunctionData()

        val loadedFunctions = when (result) {
            is TmplToolRunner.RunResult.Success -> {
                LOG.info("Successfully loaded ${result.functions.size} functions from tmpltool")
                result.functions.map { raw ->
                    TmplFunction(
                        name = raw.name,
                        category = raw.category.replaceFirstChar { it.uppercase() },
                        description = raw.description,
                        params = raw.arguments.map { arg ->
                            FunctionParam(
                                name = arg.name,
                                type = arg.argType,
                                required = arg.required,
                                default = arg.default,
                                description = arg.description
                            )
                        },
                        returnType = raw.returnType,
                        examples = raw.examples,
                        isFunction = raw.syntax.function,
                        isFilter = raw.syntax.filter,
                        isTest = raw.syntax.isTest
                    )
                }
            }
            is TmplToolRunner.RunResult.Error -> {
                LOG.warn("Failed to load functions from tmpltool: ${result.message}. Using fallback definitions.")
                fallbackFunctions
            }
        }

        _functions = loadedFunctions
        _functionsByName = loadedFunctions.associateBy { it.name }
        _functionsByCategory = loadedFunctions.groupBy { it.category }
    }

    fun getFunction(name: String): TmplFunction? {
        ensureInitialized()
        return _functionsByName[name]
    }

    fun generateDocumentation(func: TmplFunction): String = buildString {
        append("<b>${func.name}</b>")

        // Show syntax types
        val syntaxTypes = mutableListOf<String>()
        if (func.isFunction) syntaxTypes.add("function")
        if (func.isFilter) syntaxTypes.add("filter")
        if (func.isTest) syntaxTypes.add("test")
        if (syntaxTypes.isNotEmpty()) {
            append(" <i>(${syntaxTypes.joinToString(", ")})</i>")
        }

        append("<br/><br/>")
        append(func.description)
        append("<br/><br/>")

        if (func.params.isNotEmpty()) {
            append("<b>Parameters:</b><br/>")
            for (param in func.params) {
                append("• <code>${param.name}</code>: ${param.type}")
                if (!param.required) {
                    append(" (optional")
                    if (param.default != null) {
                        append(", default: ${param.default}")
                    }
                    append(")")
                }
                if (param.description.isNotEmpty()) {
                    append(" - ${param.description}")
                }
                append("<br/>")
            }
            append("<br/>")
        }

        append("<b>Returns:</b> ${func.returnType}<br/><br/>")

        if (func.examples.isNotEmpty()) {
            append("<b>Examples:</b><br/>")
            for (example in func.examples) {
                append("<code>${example}</code><br/>")
            }
        }
    }

    /**
     * Fallback function definitions when tmpltool CLI is not available.
     * This provides basic completions even without the CLI installed.
     */
    private val fallbackFunctions: List<TmplFunction> = listOf(
        // ==================== Environment ====================
        TmplFunction(
            name = "get_env",
            category = "Environment",
            description = "Gets an environment variable value",
            params = listOf(
                FunctionParam("name", "string", description = "Name of the environment variable"),
                FunctionParam("default", "string", required = false, description = "Default value if not set")
            ),
            examples = listOf("""{{ get_env(name="HOME", default="/root") }}"""),
            isFunction = true,
            isFilter = false
        ),

        // ==================== Hash Functions ====================
        TmplFunction(
            name = "md5",
            category = "Hash",
            description = "Computes MD5 hash of a value",
            params = listOf(FunctionParam("string", "string", description = "Value to hash")),
            examples = listOf("""{{ md5(string="hello") }}""", """{{ "hello" | md5 }}"""),
            isFunction = true,
            isFilter = true
        ),
        TmplFunction(
            name = "sha1",
            category = "Hash",
            description = "Computes SHA-1 hash of a value",
            params = listOf(FunctionParam("string", "string", description = "Value to hash")),
            examples = listOf("""{{ sha1(string="hello") }}""", """{{ "hello" | sha1 }}"""),
            isFunction = true,
            isFilter = true
        ),
        TmplFunction(
            name = "sha256",
            category = "Hash",
            description = "Computes SHA-256 hash of a value",
            params = listOf(FunctionParam("string", "string", description = "Value to hash")),
            examples = listOf("""{{ sha256(string="hello") }}""", """{{ "hello" | sha256 }}"""),
            isFunction = true,
            isFilter = true
        ),
        TmplFunction(
            name = "sha512",
            category = "Hash",
            description = "Computes SHA-512 hash of a value",
            params = listOf(FunctionParam("string", "string", description = "Value to hash")),
            examples = listOf("""{{ sha512(string="hello") }}""", """{{ "hello" | sha512 }}"""),
            isFunction = true,
            isFilter = true
        ),

        // ==================== Encoding ====================
        TmplFunction(
            name = "base64_encode",
            category = "Encoding",
            description = "Encodes a string to Base64",
            params = listOf(FunctionParam("string", "string", description = "String to encode")),
            examples = listOf("""{{ base64_encode(string="hello") }}""", """{{ "hello" | base64_encode }}"""),
            isFunction = true,
            isFilter = true
        ),
        TmplFunction(
            name = "base64_decode",
            category = "Encoding",
            description = "Decodes a Base64 string",
            params = listOf(FunctionParam("string", "string", description = "Base64 string to decode")),
            examples = listOf("""{{ base64_decode(string="aGVsbG8=") }}""", """{{ "aGVsbG8=" | base64_decode }}"""),
            isFunction = true,
            isFilter = true
        ),
        TmplFunction(
            name = "hex_encode",
            category = "Encoding",
            description = "Encodes a string to hexadecimal",
            params = listOf(FunctionParam("string", "string", description = "String to encode")),
            examples = listOf("""{{ hex_encode(string="hello") }}""", """{{ "hello" | hex_encode }}"""),
            isFunction = true,
            isFilter = true
        ),
        TmplFunction(
            name = "hex_decode",
            category = "Encoding",
            description = "Decodes a hexadecimal string",
            params = listOf(FunctionParam("string", "string", description = "Hex string to decode")),
            examples = listOf("""{{ hex_decode(string="68656c6c6f") }}""", """{{ "68656c6c6f" | hex_decode }}"""),
            isFunction = true,
            isFilter = true
        ),

        // ==================== Serialization ====================
        TmplFunction(
            name = "parse_json",
            category = "Serialization",
            description = "Parses a JSON string into an object",
            params = listOf(FunctionParam("string", "string", description = "JSON string to parse")),
            returnType = "any",
            examples = listOf("""{{ parse_json(string='{"key": "value"}') }}"""),
            isFunction = true,
            isFilter = true
        ),
        TmplFunction(
            name = "parse_yaml",
            category = "Serialization",
            description = "Parses a YAML string into an object",
            params = listOf(FunctionParam("string", "string", description = "YAML string to parse")),
            returnType = "any",
            examples = listOf("""{{ parse_yaml(string="key: value") }}"""),
            isFunction = true,
            isFilter = true
        ),
        TmplFunction(
            name = "to_json",
            category = "Serialization",
            description = "Convert object to JSON string",
            params = listOf(
                FunctionParam("object", "any", description = "The value to serialize"),
                FunctionParam("pretty", "boolean", required = false, default = "false", description = "Pretty-print the JSON output")
            ),
            examples = listOf("""{{ to_json(object=config) }}""", """{{ config | to_json(pretty=true) }}"""),
            isFunction = true,
            isFilter = true
        ),
        TmplFunction(
            name = "to_yaml",
            category = "Serialization",
            description = "Convert object to YAML string",
            params = listOf(FunctionParam("object", "any", description = "The value to serialize")),
            examples = listOf("""{{ to_yaml(object=config) }}""", """{{ config | to_yaml }}"""),
            isFunction = true,
            isFilter = true
        ),

        // ==================== Filesystem ====================
        TmplFunction(
            name = "read_file",
            category = "Filesystem",
            description = "Reads content from a file",
            params = listOf(FunctionParam("path", "string", description = "Path to the file")),
            examples = listOf("""{{ read_file(path="config.txt") }}"""),
            isFunction = true
        ),
        TmplFunction(
            name = "file_exists",
            category = "Filesystem",
            description = "Checks if a file exists",
            params = listOf(FunctionParam("path", "string", description = "Path to check")),
            returnType = "boolean",
            examples = listOf("""{% if file_exists(path="config.txt") %}...{% endif %}"""),
            isFunction = true
        ),
        TmplFunction(
            name = "glob",
            category = "Filesystem",
            description = "Finds files matching a glob pattern",
            params = listOf(FunctionParam("pattern", "string", description = "Glob pattern (e.g., '*.txt')")),
            returnType = "array",
            examples = listOf("""{% for file in glob(pattern="*.yaml") %}{{ file }}{% endfor %}"""),
            isFunction = true
        ),

        // ==================== Validation ====================
        TmplFunction(
            name = "is_email",
            category = "Validation",
            description = "Checks if a value is a valid email address",
            params = listOf(FunctionParam("value", "string", description = "Value to validate")),
            returnType = "boolean",
            examples = listOf("""{% if "test@example.com" is is_email %}valid{% endif %}"""),
            isFunction = true,
            isTest = true
        ),
        TmplFunction(
            name = "is_url",
            category = "Validation",
            description = "Checks if a value is a valid URL",
            params = listOf(FunctionParam("value", "string", description = "Value to validate")),
            returnType = "boolean",
            examples = listOf("""{% if "https://example.com" is is_url %}valid{% endif %}"""),
            isFunction = true,
            isTest = true
        ),
        TmplFunction(
            name = "is_uuid",
            category = "Validation",
            description = "Checks if a value is a valid UUID",
            params = listOf(FunctionParam("value", "string", description = "Value to validate")),
            returnType = "boolean",
            examples = listOf("""{% if value is is_uuid %}valid{% endif %}"""),
            isFunction = true,
            isTest = true
        ),

        // ==================== DateTime ====================
        TmplFunction(
            name = "now",
            category = "Datetime",
            description = "Returns the current date and time",
            params = listOf(
                FunctionParam("format", "string", required = false, default = "%Y-%m-%d %H:%M:%S", description = "Date format string"),
                FunctionParam("utc", "boolean", required = false, default = "false", description = "Use UTC timezone")
            ),
            examples = listOf("""{{ now(format="%Y-%m-%d") }}"""),
            isFunction = true
        ),

        // ==================== Random ====================
        TmplFunction(
            name = "random_int",
            category = "Random",
            description = "Generates a random integer",
            params = listOf(
                FunctionParam("min", "integer", required = false, default = "0", description = "Minimum value"),
                FunctionParam("max", "integer", required = false, default = "100", description = "Maximum value")
            ),
            returnType = "integer",
            examples = listOf("""{{ random_int(min=1, max=100) }}"""),
            isFunction = true
        ),
        TmplFunction(
            name = "random_string",
            category = "Random",
            description = "Generates a random alphanumeric string",
            params = listOf(
                FunctionParam("length", "integer", required = false, default = "16", description = "Length of the string")
            ),
            examples = listOf("""{{ random_string(length=32) }}"""),
            isFunction = true
        ),

        // ==================== UUID ====================
        TmplFunction(
            name = "uuid_v4",
            category = "Uuid",
            description = "Generates a random UUID v4",
            params = emptyList(),
            examples = listOf("""{{ uuid_v4() }}"""),
            isFunction = true
        ),
        TmplFunction(
            name = "uuid_v7",
            category = "Uuid",
            description = "Generates a time-ordered UUID v7",
            params = emptyList(),
            examples = listOf("""{{ uuid_v7() }}"""),
            isFunction = true
        ),

        // ==================== String ====================
        TmplFunction(
            name = "slugify",
            category = "String",
            description = "Converts a string to a URL-friendly slug",
            params = listOf(FunctionParam("string", "string", description = "String to slugify")),
            examples = listOf("""{{ slugify(string="Hello World!") }}""", """{{ "Hello World!" | slugify }}"""),
            isFunction = true,
            isFilter = true
        )
    )
}
