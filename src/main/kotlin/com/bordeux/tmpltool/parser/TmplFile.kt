package com.bordeux.tmpltool.parser

import com.bordeux.tmpltool.TmplFileType
import com.bordeux.tmpltool.TmplLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

class TmplFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, TmplLanguage.INSTANCE) {
    override fun getFileType(): FileType = TmplFileType.INSTANCE

    override fun toString(): String = "Tmpltool Template File"
}
