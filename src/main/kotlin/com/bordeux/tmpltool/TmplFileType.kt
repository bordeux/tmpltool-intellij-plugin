package com.bordeux.tmpltool

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

class TmplFileType private constructor() : LanguageFileType(TmplLanguage.INSTANCE) {

    override fun getName(): String = "Tmpltool Template"

    override fun getDescription(): String = "Tmpltool template file"

    override fun getDefaultExtension(): String = "tmpltool"

    override fun getIcon(): Icon = TmplIcons.FILE

    companion object {
        @JvmStatic
        val INSTANCE = TmplFileType()
    }
}
