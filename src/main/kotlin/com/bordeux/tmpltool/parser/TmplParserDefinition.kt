package com.bordeux.tmpltool.parser

import com.bordeux.tmpltool.TmplLanguage
import com.bordeux.tmpltool.lexer.TmplLexer
import com.bordeux.tmpltool.lexer.TmplTokenTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class TmplParserDefinition : ParserDefinition {

    companion object {
        val FILE = IFileElementType(TmplLanguage.INSTANCE)

        val COMMENTS = TokenSet.create(
            TmplTokenTypes.COMMENT_START,
            TmplTokenTypes.COMMENT_CONTENT,
            TmplTokenTypes.COMMENT_END
        )

        val STRINGS = TokenSet.create(TmplTokenTypes.STRING)

        val WHITE_SPACES = TokenSet.create(TmplTokenTypes.WHITE_SPACE)
    }

    override fun createLexer(project: Project?): Lexer = TmplLexer()

    override fun createParser(project: Project?): PsiParser = TmplParser()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet = COMMENTS

    override fun getStringLiteralElements(): TokenSet = STRINGS

    override fun getWhitespaceTokens(): TokenSet = WHITE_SPACES

    override fun createElement(node: ASTNode): PsiElement {
        // Note: Outer language elements are created by TmplASTFactory
        return TmplPsiElement(node)
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile = TmplFile(viewProvider)
}
