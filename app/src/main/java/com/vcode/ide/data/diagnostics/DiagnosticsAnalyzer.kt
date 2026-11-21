package com.vcode.ide.data.diagnostics

import com.vcode.ide.domain.model.LanguageMode
import com.vcode.ide.domain.model.ProblemItem
import com.vcode.ide.domain.model.ProblemSeverity
import org.json.JSONArray
import org.json.JSONObject

object DiagnosticsAnalyzer {
    fun analyze(filePath: String, fileName: String, language: LanguageMode, content: String): List<ProblemItem> {
        val problems = mutableListOf<ProblemItem>()
        when (language) {
            LanguageMode.JSON -> validateJson(filePath, fileName, content, problems)
            LanguageMode.HTML, LanguageMode.XML -> validateMarkupTags(filePath, fileName, content, problems)
            LanguageMode.JAVASCRIPT, LanguageMode.TYPESCRIPT, LanguageMode.JAVA,
            LanguageMode.KOTLIN, LanguageMode.C, LanguageMode.CPP, LanguageMode.CSS,
            LanguageMode.PHP -> {
                validateDelimiters(filePath, fileName, language.displayName, content, problems)
                if (language == LanguageMode.JAVASCRIPT || language == LanguageMode.TYPESCRIPT) {
                    checkCommonJsSmells(filePath, fileName, content, problems)
                }
            }
            LanguageMode.PYTHON -> validatePythonBasics(filePath, fileName, content, problems)
            else -> {}
        }
        return problems
    }

    private fun validateJson(filePath: String, fileName: String, content: String, out: MutableList<ProblemItem>) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return
        try {
            if (trimmed.startsWith("[")) JSONArray(trimmed) else JSONObject(trimmed)
        } catch (e: Exception) {
            val msg = e.message ?: "Invalid JSON syntax"
            val lineGuess = Regex("line (\\d+)").find(msg)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: content.lines().size.coerceAtLeast(1)
            out += ProblemItem(filePath, fileName, lineGuess, 1, msg, ProblemSeverity.ERROR, "JSON")
        }
    }

    private fun validateDelimiters(filePath: String, fileName: String, source: String, content: String, out: MutableList<ProblemItem>) {
        val stack = ArrayDeque<Triple<Char, Int, Int>>()
        var line = 1; var col = 0
        var inSingle = false; var inDouble = false; var inTick = false; var inLineC = false; var inBlockC = false
        var i = 0
        while (i < content.length) {
            val c = content[i]; val next = content.getOrNull(i + 1)
            if (c == '\n') { line++; col = 0; inLineC = false; i++; continue }
            col++
            if (inLineC) { i++; continue }
            if (inBlockC) { if (c == '*' && next == '/') { inBlockC = false; i += 2; col++; continue }; i++; continue }
            if (!inSingle && !inDouble && !inTick) {
                if (c == '/' && next == '/') { inLineC = true; i += 2; col++; continue }
                if (c == '/' && next == '*') { inBlockC = true; i += 2; col++; continue }
            }
            if (c == '\\' && (inSingle || inDouble || inTick)) { i += 2; col++; continue }
            when {
                c == '\'' && !inDouble && !inTick -> inSingle = !inSingle
                c == '"' && !inSingle && !inTick -> inDouble = !inDouble
                c == '`' && !inSingle && !inDouble -> inTick = !inTick
                !inSingle && !inDouble && !inTick -> when (c) {
                    '{', '(', '[' -> stack.addLast(Triple(c, line, col))
                    '}', ')', ']' -> {
                        val expected = when (c) { '}' -> '{'; ')' -> '('; else -> '[' }
                        val top = stack.lastOrNull()
                        if (top == null) out += ProblemItem(filePath, fileName, line, col, "Unexpected '$c'", ProblemSeverity.ERROR, source)
                        else if (top.first != expected) {
                            out += ProblemItem(filePath, fileName, line, col, "Mismatched '$c' for '${top.first}' on line ${top.second}", ProblemSeverity.ERROR, source)
                            stack.removeLast()
                        } else stack.removeLast()
                    }
                }
            }
            i++
        }
        if (inBlockC) out += ProblemItem(filePath, fileName, line, 1, "Unclosed block comment", ProblemSeverity.ERROR, source)
        while (stack.isNotEmpty()) {
            val (ch, ln, cp) = stack.removeLast()
            out += ProblemItem(filePath, fileName, ln, cp, "Unclosed '$ch'", ProblemSeverity.ERROR, source)
        }
    }

    private fun validateMarkupTags(filePath: String, fileName: String, content: String, out: MutableList<ProblemItem>) {
        val voidTags = setOf("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr", "!doctype")
        val stack = ArrayDeque<Pair<String, Int>>()
        val tagRegex = Regex("<(/?)([a-zA-Z0-9:-]+)([^>]*)>")
        content.lines().forEachIndexed { idx, lineText ->
            val clean = lineText.replace(Regex("<!--.*?-->"), "")
            tagRegex.findAll(clean).forEach { match ->
                val isClose = match.groupValues[1] == "/"
                val tag = match.groupValues[2].lowercase()
                val tail = match.groupValues[3]
                if (!tail.trimEnd().endsWith("/") && tag !in voidTags) {
                    if (!isClose) stack.addLast(tag to (idx + 1))
                    else {
                        val last = stack.lastOrNull()
                        if (last == null) out += ProblemItem(filePath, fileName, idx + 1, match.range.first + 1, "Unexpected </$tag>", ProblemSeverity.WARNING, "Markup")
                        else if (last.first != tag) out += ProblemItem(filePath, fileName, idx + 1, match.range.first + 1, "Expected </${last.first}> before </$tag>", ProblemSeverity.WARNING, "Markup")
                        else stack.removeLast()
                    }
                }
            }
        }
        stack.forEach { (tag, ln) -> out += ProblemItem(filePath, fileName, ln, 1, "Unclosed <$tag>", ProblemSeverity.WARNING, "Markup") }
    }

    private fun checkCommonJsSmells(filePath: String, fileName: String, content: String, out: MutableList<ProblemItem>) {
        content.lines().forEachIndexed { idx, line ->
            val t = line.trim()
            if (t.contains("TODO:") || t.contains("FIXME:")) out += ProblemItem(filePath, fileName, idx + 1, 1, t.take(80), ProblemSeverity.INFO, "Lint")
            if (t.startsWith("var ")) out += ProblemItem(filePath, fileName, idx + 1, 1, "Prefer 'const' or 'let' over 'var'", ProblemSeverity.WARNING, "JS Lint")
        }
    }

    private fun validatePythonBasics(filePath: String, fileName: String, content: String, out: MutableList<ProblemItem>) {
        content.lines().forEachIndexed { idx, line ->
            if (line.contains('\t') && line.startsWith(" ")) out += ProblemItem(filePath, fileName, idx + 1, 1, "Mixed tabs and spaces", ProblemSeverity.WARNING, "Python")
        }
    }
}
