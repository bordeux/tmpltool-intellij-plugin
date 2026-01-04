package com.bordeux.tmpltool.completion

import com.bordeux.tmpltool.TmplLanguage
import com.bordeux.tmpltool.lexer.TmplTokenTypes
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext

class TmplCompletionContributor : CompletionContributor() {

    init {
        // Complete function names after {{ or inside expressions
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withLanguage(TmplLanguage.INSTANCE),
            TmplFunctionCompletionProvider()
        )
    }
}

class TmplFunctionCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val position = parameters.position
        val prevSibling = findPrevNonWhitespaceSibling(position)

        // Check if we're inside a template expression context
        if (!isInTemplateContext(position, prevSibling)) {
            return
        }

        // Check if we're after a dot (object access) - don't complete functions
        if (prevSibling?.text == ".") {
            return
        }

        // Check if we're inside a function call (inside parentheses)
        val document = parameters.editor.document
        val offset = parameters.offset
        val text = document.text
        val functionContext = findFunctionContext(text, offset)

        if (functionContext != null) {
            // We're inside a function call - show parameter name completions instead
            // Check if this is a filter call (after pipe) - skip first param if so
            val isFilterCall = isFilterCall(text, offset)
            addParameterNameCompletions(functionContext, result, skipFirstParam = isFilterCall)
            return
        }

        // Check if we're after a pipe (filter context)
        val isAfterPipe = isAfterPipe(text, offset)

        if (isAfterPipe) {
            // Show only filters
            addFilterCompletions(result)
            return
        }

        // Add all function completions
        for (func in TmplFunctionRegistry.functions) {
            val lookupElement = LookupElementBuilder.create(func.name)
                .withIcon(getCategoryIcon(func.category))
                .withTypeText(func.category, true)
                .withTailText(buildParamsTail(func), true)
                .withInsertHandler { ctx, _ ->
                    // Insert parentheses after function name
                    val editor = ctx.editor
                    val document = editor.document
                    val offset = ctx.tailOffset

                    if (func.params.isEmpty()) {
                        document.insertString(offset, "()")
                        editor.caretModel.moveToOffset(offset + 2)
                    } else {
                        document.insertString(offset, "()")
                        editor.caretModel.moveToOffset(offset + 1)
                    }
                }
                .withBoldness(true)

            result.addElement(PrioritizedLookupElement.withPriority(lookupElement, getPriority(func)))
        }

        // Add keyword completions
        for (keyword in TmplTokenTypes.KEYWORDS) {
            val lookupElement = LookupElementBuilder.create(keyword)
                .withIcon(AllIcons.Nodes.Favorite)
                .withTypeText("keyword", true)
                .withBoldness(false)

            result.addElement(PrioritizedLookupElement.withPriority(lookupElement, -10.0))
        }
    }

    private fun isInTemplateContext(position: PsiElement, prevSibling: PsiElement?): Boolean {
        // Walk up the tree and check if we're inside a template tag
        var current: PsiElement? = position
        while (current != null) {
            val text = current.text
            // Check if we're in {{ }}, {% %}, or after those delimiters
            if (text.contains("{{") || text.contains("{%")) {
                return true
            }
            current = current.parent
        }

        // Also check the previous sibling
        if (prevSibling != null) {
            val prevText = prevSibling.text
            if (prevText == "{{" || prevText == "{%") {
                return true
            }
        }

        // Check if we're directly after expression/statement start
        val elementType = position.node?.elementType
        return elementType == TmplTokenTypes.IDENTIFIER ||
                elementType == TmplTokenTypes.EXPR_START ||
                elementType == TmplTokenTypes.STMT_START
    }

    private fun findPrevNonWhitespaceSibling(element: PsiElement): PsiElement? {
        var sibling = element.prevSibling
        while (sibling != null && sibling.text.isBlank()) {
            sibling = sibling.prevSibling
        }
        return sibling
    }

    private fun buildParamsTail(func: TmplFunctionRegistry.TmplFunction): String {
        if (func.params.isEmpty()) {
            return "()"
        }
        val params = func.params.joinToString(", ") { param ->
            if (param.required) {
                "${param.name}=..."
            } else {
                "[${param.name}=...]"
            }
        }
        return "($params)"
    }

    private fun getCategoryIcon(category: String) = when (category) {
        "Environment" -> AllIcons.Nodes.Variable
        "Hash" -> AllIcons.Nodes.SecurityRole
        "Filesystem" -> AllIcons.Nodes.Folder
        "Data Parsing" -> AllIcons.FileTypes.Json
        "Validation" -> AllIcons.Actions.Checked
        "DateTime" -> AllIcons.Actions.RealIntentionBulb
        "Random" -> AllIcons.Actions.Refresh
        "UUID" -> AllIcons.Nodes.Type
        "Network" -> AllIcons.Nodes.Deploy
        "String" -> AllIcons.Nodes.Field
        "Encoding" -> AllIcons.Nodes.Artifact
        else -> AllIcons.Nodes.Function
    }

    private fun getPriority(func: TmplFunctionRegistry.TmplFunction): Double {
        // Prioritize commonly used functions
        return when (func.name) {
            "get_env" -> 100.0
            "now" -> 90.0
            "uuid_v4" -> 85.0
            "hash_sha256" -> 80.0
            "random_string" -> 75.0
            else -> when (func.category) {
                "Environment" -> 70.0
                "Hash" -> 60.0
                "Validation" -> 50.0
                "DateTime" -> 40.0
                else -> 30.0
            }
        }
    }

    /**
     * Check if cursor is after a pipe character (filter context).
     */
    private fun isAfterPipe(text: String, offset: Int): Boolean {
        var i = offset - 1

        // Skip any partial identifier being typed
        while (i >= 0 && (text[i].isLetterOrDigit() || text[i] == '_')) {
            i--
        }

        // Skip whitespace
        while (i >= 0 && text[i].isWhitespace()) {
            i--
        }

        // Check if we hit a pipe
        return i >= 0 && text[i] == '|'
    }

    /**
     * Check if we're inside a filter call (function call that follows a pipe).
     * E.g., {{ "test" | truncate( }} - we're in a filter call
     */
    private fun isFilterCall(text: String, offset: Int): Boolean {
        // Find the opening paren first
        var parenDepth = 0
        var i = offset - 1

        while (i >= 0) {
            when (text[i]) {
                ')' -> parenDepth++
                '(' -> {
                    if (parenDepth == 0) {
                        // Found opening paren, now check if there's a pipe before the function name
                        return hasPipeBeforeFunction(text, i)
                    }
                    parenDepth--
                }
                '{' -> {
                    if (i > 0 && (text[i - 1] == '{' || text[i - 1] == '%')) {
                        return false
                    }
                }
            }
            i--
        }
        return false
    }

    /**
     * Check if there's a pipe before the function name at the given paren position.
     */
    private fun hasPipeBeforeFunction(text: String, parenIndex: Int): Boolean {
        var i = parenIndex - 1

        // Skip function name
        while (i >= 0 && text[i].isWhitespace()) i--
        while (i >= 0 && (text[i].isLetterOrDigit() || text[i] == '_')) i--

        // Skip whitespace
        while (i >= 0 && text[i].isWhitespace()) i--

        // Check for pipe
        return i >= 0 && text[i] == '|'
    }

    /**
     * Add filter completions (only items where isFilter=true).
     */
    private fun addFilterCompletions(result: CompletionResultSet) {
        for (func in TmplFunctionRegistry.functions) {
            if (!func.isFilter) continue

            val lookupElement = LookupElementBuilder.create(func.name)
                .withIcon(AllIcons.Nodes.Function)
                .withTypeText("filter", true)
                .withTailText(if (func.params.isNotEmpty()) buildParamsTail(func) else "", true)
                .withInsertHandler { ctx, _ ->
                    // If filter has required params, insert parentheses
                    val hasRequiredParams = func.params.any { it.required }
                    if (hasRequiredParams) {
                        val editor = ctx.editor
                        val document = editor.document
                        val tailOffset = ctx.tailOffset
                        document.insertString(tailOffset, "()")
                        editor.caretModel.moveToOffset(tailOffset + 1)
                    }
                }
                .withBoldness(true)

            result.addElement(PrioritizedLookupElement.withPriority(lookupElement, 100.0))
        }
    }

    /**
     * Find if we're inside a function call and return the function name.
     * Returns null if not inside a function call.
     */
    private fun findFunctionContext(text: String, offset: Int): TmplFunctionRegistry.TmplFunction? {
        if (offset > text.length) return null

        // Walk backwards from cursor to find opening paren
        var parenDepth = 0
        var i = offset - 1

        while (i >= 0) {
            val c = text[i]
            when (c) {
                ')' -> parenDepth++
                '(' -> {
                    if (parenDepth == 0) {
                        // Found the opening paren, now find the function name
                        val functionName = extractFunctionName(text, i)
                        if (functionName != null) {
                            return TmplFunctionRegistry.getFunction(functionName)
                        }
                        return null
                    }
                    parenDepth--
                }
                // Stop if we hit template delimiters
                '{' -> {
                    if (i > 0 && (text[i - 1] == '{' || text[i - 1] == '%')) {
                        return null
                    }
                }
            }
            i--
        }
        return null
    }

    /**
     * Extract function name immediately before the opening parenthesis.
     */
    private fun extractFunctionName(text: String, parenIndex: Int): String? {
        var end = parenIndex - 1

        // Skip whitespace
        while (end >= 0 && text[end].isWhitespace()) {
            end--
        }

        if (end < 0) return null

        // Check if it's a valid identifier character
        if (!text[end].isLetterOrDigit() && text[end] != '_') {
            return null
        }

        // Find the start of the identifier
        var start = end
        while (start > 0 && (text[start - 1].isLetterOrDigit() || text[start - 1] == '_')) {
            start--
        }

        val name = text.substring(start, end + 1)
        // Verify it's a valid function name (starts with letter or underscore)
        return if (name.isNotEmpty() && (name[0].isLetter() || name[0] == '_')) {
            name
        } else {
            null
        }
    }

    /**
     * Add parameter name completions for the given function.
     * @param skipFirstParam If true, skip the first parameter (used for filters where first arg is the piped value)
     */
    private fun addParameterNameCompletions(func: TmplFunctionRegistry.TmplFunction, result: CompletionResultSet, skipFirstParam: Boolean = false) {
        val params = if (skipFirstParam && func.params.isNotEmpty()) func.params.drop(1) else func.params
        for (param in params) {
            val lookupElement = LookupElementBuilder.create("${param.name}=")
                .withIcon(AllIcons.Nodes.Parameter)
                .withTypeText(param.type, true)
                .withTailText(if (param.required) " (required)" else " (optional)", true)
                .withPresentableText(param.name)
                .withInsertHandler { ctx, _ ->
                    // If param type is string, add quotes
                    if (param.type == "string") {
                        val editor = ctx.editor
                        val document = editor.document
                        val offset = ctx.tailOffset
                        document.insertString(offset, "\"\"")
                        editor.caretModel.moveToOffset(offset + 1)
                    }
                }

            val priority = if (param.required) 100.0 else 50.0
            result.addElement(PrioritizedLookupElement.withPriority(lookupElement, priority))
        }
    }
}
