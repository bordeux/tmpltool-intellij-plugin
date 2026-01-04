package com.bordeux.tmpltool.parser

import com.bordeux.tmpltool.TmplOuterElementType
import com.intellij.lang.ASTFactory
import com.intellij.psi.impl.source.tree.LeafElement
import com.intellij.psi.templateLanguages.OuterLanguageElementImpl
import com.intellij.psi.tree.IElementType

/**
 * Custom AST factory for Tmpltool language.
 * Creates OuterLanguageElementImpl for TMPL_OUTER element type to satisfy
 * TemplateDataElementType's requirement for OuterLanguageElement.
 *
 * This follows the same pattern used by the JetBrains Handlebars plugin.
 */
class TmplASTFactory : ASTFactory() {

    override fun createLeaf(type: IElementType, text: CharSequence): LeafElement? {
        if (type == TmplOuterElementType.INSTANCE) {
            return OuterLanguageElementImpl(type, text)
        }
        return super.createLeaf(type, text)
    }
}
