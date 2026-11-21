package com.vcode.ide.data.diagnostics

import com.vcode.ide.domain.model.LanguageMode

object FormatterEngine {
    fun format(content: String, language: LanguageMode, tabSize: Int = 2): String {
        return when (language) {
            LanguageMode.JSON -> formatJson(content, tabSize)
            LanguageMode.HTML, LanguageMode.XML -> formatMarkup(content, tabSize)
            LanguageMode.CSS, LanguageMode.JAVASCRIPT, LanguageMode.TYPESCRIPT,
            LanguageMode.JAVA, LanguageMode.KOTLIN, LanguageMode.C, LanguageMode.CPP,
            LanguageMode.PHP -> formatBraceLanguage(content, tabSize)
            else -> content.lines().joinToString("\n") { it.trimEnd() }
        }
    }

    private fun formatJson(content: String, tabSize: Int): String {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return content
        val indentUnit = " ".repeat(tabSize)
        val out = StringBuilder(trimmed.length + 32)
        var level = 0
        var inString = false
        var i = 0
        while (i < trimmed.length) {
            val c = trimmed[i]
            if (inString) {
                out.append(c)
                if (c == '\\' && i + 1 < trimmed.length) {
                    out.append(trimmed[++i])
                } else if (c == '"') {
                    inString = false
                }
            } else {
                when (c) {
                    '"' -> {
                        inString = true
                        out.append(c)
                    }
                    '{', '[' -> {
                        out.append(c)
                        val nextNonWs = nextNonWhitespace(trimmed, i + 1)
                        if ((c == '{' && nextNonWs == '}') || (c == '[' && nextNonWs == ']')) {
                            // keep empty {} or [] compact
                        } else {
                            level++
                            out.append('\n').append(indentUnit.repeat(level))
                        }
                    }
                    '}', ']' -> {
                        val prevNonWs = out.lastOrNull { !it.isWhitespace() }
                        if ((c == '}' && prevNonWs == '{') || (c == ']' && prevNonWs == '[')) {
                            out.append(c)
                        } else {
                            level = (level - 1).coerceAtLeast(0)
                            out.append('\n').append(indentUnit.repeat(level)).append(c)
                        }
                    }
                    ',' -> {
                        out.append(",\n").append(indentUnit.repeat(level))
                    }
                    ':' -> {
                        out.append(": ")
                    }
                    ' ', '\t', '\r', '\n' -> {}
                    else -> out.append(c)
                }
            }
            i++
        }
        return out.toString()
    }

    private fun nextNonWhitespace(s: String, start: Int): Char? {
        var idx = start
        while (idx < s.length) {
            val c = s[idx]
            if (!c.isWhitespace()) return c
            idx++
        }
        return null
    }

    private fun formatBraceLanguage(content: String, tabSize: Int): String {
        val indentUnit = " ".repeat(tabSize)
        val out = StringBuilder()
        var level = 0
        content.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) { out.append('\n'); return@forEach }
            if (line.startsWith("}") || line.startsWith("]") || line.startsWith(")")) level = (level - 1).coerceAtLeast(0)
            out.append(indentUnit.repeat(level)).append(line).append('\n')
            val net = line.count { it == '{' } - line.count { it == '}' }
            level = if (line.startsWith("}") && net < 0) (level + net + 1).coerceAtLeast(0) else (level + net).coerceAtLeast(0)
        }
        return out.toString().trimEnd() + "\n"
    }

    private fun formatMarkup(content: String, tabSize: Int): String {
        val indentUnit = " ".repeat(tabSize)
        val out = StringBuilder()
        var level = 0
        val voidTags = setOf("meta", "link", "img", "br", "hr", "input", "!doctype")
        content.lines().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) { out.append('\n'); return@forEach }
            if (line.startsWith("</")) level = (level - 1).coerceAtLeast(0)
            out.append(indentUnit.repeat(level)).append(line).append('\n')
            val openMatch = Regex("^<([a-zA-Z0-9:-]+)\\b[^>]*>$").find(line)
            if (openMatch != null) {
                val tag = openMatch.groupValues[1].lowercase()
                if (!line.endsWith("/>") && tag !in voidTags && !line.contains("</$tag>")) level++
            }
        }
        return out.toString().trimEnd() + "\n"
    }
}
