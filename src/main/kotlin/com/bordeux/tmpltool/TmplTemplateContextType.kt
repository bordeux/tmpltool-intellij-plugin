package com.bordeux.tmpltool

import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateContextType

class TmplTemplateContextType : TemplateContextType("Tmpltool template") {

    override fun isInContext(templateActionContext: TemplateActionContext): Boolean {
        val file = templateActionContext.file
        return file.name.endsWith(".tmpltool") || file.language == TmplLanguage.INSTANCE
    }
}
