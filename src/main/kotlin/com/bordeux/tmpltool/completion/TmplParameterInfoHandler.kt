package com.bordeux.tmpltool.completion

import com.bordeux.tmpltool.TmplLanguage
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.parameterInfo.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class TmplParameterInfoHandler : ParameterInfoHandler<PsiElement, TmplParameterInfoHandler.FunctionWithContext> {

    companion object {
        private val LOG = Logger.getInstance(TmplParameterInfoHandler::class.java)
    }

    override fun findElementForParameterInfo(context: CreateParameterInfoContext): PsiElement? {
        val file = context.file
        val offset = context.offset

        LOG.info("TmplParameterInfoHandler.findElementForParameterInfo called: offset=$offset, file=${file.name}, language=${file.language}")

        // Get the Tmpltool PSI tree
        val viewProvider = file.viewProvider
        val tmplFile = viewProvider.getPsi(TmplLanguage.INSTANCE)

        if (tmplFile == null) {
            LOG.info("  tmplFile is null, trying original file")
        }

        val targetFile = tmplFile ?: file
        val element = targetFile.findElementAt(offset)

        if (element == null) {
            LOG.info("  element at offset is null")
            return null
        }

        LOG.info("  element: ${element.text}, type: ${element.node?.elementType}")

        // Use document text for more reliable parsing
        val document = context.editor.document
        val text = document.text

        // Find function name by analyzing text before cursor
        val functionName = findFunctionNameFromText(text, offset)
        LOG.info("  functionName: $functionName")

        if (functionName == null) return null

        val function = TmplFunctionRegistry.getFunction(functionName)
        LOG.info("  function found: ${function != null}")

        if (function == null) return null

        // Check if this is a filter call - if so, we'll skip the first parameter
        val isFilterCall = isFilterCall(text, offset)
        context.itemsToShow = arrayOf(FunctionWithContext(function, isFilterCall))
        return element
    }

    /**
     * Wrapper to pass filter context to updateUI
     */
    data class FunctionWithContext(
        val function: TmplFunctionRegistry.TmplFunction,
        val isFilterCall: Boolean
    )

    override fun findElementForUpdatingParameterInfo(context: UpdateParameterInfoContext): PsiElement? {
        val file = context.file
        val viewProvider = file.viewProvider
        val tmplFile = viewProvider.getPsi(TmplLanguage.INSTANCE) ?: return null
        return tmplFile.findElementAt(context.offset)
    }

    override fun showParameterInfo(element: PsiElement, context: CreateParameterInfoContext) {
        context.showHint(element, element.textRange.startOffset, this)
    }

    override fun updateParameterInfo(parameterOwner: PsiElement, context: UpdateParameterInfoContext) {
        val file = context.file
        val viewProvider = file.viewProvider
        val tmplFile = viewProvider.getPsi(TmplLanguage.INSTANCE) ?: return

        val currentParamIndex = findCurrentParameterIndex(tmplFile.text, context.offset)
        context.setCurrentParameter(currentParamIndex)
    }

    override fun updateUI(functionWithContext: FunctionWithContext?, context: ParameterInfoUIContext) {
        if (functionWithContext == null) {
            context.isUIComponentEnabled = false
            return
        }

        val function = functionWithContext.function
        // For filters, skip the first parameter (it's the piped value)
        val params = if (functionWithContext.isFilterCall && function.params.isNotEmpty()) {
            function.params.drop(1)
        } else {
            function.params
        }

        if (params.isEmpty()) {
            val suffix = if (functionWithContext.isFilterCall) " (value passed via pipe)" else " - no parameters"
            context.setupUIComponentPresentation(
                "${function.name}()$suffix",
                -1, 0, false, false, false,
                context.defaultParameterColor
            )
            return
        }

        val currentIndex = context.currentParameterIndex
        val sb = StringBuilder()
        var highlightStart = -1
        var highlightEnd = -1

        params.forEachIndexed { index, param ->
            if (index > 0) sb.append(", ")

            val paramStart = sb.length

            // Format: name: type or name: type = default
            sb.append(param.name).append(": ").append(param.type)
            if (!param.required && param.default != null) {
                sb.append(" = ").append(param.default)
            }
            if (!param.required) {
                sb.append("?")
            }

            val paramEnd = sb.length

            if (index == currentIndex) {
                highlightStart = paramStart
                highlightEnd = paramEnd
            }
        }

        context.setupUIComponentPresentation(
            sb.toString(),
            highlightStart, highlightEnd,
            false, false, false,
            context.defaultParameterColor
        )
    }

    /**
     * Find function name by analyzing text before the cursor.
     * Looks for pattern: identifier( ... cursor
     * Also handles filter pattern: value | filter( ... cursor
     */
    private fun findFunctionNameFromText(text: String, offset: Int): String? {
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
                        // Found the opening paren, now find the function/filter name
                        return extractFunctionName(text, i)
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
     * Count which parameter we're currently on (0-indexed).
     */
    private fun findCurrentParameterIndex(text: String, offset: Int): Int {
        var parenDepth = 0
        var commaCount = 0
        var i = offset - 1

        while (i >= 0) {
            when (text[i]) {
                ')' -> parenDepth++
                '(' -> {
                    if (parenDepth == 0) {
                        return commaCount
                    }
                    parenDepth--
                }
                ',' -> {
                    if (parenDepth == 0) {
                        commaCount++
                    }
                }
            }
            i--
        }
        return commaCount
    }

    /**
     * Check if we're inside a filter call (function call that follows a pipe).
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

    @Deprecated("Deprecated in Java")
    override fun getParametersForLookup(item: LookupElement, context: ParameterInfoContext): Array<Any>? {
        return null
    }
}
