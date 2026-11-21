package com.vcode.ide.data.diagnostics

import com.vcode.ide.domain.model.LanguageMode
import org.json.JSONArray
import org.json.JSONObject

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
        return runCatching {
            if (trimmed.startsWith("[")) JSONArray(trimmed).toString(tabSize) else JSONObject(trimmed).toString(tabSize)
        }.getOrDefault(content)
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
