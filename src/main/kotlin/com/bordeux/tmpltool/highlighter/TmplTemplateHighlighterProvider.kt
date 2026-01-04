package com.bordeux.tmpltool.highlighter

import com.bordeux.tmpltool.TmplFileType
import com.bordeux.tmpltool.TmplLanguage
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.ex.util.LayerDescriptor
import com.intellij.openapi.editor.ex.util.LayeredLexerEditorHighlighter
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.fileTypes.EditorHighlighterProvider
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.templateLanguages.TemplateDataLanguageMappings
import com.bordeux.tmpltool.injection.TmplBaseLanguageProvider
import com.bordeux.tmpltool.TmplOuterElementType

class TmplTemplateHighlighterProvider : EditorHighlighterProvider {

    override fun getEditorHighlighter(
        project: Project?,
        fileType: FileType,
        virtualFile: VirtualFile?,
        colors: EditorColorsScheme
    ): EditorHighlighter {
        return TmplEditorHighlighter(project, virtualFile, colors)
    }
}

class TmplEditorHighlighter(
    private val project: Project?,
    private val virtualFile: VirtualFile?,
    colors: EditorColorsScheme
) : LayeredLexerEditorHighlighter(TmplSyntaxHighlighter(), colors) {

    init {
        registerBaseLanguageLayer()
    }

    private fun registerBaseLanguageLayer() {
        if (project == null || virtualFile == null) return

        // First check for user-configured template data language
        val mappings = TemplateDataLanguageMappings.getInstance(project)
        val configuredLanguage = mappings.getMapping(virtualFile)

        // Use configured language or detect from filename
        val baseLanguage = configuredLanguage ?: TmplBaseLanguageProvider.getBaseLanguage(virtualFile)

        if (baseLanguage != null && baseLanguage !== TmplLanguage.INSTANCE) {
            val baseSyntaxHighlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(baseLanguage, project, virtualFile)
            if (baseSyntaxHighlighter != null) {
                val layerDescriptor = LayerDescriptor(baseSyntaxHighlighter, "")
                registerLayer(TmplOuterElementType.INSTANCE, layerDescriptor)
            }
        }
    }
}
