package com.bordeux.tmpltool.reference

import com.bordeux.tmpltool.TmplLanguage
import com.bordeux.tmpltool.lexer.TmplTokenTypes
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.util.elementType
import com.intellij.util.ProcessingContext

class TmplReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // Register reference provider for string literals (potential file paths)
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement()
                .withLanguage(TmplLanguage.INSTANCE),
            TmplIncludeReferenceProvider()
        )
    }
}

class TmplIncludeReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        // Only process string literals
        if (element.elementType != TmplTokenTypes.STRING) {
            return PsiReference.EMPTY_ARRAY
        }

        // Check if this is an include statement
        if (!isInIncludeStatement(element)) {
            return PsiReference.EMPTY_ARRAY
        }

        val text = element.text
        if (text.length < 2) return PsiReference.EMPTY_ARRAY

        // Remove quotes
        val path = text.substring(1, text.length - 1)
        if (path.isEmpty()) return PsiReference.EMPTY_ARRAY

        // Create reference for the path (without quotes)
        val range = TextRange(1, text.length - 1)
        return arrayOf(TmplIncludeReference(element, range, path))
    }

    private fun isInIncludeStatement(element: PsiElement): Boolean {
        // Walk backwards to find "include" keyword
        var sibling = element.prevSibling
        var foundInclude = false

        while (sibling != null) {
            val text = sibling.text.trim()
            when {
                text == "include" -> {
                    foundInclude = true
                    break
                }
                text == "{%" || text == "{{" -> break
                text.isNotEmpty() && text !in listOf("include", "") && !text.isBlank() -> {
                    // Found something else, not an include
                    if (sibling.elementType == TmplTokenTypes.KEYWORD && text != "include") {
                        break
                    }
                }
            }
            sibling = sibling.prevSibling
        }

        return foundInclude
    }
}

class TmplIncludeReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val path: String
) : PsiReferenceBase<PsiElement>(element, rangeInElement, true) {

    override fun resolve(): PsiElement? {
        val containingFile = element.containingFile?.virtualFile ?: return null
        val baseDir = containingFile.parent ?: return null

        // Try to find the referenced file
        val targetFile = baseDir.findFileByRelativePath(path) ?: return null

        // Return the PsiFile for navigation
        val project = element.project
        return PsiManager.getInstance(project).findFile(targetFile)
    }

    override fun getVariants(): Array<Any> {
        // Provide completion for template files
        val containingFile = element.containingFile?.virtualFile ?: return emptyArray()
        val baseDir = containingFile.parent ?: return emptyArray()

        val variants = mutableListOf<String>()
        collectTemplateFiles(baseDir, "", variants)

        return variants.toTypedArray()
    }

    private fun collectTemplateFiles(
        dir: com.intellij.openapi.vfs.VirtualFile,
        prefix: String,
        results: MutableList<String>
    ) {
        for (child in dir.children) {
            val relativePath = if (prefix.isEmpty()) child.name else "$prefix/${child.name}"
            when {
                child.isDirectory -> collectTemplateFiles(child, relativePath, results)
                child.name.endsWith(".tmpltool") -> results.add(relativePath)
            }
        }
    }
}
