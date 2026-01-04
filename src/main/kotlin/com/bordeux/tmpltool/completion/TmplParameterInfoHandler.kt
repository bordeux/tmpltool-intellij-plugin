package com.bordeux.tmpltool.completion

import com.bordeux.tmpltool.TmplLanguage
import com.bordeux.tmpltool.completion.TmplFunctionRegistry.TmplFunction
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.parameterInfo.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class TmplParameterInfoHandler : ParameterInfoHandler<PsiElement, TmplFunction> {

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

        context.itemsToShow = arrayOf(function)
        return element
    }

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

    override fun updateUI(function: TmplFunction?, context: ParameterInfoUIContext) {
        if (function == null) {
            context.isUIComponentEnabled = false
            return
        }

        val params = function.params
        if (params.isEmpty()) {
            context.setupUIComponentPresentation(
                "${function.name}() - no parameters",
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
                        // Found the opening paren, now find the function name
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

    @Deprecated("Deprecated in Java")
    override fun getParametersForLookup(item: LookupElement, context: ParameterInfoContext): Array<Any>? {
        return null
    }
}
