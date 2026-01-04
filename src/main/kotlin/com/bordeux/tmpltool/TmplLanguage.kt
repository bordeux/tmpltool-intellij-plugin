package com.bordeux.tmpltool

import com.intellij.lang.Language

class TmplLanguage private constructor() : Language("Tmpltool") {
    companion object {
        @JvmStatic
        val INSTANCE = TmplLanguage()
    }
}
