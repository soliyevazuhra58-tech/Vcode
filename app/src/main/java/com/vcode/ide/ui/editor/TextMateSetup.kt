package com.vcode.ide.ui.editor

import android.content.Context
import com.vcode.ide.domain.model.EditorThemeId
import com.vcode.ide.domain.model.LanguageMode
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import org.eclipse.tm4e.core.registry.IThemeSource

object TextMateSetup {
    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            runCatching {
                FileProviderRegistry.getInstance()
                    .addFileProvider(AssetsFileResolver(context.applicationContext.assets))

                val themeRegistry = ThemeRegistry.getInstance()
                EditorThemeId.entries.forEach { theme ->
                    val stream = FileProviderRegistry.getInstance().tryGetInputStream(theme.assetPath)
                    if (stream != null) {
                        stream.use {
                            val source = IThemeSource.fromInputStream(it, theme.assetPath, null)
                            val model = ThemeModel(source, theme.id).apply { isDark = theme.isDark }
                            themeRegistry.loadTheme(model, false)
                        }
                    }
                }
                themeRegistry.setTheme(EditorThemeId.DARK_PLUS.id)
                GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
                initialized = true
            }
        }
    }

    fun applyTheme(themeId: EditorThemeId): TextMateColorScheme? {
        return runCatching {
            val registry = ThemeRegistry.getInstance()
            registry.setTheme(themeId.id)
            TextMateColorScheme.create(registry)
        }.getOrNull()
    }

    fun createLanguage(mode: LanguageMode, autoComplete: Boolean, tabSize: Int): Language {
        val scope = mode.scopeName ?: return EmptyLanguage()
        return runCatching {
            TextMateLanguage.create(scope, autoComplete).apply {
                this.tabSize = tabSize
                useTab(false)
                setCompleterKeywords(keywordsFor(mode))
            }
        }.getOrElse { EmptyLanguage() }
    }

    private fun keywordsFor(mode: LanguageMode): Array<String> = when (mode) {
        LanguageMode.HTML -> arrayOf("<!DOCTYPE html>", "html", "head", "body", "meta", "title", "link", "script", "style", "div", "span", "header", "main", "section", "article", "footer", "nav", "h1", "h2", "h3", "p", "a", "img", "button", "input", "form", "ul", "ol", "li")
        LanguageMode.CSS -> arrayOf("display", "flex", "grid", "position", "relative", "absolute", "background", "color", "font-family", "font-size", "margin", "padding", "border", "border-radius", "gap", "align-items", "justify-content", "grid-template-columns", ":root", "@media")
        LanguageMode.JAVASCRIPT, LanguageMode.TYPESCRIPT -> arrayOf("const", "let", "var", "function", "return", "async", "await", "export", "import", "from", "class", "interface", "type", "if", "else", "for", "while", "try", "catch", "console.log", "document.getElementById", "addEventListener", "Promise", "fetch")
        LanguageMode.PYTHON -> arrayOf("def", "class", "return", "import", "from", "as", "if", "elif", "else", "for", "while", "in", "try", "except", "with", "lambda", "async", "await", "print", "len", "range")
        LanguageMode.KOTLIN -> arrayOf("package", "import", "class", "data", "sealed", "object", "interface", "fun", "val", "var", "override", "suspend", "private", "if", "else", "when", "for", "while", "return", "println", "listOf")
        LanguageMode.JAVA -> arrayOf("package", "import", "public", "private", "class", "interface", "static", "final", "void", "int", "double", "boolean", "String", "new", "return", "if", "else", "for", "while", "System.out.println")
        LanguageMode.C, LanguageMode.CPP -> arrayOf("#include", "#define", "int", "void", "char", "double", "bool", "struct", "class", "namespace", "using", "std", "vector", "string", "cout", "endl", "printf", "return")
        LanguageMode.PHP -> arrayOf("<?php", "function", "class", "public", "private", "echo", "return", "if", "else", "foreach", "array")
        LanguageMode.SQL -> arrayOf("SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE", "CREATE", "TABLE", "PRIMARY", "KEY", "JOIN", "ORDER", "BY")
        LanguageMode.SHELL -> arrayOf("#!/bin/sh", "echo", "export", "if", "then", "else", "fi", "for", "do", "done", "mkdir", "grep", "find")
        else -> emptyArray()
    }
}
