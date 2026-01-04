package com.bordeux.tmpltool

import com.bordeux.tmpltool.injection.TmplBaseLanguageProvider
import com.intellij.lang.Language
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.templateLanguages.TemplateDataLanguageMappings

class TmplFileViewProviderFactory : FileViewProviderFactory {
    override fun createFileViewProvider(
        file: VirtualFile,
        language: Language?,
        manager: PsiManager,
        eventSystemEnabled: Boolean
    ): FileViewProvider {
        // Check if there's a manually configured template data language
        val mappings = TemplateDataLanguageMappings.getInstance(manager.project)
        val configuredLanguage = mappings.getMapping(file)

        // Use configured language, or detect from extension, or fall back to plain text
        val baseLanguage = configuredLanguage
            ?: TmplBaseLanguageProvider.getBaseLanguage(file)

        return if (baseLanguage != null) {
            TmplFileViewProvider(manager, file, eventSystemEnabled, baseLanguage)
        } else {
            TmplFileViewProvider(manager, file, eventSystemEnabled, null)
        }
    }
}
