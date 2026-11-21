package com.vcode.ide.domain.model

import androidx.compose.ui.graphics.Color

enum class SidebarTab(val title: String) {
    EXPLORER("Explorer"),
    SEARCH("Search"),
    SOURCE_CONTROL("Source Control"),
    EXTENSIONS("Extensions"),
    PREVIEW("Live Preview"),
    SETTINGS("Settings")
}

enum class BottomPanelTab(val title: String) {
    TERMINAL("Terminal"),
    OUTPUT("Output"),
    PROBLEMS("Problems")
}

enum class EditorThemeId(
    val id: String,
    val displayName: String,
    val assetPath: String,
    val isDark: Boolean
) {
    DARK_PLUS("dark-plus", "VS Code Dark+", "textmate/themes/dark-plus.json", true),
    ONE_DARK_PRO("one-dark-pro", "One Dark Pro", "textmate/themes/one-dark-pro.json", true),
    DRACULA("dracula", "Dracula", "textmate/themes/dracula.json", true),
    MONOKAI("monokai", "Monokai", "textmate/themes/monokai.json", true),
    LIGHT_PLUS("light-plus", "VS Code Light+", "textmate/themes/light-plus.json", false),
    GITHUB_LIGHT("github-light", "GitHub Light", "textmate/themes/github-light.json", false);

    companion object {
        fun fromId(id: String): EditorThemeId = entries.firstOrNull { it.id == id } ?: DARK_PLUS
    }
}

enum class EditorFontFamily(val id: String, val displayName: String) {
    MONOSPACE("monospace", "JetBrains / Monospace"),
    SANS_SERIF("sans", "System Sans"),
    SERIF("serif", "Classic Serif");

    companion object {
        fun fromId(id: String): EditorFontFamily = entries.firstOrNull { it.id == id } ?: MONOSPACE
    }
}

enum class LanguageMode(
    val id: String,
    val displayName: String,
    val scopeName: String?,
    val extensions: List<String>,
    val badge: String,
    val badgeColorHex: Long
) {
    HTML("html", "HTML", "text.html.basic", listOf("html", "htm"), "HTML", 0xFFE34C26),
    CSS("css", "CSS", "source.css", listOf("css", "scss"), "CSS", 0xFF264DE4),
    JAVASCRIPT("javascript", "JavaScript", "source.js", listOf("js", "mjs", "cjs", "jsx"), "JS", 0xFFF7DF1E),
    TYPESCRIPT("typescript", "TypeScript", "source.ts", listOf("ts", "tsx"), "TS", 0xFF3178C6),
    PYTHON("python", "Python", "source.python", listOf("py"), "PY", 0xFF3776AB),
    JAVA("java", "Java", "source.java", listOf("java"), "JAVA", 0xFFED8B00),
    KOTLIN("kotlin", "Kotlin", "source.kotlin", listOf("kt", "kts"), "KT", 0xFF7F52FF),
    C("c", "C", "source.c", listOf("c", "h"), "C", 0xFF5C6BC0),
    CPP("cpp", "C++", "source.cpp", listOf("cpp", "cc", "cxx", "hpp"), "C++", 0xFF00599C),
    JSON("json", "JSON", "source.json", listOf("json", "jsonc"), "{}", 0xFFCBCB41),
    XML("xml", "XML", "text.xml", listOf("xml", "svg", "gradle"), "XML", 0xFFE37933),
    PHP("php", "PHP", "source.php", listOf("php"), "PHP", 0xFF777BB4),
    SQL("sql", "SQL", "source.sql", listOf("sql"), "SQL", 0xFFE38C00),
    MARKDOWN("markdown", "Markdown", "text.html.markdown", listOf("md", "markdown"), "MD", 0xFF519ABA),
    SHELL("shellscript", "Shell", "source.shell", listOf("sh", "bash", "zsh"), "SH", 0xFF89E051),
    PLAIN_TEXT("plaintext", "Plain Text", null, listOf("txt", "log", "env", "gitignore"), "TXT", 0xFF8D8D8D);

    val badgeColor: Color get() = Color(badgeColorHex)

    companion object {
        fun detect(fileName: String): LanguageMode {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            return entries.firstOrNull { ext in it.extensions } ?: PLAIN_TEXT
        }

        fun fromId(id: String): LanguageMode = entries.firstOrNull { it.id == id } ?: PLAIN_TEXT
    }
}

data class AppSettings(
    val themeId: EditorThemeId = EditorThemeId.DARK_PLUS,
    val fontFamily: EditorFontFamily = EditorFontFamily.MONOSPACE,
    val fontSizeSp: Int = 14,
    val tabSize: Int = 2,
    val wordWrap: Boolean = false,
    val showLineNumbers: Boolean = true,
    val showMinimap: Boolean = true,
    val codeFolding: Boolean = true,
    val autoSave: Boolean = true,
    val autoSaveDelayMs: Long = 1200L,
    val autoIndent: Boolean = true,
    val bracketMatching: Boolean = true,
    val showExtraKeyBar: Boolean = true,
    val accentColorHex: Long = 0xFF007ACC,
    val enabledExtensions: Set<String> = setOf(
        "ext.textmate.essentials",
        "ext.prettier.formatter",
        "ext.emmet.snippets",
        "ext.git.lens",
        "ext.material.icons"
    )
)

data class VCodeProject(
    val id: String,
    val name: String,
    val rootPath: String,
    val templateId: String,
    val lastOpenedAt: Long,
    val linkedTreeUri: String? = null
)

data class FileNode(
    val path: String,
    val relativePath: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val depth: Int,
    val isExpanded: Boolean = false
)

enum class ClipboardMode { COPY, CUT }

data class FileClipboard(
    val sourcePath: String,
    val fileName: String,
    val mode: ClipboardMode
)

data class EditorTab(
    val filePath: String,
    val relativePath: String,
    val title: String,
    val content: String,
    val isDirty: Boolean = false,
    val language: LanguageMode = LanguageMode.detect(title),
    val cursorLine: Int = 1,
    val cursorColumn: Int = 1,
    val version: Long = 0L
)

enum class ProblemSeverity { ERROR, WARNING, INFO }

data class ProblemItem(
    val filePath: String,
    val fileName: String,
    val line: Int,
    val column: Int,
    val message: String,
    val severity: ProblemSeverity,
    val source: String
)

data class SearchMatch(
    val filePath: String,
    val relativePath: String,
    val fileName: String,
    val line: Int,
    val column: Int,
    val linePreview: String
)

data class GitFileStatus(
    val relativePath: String,
    val status: String,
    val isStaged: Boolean
)

data class GitCommitInfo(
    val shortId: String,
    val message: String,
    val author: String,
    val timestamp: Long
)

data class GitState(
    val isInitialized: Boolean = false,
    val currentBranch: String = "main",
    val branches: List<String> = emptyList(),
    val changes: List<GitFileStatus> = emptyList(),
    val commits: List<GitCommitInfo> = emptyList(),
    val selectedDiff: String = ""
)

data class ExtensionItem(
    val id: String,
    val name: String,
    val publisher: String,
    val version: String,
    val description: String,
    val category: String
)

enum class ViewportDeviceType(
    val label: String,
    val defaultWidthDp: Int,
    val defaultHeightDp: Int
) {
    RESPONSIVE("Responsive", 0, 0),
    MOBILE("Mobile", 390, 844),
    TABLET("Tablet", 768, 1024),
    DESKTOP("Desktop", 1280, 800),
    CUSTOM("Custom", 480, 720)
}

data class ViewportConfig(
    val deviceType: ViewportDeviceType = ViewportDeviceType.MOBILE,
    val widthPx: Int = 390,
    val heightPx: Int = 844,
    val isLandscape: Boolean = false
) {
    val effectiveWidth: Int get() = if (isLandscape && deviceType != ViewportDeviceType.RESPONSIVE) heightPx else widthPx
    val effectiveHeight: Int get() = if (isLandscape && deviceType != ViewportDeviceType.RESPONSIVE) widthPx else heightPx
}

data class ConsoleLogEntry(
    val id: Long,
    val level: String,
    val message: String,
    val source: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class OutputEntry(
    val id: Long,
    val tag: String,
    val message: String,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class ProjectTemplate(
    val id: String,
    val name: String,
    val subtitle: String,
    val badge: String,
    val files: Map<String, String>
)

data class CommandItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val shortcut: String = "",
    val action: () -> Unit
)
