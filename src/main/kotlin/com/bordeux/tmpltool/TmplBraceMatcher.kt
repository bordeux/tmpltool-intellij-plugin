package com.bordeux.tmpltool

import com.bordeux.tmpltool.lexer.TmplTokenTypes
import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

class TmplBraceMatcher : PairedBraceMatcher {

    companion object {
        private val PAIRS = arrayOf(
            BracePair(TmplTokenTypes.EXPR_START, TmplTokenTypes.EXPR_END, true),
            BracePair(TmplTokenTypes.STMT_START, TmplTokenTypes.STMT_END, true),
            BracePair(TmplTokenTypes.COMMENT_START, TmplTokenTypes.COMMENT_END, true),
            BracePair(TmplTokenTypes.LPAREN, TmplTokenTypes.RPAREN, false),
            BracePair(TmplTokenTypes.LBRACKET, TmplTokenTypes.RBRACKET, false),
        )
    }

    override fun getPairs(): Array<BracePair> = PAIRS

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset
}
