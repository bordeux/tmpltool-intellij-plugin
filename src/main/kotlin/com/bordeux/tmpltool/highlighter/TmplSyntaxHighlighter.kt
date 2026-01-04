package com.bordeux.tmpltool.highlighter

import com.bordeux.tmpltool.TmplOuterElementType
import com.bordeux.tmpltool.lexer.TmplLexer
import com.bordeux.tmpltool.lexer.TmplTokenTypes
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

class TmplSyntaxHighlighter : SyntaxHighlighterBase() {

    companion object {
        // Delimiters
        val DELIMITER = createTextAttributesKey("TMPL_DELIMITER", DefaultLanguageHighlighterColors.BRACES)

        // Template content
        val KEYWORD = createTextAttributesKey("TMPL_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
        val IDENTIFIER = createTextAttributesKey("TMPL_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)
        val FUNCTION_CALL = createTextAttributesKey("TMPL_FUNCTION_CALL", DefaultLanguageHighlighterColors.FUNCTION_CALL)
        val STRING = createTextAttributesKey("TMPL_STRING", DefaultLanguageHighlighterColors.STRING)
        val NUMBER = createTextAttributesKey("TMPL_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
        val OPERATOR = createTextAttributesKey("TMPL_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
        val PARENTHESES = createTextAttributesKey("TMPL_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES)
        val BRACKETS = createTextAttributesKey("TMPL_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)
        val COMMA = createTextAttributesKey("TMPL_COMMA", DefaultLanguageHighlighterColors.COMMA)
        val DOT = createTextAttributesKey("TMPL_DOT", DefaultLanguageHighlighterColors.DOT)

        // Comments
        val COMMENT = createTextAttributesKey("TMPL_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT)

        // Text outside templates
        val TEXT = createTextAttributesKey("TMPL_TEXT", HighlighterColors.TEXT)

        // Bad character
        val BAD_CHAR = createTextAttributesKey("TMPL_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)

        private val DELIMITER_KEYS = arrayOf(DELIMITER)
        private val KEYWORD_KEYS = arrayOf(KEYWORD)
        private val IDENTIFIER_KEYS = arrayOf(IDENTIFIER)
        private val STRING_KEYS = arrayOf(STRING)
        private val NUMBER_KEYS = arrayOf(NUMBER)
        private val OPERATOR_KEYS = arrayOf(OPERATOR)
        private val PARENTHESES_KEYS = arrayOf(PARENTHESES)
        private val BRACKETS_KEYS = arrayOf(BRACKETS)
        private val COMMA_KEYS = arrayOf(COMMA)
        private val DOT_KEYS = arrayOf(DOT)
        private val COMMENT_KEYS = arrayOf(COMMENT)
        private val TEXT_KEYS = arrayOf(TEXT)
        private val BAD_CHAR_KEYS = arrayOf(BAD_CHAR)
        private val EMPTY_KEYS = emptyArray<TextAttributesKey>()
    }

    override fun getHighlightingLexer(): Lexer = TmplLexer()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        return when (tokenType) {
            TmplTokenTypes.EXPR_START, TmplTokenTypes.EXPR_END,
            TmplTokenTypes.STMT_START, TmplTokenTypes.STMT_END,
            TmplTokenTypes.COMMENT_START, TmplTokenTypes.COMMENT_END -> DELIMITER_KEYS

            TmplTokenTypes.KEYWORD -> KEYWORD_KEYS
            TmplTokenTypes.IDENTIFIER -> IDENTIFIER_KEYS
            TmplTokenTypes.STRING -> STRING_KEYS
            TmplTokenTypes.NUMBER -> NUMBER_KEYS
            TmplTokenTypes.OPERATOR, TmplTokenTypes.PIPE, TmplTokenTypes.EQUALS, TmplTokenTypes.COLON -> OPERATOR_KEYS
            TmplTokenTypes.LPAREN, TmplTokenTypes.RPAREN -> PARENTHESES_KEYS
            TmplTokenTypes.LBRACKET, TmplTokenTypes.RBRACKET -> BRACKETS_KEYS
            TmplTokenTypes.COMMA -> COMMA_KEYS
            TmplTokenTypes.DOT -> DOT_KEYS
            TmplTokenTypes.COMMENT_CONTENT -> COMMENT_KEYS
            TmplTokenTypes.TEXT -> TEXT_KEYS
            TmplTokenTypes.BAD_CHARACTER -> BAD_CHAR_KEYS
            TmplTokenTypes.WHITE_SPACE -> EMPTY_KEYS
            // Outer content is handled by layered highlighter with base language
            TmplOuterElementType.INSTANCE -> EMPTY_KEYS
            else -> EMPTY_KEYS
        }
    }
}
