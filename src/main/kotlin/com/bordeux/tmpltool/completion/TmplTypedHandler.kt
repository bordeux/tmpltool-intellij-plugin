package com.bordeux.tmpltool.completion

import com.bordeux.tmpltool.TmplLanguage
import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

/**
 * Handles typed characters in Tmpltool files to trigger auto-popup for parameter info.
 */
class TmplTypedHandler : TypedHandlerDelegate() {

    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        // Check if we're in a Tmpltool file
        val viewProvider = file.viewProvider
        val tmplFile = viewProvider.getPsi(TmplLanguage.INSTANCE)

        if (tmplFile == null && file.language != TmplLanguage.INSTANCE) {
            return Result.CONTINUE
        }

        when (c) {
            '(' -> {
                // Auto-show parameter info when opening parenthesis is typed
                AutoPopupController.getInstance(project).autoPopupParameterInfo(editor, null)
            }
            ',' -> {
                // Also trigger on comma to update parameter index
                AutoPopupController.getInstance(project).autoPopupParameterInfo(editor, null)
            }
        }

        return Result.CONTINUE
    }
}
