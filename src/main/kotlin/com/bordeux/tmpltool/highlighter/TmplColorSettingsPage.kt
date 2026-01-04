package com.bordeux.tmpltool.highlighter

import com.bordeux.tmpltool.TmplIcons
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

class TmplColorSettingsPage : ColorSettingsPage {

    companion object {
        private val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Delimiters", TmplSyntaxHighlighter.DELIMITER),
            AttributesDescriptor("Keyword", TmplSyntaxHighlighter.KEYWORD),
            AttributesDescriptor("Identifier", TmplSyntaxHighlighter.IDENTIFIER),
            AttributesDescriptor("Function call", TmplSyntaxHighlighter.FUNCTION_CALL),
            AttributesDescriptor("String", TmplSyntaxHighlighter.STRING),
            AttributesDescriptor("Number", TmplSyntaxHighlighter.NUMBER),
            AttributesDescriptor("Operator", TmplSyntaxHighlighter.OPERATOR),
            AttributesDescriptor("Parentheses", TmplSyntaxHighlighter.PARENTHESES),
            AttributesDescriptor("Brackets", TmplSyntaxHighlighter.BRACKETS),
            AttributesDescriptor("Comma", TmplSyntaxHighlighter.COMMA),
            AttributesDescriptor("Dot", TmplSyntaxHighlighter.DOT),
            AttributesDescriptor("Comment", TmplSyntaxHighlighter.COMMENT),
            AttributesDescriptor("Text", TmplSyntaxHighlighter.TEXT),
            AttributesDescriptor("Bad character", TmplSyntaxHighlighter.BAD_CHAR),
        )
    }

    override fun getIcon(): Icon = TmplIcons.FILE

    override fun getHighlighter(): SyntaxHighlighter = TmplSyntaxHighlighter()

    override fun getDemoText(): String = """
        {# This is a comment #}

        version: "3.8"
        services:
          web:
            image: nginx:{{ get_env(name="NGINX_VERSION", default="latest") }}
            ports:
              - "{{ get_env(name="PORT", default="80") }}:80"
            environment:
              - API_KEY={{ hash_sha256(value="secret") }}

        {% if is_production %}
        production:
          replicas: 3
        {% else %}
        development:
          debug: true
        {% endif %}

        {% for item in items %}
          - {{ item.name }}: {{ item.value | upper }}
        {% endfor %}
    """.trimIndent()

    override fun getAdditionalHighlightingTagToDescriptorMap(): MutableMap<String, TextAttributesKey>? = null

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): String = "Tmpltool Template"
}
