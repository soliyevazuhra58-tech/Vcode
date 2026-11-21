package com.vcode.ide

import com.vcode.ide.data.diagnostics.DiagnosticsAnalyzer
import com.vcode.ide.data.diagnostics.FormatterEngine
import com.vcode.ide.domain.model.LanguageMode
import com.vcode.ide.domain.model.ProblemSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsAndFormatterTest {
    @Test
    fun detectsUnclosedBraceInKotlin() {
        val problems = DiagnosticsAnalyzer.analyze("/tmp/Main.kt", "Main.kt", LanguageMode.KOTLIN, "fun hello() {\n println(\"Hi\")")
        assertTrue(problems.any { it.severity == ProblemSeverity.ERROR && it.message.contains("Unclosed") })
    }

    @Test
    fun detectsInvalidJson() {
        val problems = DiagnosticsAnalyzer.analyze("/tmp/a.json", "a.json", LanguageMode.JSON, """{ "name": "VCode", }""")
        assertEquals(1, problems.size)
        assertEquals(ProblemSeverity.ERROR, problems.first().severity)
    }

    @Test
    fun formatsJsonWithIndentation() {
        val formatted = FormatterEngine.format("""{"a":1,"b":[2,3]}""", LanguageMode.JSON, 2)
        assertTrue(formatted.contains("\n") && formatted.contains("  \"a\": 1"))
    }
}
