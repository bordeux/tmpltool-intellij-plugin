package com.bordeux.tmpltool.completion

import com.bordeux.tmpltool.TmplLanguage
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.bordeux.tmpltool.lexer.TmplTokenTypes

class TmplDocumentationProvider : AbstractDocumentationProvider() {

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val target = element ?: originalElement ?: return null

        // Only handle our language
        if (target.language != TmplLanguage.INSTANCE) {
            return null
        }

        // Check if this is an identifier (potential function name)
        if (target.elementType != TmplTokenTypes.IDENTIFIER) {
            return null
        }

        val functionName = target.text
        val func = TmplFunctionRegistry.getFunction(functionName) ?: return null

        return buildDocumentation(func)
    }

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        val target = element ?: originalElement ?: return null

        if (target.language != TmplLanguage.INSTANCE) {
            return null
        }

        if (target.elementType != TmplTokenTypes.IDENTIFIER) {
            return null
        }

        val functionName = target.text
        val func = TmplFunctionRegistry.getFunction(functionName) ?: return null

        return buildQuickInfo(func)
    }

    private fun buildDocumentation(func: TmplFunctionRegistry.TmplFunction): String = buildString {
        append(DocumentationMarkup.DEFINITION_START)
        append("<b>${func.name}</b>")
        append(buildSignature(func))
        append(DocumentationMarkup.DEFINITION_END)

        append(DocumentationMarkup.CONTENT_START)
        append(func.description)
        append(DocumentationMarkup.CONTENT_END)

        if (func.params.isNotEmpty()) {
            append(DocumentationMarkup.SECTIONS_START)
            append(DocumentationMarkup.SECTION_HEADER_START)
            append("Parameters:")
            append(DocumentationMarkup.SECTION_SEPARATOR)
            append("<p>")
            for (param in func.params) {
                append("<code>${param.name}</code>: ${param.type}")
                if (!param.required) {
                    append(" <i>(optional")
                    if (param.default != null) {
                        append(", default: ${param.default}")
                    }
                    append(")</i>")
                }
                if (param.description.isNotEmpty()) {
                    append(" — ${param.description}")
                }
                append("<br/>")
            }
            append("</p>")
            append(DocumentationMarkup.SECTION_END)
            append(DocumentationMarkup.SECTIONS_END)
        }

        append(DocumentationMarkup.SECTIONS_START)
        append(DocumentationMarkup.SECTION_HEADER_START)
        append("Returns:")
        append(DocumentationMarkup.SECTION_SEPARATOR)
        append("<code>${func.returnType}</code>")
        append(DocumentationMarkup.SECTION_END)

        if (func.example.isNotEmpty()) {
            append(DocumentationMarkup.SECTION_HEADER_START)
            append("Example:")
            append(DocumentationMarkup.SECTION_SEPARATOR)
            append("<pre><code>${escapeHtml(func.example)}</code></pre>")
            append(DocumentationMarkup.SECTION_END)
        }

        append(DocumentationMarkup.SECTION_HEADER_START)
        append("Category:")
        append(DocumentationMarkup.SECTION_SEPARATOR)
        append(func.category)
        append(DocumentationMarkup.SECTION_END)
        append(DocumentationMarkup.SECTIONS_END)
    }

    private fun buildSignature(func: TmplFunctionRegistry.TmplFunction): String = buildString {
        append("(")
        append(func.params.joinToString(", ") { param ->
            val optional = if (param.required) "" else "?"
            "${param.name}$optional: ${param.type}"
        })
        append(")")
        append(" → ${func.returnType}")
    }

    private fun buildQuickInfo(func: TmplFunctionRegistry.TmplFunction): String = buildString {
        append("<b>${func.name}</b>")
        append(buildSignature(func))
        append("<br/>")
        append("<i>${func.category}</i>: ${func.description}")
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
