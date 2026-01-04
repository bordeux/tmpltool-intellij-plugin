package com.bordeux.tmpltool

import com.intellij.lang.Language
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.MultiplePsiFilesPerDocumentFileViewProvider
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.psi.templateLanguages.ConfigurableTemplateLanguageFileViewProvider
import com.intellij.psi.templateLanguages.TemplateDataElementType
import com.intellij.psi.tree.IElementType

class TmplFileViewProvider(
    manager: PsiManager,
    file: VirtualFile,
    eventSystemEnabled: Boolean,
    private val baseLanguage: Language?
) : MultiplePsiFilesPerDocumentFileViewProvider(manager, file, eventSystemEnabled),
    ConfigurableTemplateLanguageFileViewProvider {

    companion object {
        private val templateDataElementTypes = mutableMapOf<Language, TemplateDataElementType>()

        @Synchronized
        fun getTemplateDataElementType(language: Language): TemplateDataElementType {
            return templateDataElementTypes.getOrPut(language) {
                TemplateDataElementType(
                    "TMPL_TEMPLATE_DATA_${language.id}",
                    TmplLanguage.INSTANCE,
                    TmplOuterElementType.INSTANCE,
                    TmplOuterElementType.INSTANCE
                )
            }
        }
    }

    override fun getBaseLanguage(): Language = TmplLanguage.INSTANCE

    override fun getTemplateDataLanguage(): Language {
        // Return the base language detected from the file extension
        // (e.g., .yaml.tmpltool -> YAML, .json.tmpltool -> JSON)
        return baseLanguage ?: Language.ANY
    }

    override fun getLanguages(): Set<Language> {
        val templateDataLang = templateDataLanguage
        return if (templateDataLang != Language.ANY && templateDataLang != TmplLanguage.INSTANCE) {
            setOf(TmplLanguage.INSTANCE, templateDataLang)
        } else {
            setOf(TmplLanguage.INSTANCE)
        }
    }

    override fun cloneInner(virtualFile: VirtualFile): MultiplePsiFilesPerDocumentFileViewProvider {
        return TmplFileViewProvider(manager, virtualFile, false, baseLanguage)
    }

    override fun createFile(lang: Language): PsiFile? {
        val parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(lang) ?: return null

        return if (lang === TmplLanguage.INSTANCE) {
            parserDefinition.createFile(this)
        } else if (lang === templateDataLanguage) {
            val file = parserDefinition.createFile(this) as? PsiFileImpl
            file?.contentElementType = getTemplateDataElementType(lang)
            file
        } else {
            null
        }
    }

    override fun supportsIncrementalReparse(rootLanguage: Language): Boolean = false
}

/**
 * Element type for content outside of template tags (the "outer" template language content).
 * Used by TemplateDataElementType to identify outer language regions.
 * The TmplASTFactory creates OuterLanguageElementImpl for this element type.
 */
class TmplOuterElementType private constructor() : IElementType("TMPL_OUTER", TmplLanguage.INSTANCE) {
    companion object {
        val INSTANCE = TmplOuterElementType()
    }
}
