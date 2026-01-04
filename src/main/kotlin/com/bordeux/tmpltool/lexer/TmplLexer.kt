package com.bordeux.tmpltool.lexer

import com.bordeux.tmpltool.TmplOuterElementType
import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

class TmplLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var bufferEnd: Int = 0
    private var tokenStart: Int = 0
    private var tokenEnd: Int = 0
    private var tokenType: IElementType? = null
    private var state: Int = STATE_TEXT

    companion object {
        const val STATE_TEXT = 0
        const val STATE_EXPR = 1
        const val STATE_STMT = 2
        const val STATE_COMMENT = 3
    }

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.bufferEnd = endOffset
        this.tokenStart = startOffset
        this.tokenEnd = startOffset
        this.state = initialState
        advance()
    }

    override fun getState(): Int = state

    override fun getTokenType(): IElementType? = tokenType

    override fun getTokenStart(): Int = tokenStart

    override fun getTokenEnd(): Int = tokenEnd

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = bufferEnd

    override fun advance() {
        tokenStart = tokenEnd
        if (tokenStart >= bufferEnd) {
            tokenType = null
            return
        }

        when (state) {
            STATE_TEXT -> lexText()
            STATE_EXPR -> lexExpr()
            STATE_STMT -> lexStmt()
            STATE_COMMENT -> lexComment()
        }
    }

    private fun lexText() {
        // Look for template tags
        val pos = tokenStart

        if (pos + 1 < bufferEnd) {
            val c1 = buffer[pos]
            val c2 = buffer[pos + 1]

            when {
                c1 == '{' && c2 == '{' -> {
                    tokenEnd = pos + 2
                    tokenType = TmplTokenTypes.EXPR_START
                    state = STATE_EXPR
                    return
                }
                c1 == '{' && c2 == '%' -> {
                    tokenEnd = pos + 2
                    tokenType = TmplTokenTypes.STMT_START
                    state = STATE_STMT
                    return
                }
                c1 == '{' && c2 == '#' -> {
                    tokenEnd = pos + 2
                    tokenType = TmplTokenTypes.COMMENT_START
                    state = STATE_COMMENT
                    return
                }
            }
        }

        // Regular text - scan until we find a template tag
        var end = pos
        while (end < bufferEnd) {
            if (end + 1 < bufferEnd) {
                val c1 = buffer[end]
                val c2 = buffer[end + 1]
                if ((c1 == '{' && c2 == '{') || (c1 == '{' && c2 == '%') || (c1 == '{' && c2 == '#')) {
                    break
                }
            }
            end++
        }

        tokenEnd = end
        // Use TmplOuterElementType for base language highlighting
        tokenType = TmplOuterElementType.INSTANCE
    }

    private fun lexExpr() {
        lexTemplateContent("}}", TmplTokenTypes.EXPR_END, STATE_TEXT)
    }

    private fun lexStmt() {
        lexTemplateContent("%}", TmplTokenTypes.STMT_END, STATE_TEXT)
    }

    private fun lexTemplateContent(endMarker: String, endToken: IElementType, nextState: Int) {
        val pos = tokenStart

        // Skip whitespace
        if (pos < bufferEnd && buffer[pos].isWhitespace()) {
            var end = pos
            while (end < bufferEnd && buffer[end].isWhitespace()) {
                end++
            }
            tokenEnd = end
            tokenType = TmplTokenTypes.WHITE_SPACE
            return
        }

        // Check for end marker
        if (pos + 1 < bufferEnd && buffer[pos] == endMarker[0] && buffer[pos + 1] == endMarker[1]) {
            tokenEnd = pos + 2
            tokenType = endToken
            state = nextState
            return
        }

        val c = buffer[pos]

        // String literals
        if (c == '"' || c == '\'') {
            val quote = c
            var end = pos + 1
            while (end < bufferEnd && buffer[end] != quote) {
                if (buffer[end] == '\\' && end + 1 < bufferEnd) {
                    end += 2
                } else {
                    end++
                }
            }
            if (end < bufferEnd) end++ // Include closing quote
            tokenEnd = end
            tokenType = TmplTokenTypes.STRING
            return
        }

        // Numbers
        if (c.isDigit()) {
            var end = pos
            while (end < bufferEnd && (buffer[end].isDigit() || buffer[end] == '.')) {
                end++
            }
            tokenEnd = end
            tokenType = TmplTokenTypes.NUMBER
            return
        }

        // Identifiers and keywords
        if (c.isLetter() || c == '_') {
            var end = pos
            while (end < bufferEnd && (buffer[end].isLetterOrDigit() || buffer[end] == '_')) {
                end++
            }
            val text = buffer.substring(pos, end)
            tokenEnd = end
            tokenType = if (text in TmplTokenTypes.KEYWORDS) TmplTokenTypes.KEYWORD else TmplTokenTypes.IDENTIFIER
            return
        }

        // Single character tokens
        tokenEnd = pos + 1
        tokenType = when (c) {
            '(' -> TmplTokenTypes.LPAREN
            ')' -> TmplTokenTypes.RPAREN
            '[' -> TmplTokenTypes.LBRACKET
            ']' -> TmplTokenTypes.RBRACKET
            ',' -> TmplTokenTypes.COMMA
            '.' -> TmplTokenTypes.DOT
            '|' -> TmplTokenTypes.PIPE
            '=' -> TmplTokenTypes.EQUALS
            ':' -> TmplTokenTypes.COLON
            '+', '-', '*', '/', '<', '>', '!', '~' -> TmplTokenTypes.OPERATOR
            else -> TmplTokenTypes.BAD_CHARACTER
        }
    }

    private fun lexComment() {
        val pos = tokenStart

        // Scan until we find #}
        var end = pos
        while (end + 1 < bufferEnd) {
            if (buffer[end] == '#' && buffer[end + 1] == '}') {
                break
            }
            end++
        }

        if (end > pos) {
            tokenEnd = end
            tokenType = TmplTokenTypes.COMMENT_CONTENT
            return
        }

        // Found #}
        if (end + 1 < bufferEnd && buffer[end] == '#' && buffer[end + 1] == '}') {
            tokenEnd = end + 2
            tokenType = TmplTokenTypes.COMMENT_END
            state = STATE_TEXT
            return
        }

        // End of file in comment
        tokenEnd = bufferEnd
        tokenType = TmplTokenTypes.COMMENT_CONTENT
    }
}
