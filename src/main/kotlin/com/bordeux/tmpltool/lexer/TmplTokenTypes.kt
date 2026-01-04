package com.bordeux.tmpltool.lexer

import com.bordeux.tmpltool.TmplLanguage
import com.intellij.psi.tree.IElementType

class TmplTokenType(debugName: String) : IElementType(debugName, TmplLanguage.INSTANCE)

object TmplTokenTypes {
    // Template delimiters
    @JvmField val EXPR_START = TmplTokenType("EXPR_START")           // {{
    @JvmField val EXPR_END = TmplTokenType("EXPR_END")               // }}
    @JvmField val STMT_START = TmplTokenType("STMT_START")           // {%
    @JvmField val STMT_END = TmplTokenType("STMT_END")               // %}
    @JvmField val COMMENT_START = TmplTokenType("COMMENT_START")     // {#
    @JvmField val COMMENT_END = TmplTokenType("COMMENT_END")         // #}

    // Inside template expressions
    @JvmField val IDENTIFIER = TmplTokenType("IDENTIFIER")
    @JvmField val STRING = TmplTokenType("STRING")
    @JvmField val NUMBER = TmplTokenType("NUMBER")
    @JvmField val OPERATOR = TmplTokenType("OPERATOR")
    @JvmField val LPAREN = TmplTokenType("LPAREN")
    @JvmField val RPAREN = TmplTokenType("RPAREN")
    @JvmField val LBRACKET = TmplTokenType("LBRACKET")
    @JvmField val RBRACKET = TmplTokenType("RBRACKET")
    @JvmField val COMMA = TmplTokenType("COMMA")
    @JvmField val DOT = TmplTokenType("DOT")
    @JvmField val PIPE = TmplTokenType("PIPE")
    @JvmField val EQUALS = TmplTokenType("EQUALS")
    @JvmField val COLON = TmplTokenType("COLON")

    // Keywords
    @JvmField val KEYWORD = TmplTokenType("KEYWORD")

    // Content
    @JvmField val TEMPLATE_CONTENT = TmplTokenType("TEMPLATE_CONTENT")
    @JvmField val COMMENT_CONTENT = TmplTokenType("COMMENT_CONTENT")
    @JvmField val TEXT = TmplTokenType("TEXT")
    @JvmField val WHITE_SPACE = TmplTokenType("WHITE_SPACE")

    // Bad character
    @JvmField val BAD_CHARACTER = TmplTokenType("BAD_CHARACTER")

    // Keywords list
    val KEYWORDS = setOf(
        "if", "else", "elif", "endif",
        "for", "endfor", "in",
        "block", "endblock",
        "extends", "include",
        "macro", "endmacro", "call", "endcall",
        "filter", "endfilter",
        "set", "endset",
        "raw", "endraw",
        "with", "endwith",
        "autoescape", "endautoescape",
        "true", "false", "none",
        "and", "or", "not", "is"
    )
}
