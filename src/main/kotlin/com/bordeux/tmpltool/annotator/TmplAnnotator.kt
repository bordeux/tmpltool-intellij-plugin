package com.bordeux.tmpltool.annotator

import com.bordeux.tmpltool.TmplLanguage
import com.bordeux.tmpltool.completion.TmplFunctionRegistry
import com.bordeux.tmpltool.lexer.TmplTokenTypes
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType

class TmplAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element.language != TmplLanguage.INSTANCE) {
            return
        }

        // Highlight function calls
        if (element.elementType == TmplTokenTypes.IDENTIFIER) {
            annotateFunctionCall(element, holder)
        }

        // Check for unclosed tags in the file
        if (element.elementType == TmplTokenTypes.EXPR_START ||
            element.elementType == TmplTokenTypes.STMT_START ||
            element.elementType == TmplTokenTypes.COMMENT_START) {
            checkUnclosedTag(element, holder)
        }
    }

    private fun annotateFunctionCall(element: PsiElement, holder: AnnotationHolder) {
        val text = element.text
        val func = TmplFunctionRegistry.getFunction(text)

        if (func != null) {
            // Check if followed by parenthesis (function call)
            val nextSibling = findNextNonWhitespaceSibling(element)
            if (nextSibling?.text == "(") {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(element.textRange)
                    .textAttributes(DefaultLanguageHighlighterColors.FUNCTION_CALL)
                    .create()
            }
        }
    }

    private fun checkUnclosedTag(element: PsiElement, holder: AnnotationHolder) {
        val text = element.text
        val file = element.containingFile ?: return
        val fileText = file.text
        val startOffset = element.textOffset

        val (startTag, endTag) = when (text) {
            "{{" -> "{{" to "}}"
            "{%" -> "{%" to "%}"
            "{#" -> "{#" to "#}"
            else -> return
        }

        // Find the matching closing tag
        var depth = 1
        var pos = startOffset + startTag.length

        while (pos < fileText.length - 1 && depth > 0) {
            val twoChars = fileText.substring(pos, minOf(pos + 2, fileText.length))

            when (twoChars) {
                startTag -> depth++
                endTag -> depth--
            }

            if (depth == 0) break
            pos++
        }

        if (depth > 0) {
            // Unclosed tag
            holder.newAnnotation(HighlightSeverity.ERROR, "Unclosed template tag: $startTag")
                .range(element.textRange)
                .create()
        }
    }

    private fun findNextNonWhitespaceSibling(element: PsiElement): PsiElement? {
        var sibling = element.nextSibling
        while (sibling != null && sibling.text.isBlank()) {
            sibling = sibling.nextSibling
        }
        return sibling
    }

    companion object {
        // Block statement keywords that need closing tags
        private val BLOCK_KEYWORDS = mapOf(
            "if" to "endif",
            "for" to "endfor",
            "block" to "endblock",
            "macro" to "endmacro",
            "call" to "endcall",
            "filter" to "endfilter",
            "set" to "endset",
            "raw" to "endraw",
            "with" to "endwith",
            "autoescape" to "endautoescape"
        )
    }
}
