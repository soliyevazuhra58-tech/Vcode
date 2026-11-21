package com.vcode.ide.ui.editor

import android.graphics.Typeface
import android.view.KeyEvent
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.vcode.ide.domain.model.AppSettings
import com.vcode.ide.domain.model.EditorFontFamily
import com.vcode.ide.domain.model.EditorTab
import com.vcode.ide.ui.theme.LocalVCodeColors
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorSearcher
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

@Composable
fun SoraEditorContainer(
    activeTab: EditorTab?,
    settings: AppSettings,
    showFindBar: Boolean,
    jumpToLineRequest: Pair<String, Int>?,
    onJumpHandled: () -> Unit,
    onContentChanged: (filePath: String, newText: String) -> Unit,
    onCursorChanged: (filePath: String, line: Int, column: Int) -> Unit,
    onCloseFindBar: () -> Unit,
    onFormatDocument: () -> Unit,
    onSaveDocument: () -> Unit,
    onOpenCommandPalette: () -> Unit,
    onToggleTerminal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVCodeColors.current

    if (activeTab == null) {
        Box(
            modifier = modifier.fillMaxSize().background(colors.editorBg),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Text("VCode Studio", color = colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text("Open a file from Explorer or press Ctrl+P to launch Command Palette", color = colors.textMuted, fontSize = 13.sp)
            }
        }
        return
    }

    var editorRef by remember { mutableStateOf<CodeEditor?>(null) }
    var canUndo by remember { mutableStateOf(false) }
    var canRedo by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var caseSensitive by remember { mutableStateOf(false) }
    var useRegex by remember { mutableStateOf(false) }
    var matchCountText by remember { mutableStateOf("") }

    var loadedPath by remember { mutableStateOf<String?>(null) }
    var loadedVersion by remember { mutableStateOf(-1L) }
    var suppressChangeCallback by remember { mutableStateOf(false) }
    var currentCursorLine by remember { mutableIntStateOf(activeTab.cursorLine) }

    val activeFilePathState = remember { mutableStateOf(activeTab.filePath) }
    activeFilePathState.value = activeTab.filePath

    LaunchedEffect(activeTab.filePath, activeTab.version) {
        val editor = editorRef ?: return@LaunchedEffect
        if (loadedPath != activeTab.filePath || loadedVersion != activeTab.version) {
            suppressChangeCallback = true
            val targetLine = (activeTab.cursorLine - 1).coerceAtLeast(0)
            val targetCol = (activeTab.cursorColumn - 1).coerceAtLeast(0)
            editor.setText(activeTab.content)
            val lang = TextMateSetup.createLanguage(
                mode = activeTab.language,
                autoComplete = settings.enabledExtensions.contains("ext.emmet.snippets"),
                tabSize = settings.tabSize
            )
            editor.setEditorLanguage(lang)
            runCatching {
                val safeLine = targetLine.coerceAtMost((editor.lineCount - 1).coerceAtLeast(0))
                val lineLen = editor.text.getColumnCount(safeLine)
                editor.setSelection(safeLine, targetCol.coerceAtMost(lineLen))
            }
            loadedPath = activeTab.filePath
            loadedVersion = activeTab.version
            canUndo = editor.canUndo()
            canRedo = editor.canRedo()
            suppressChangeCallback = false
        }
    }

    LaunchedEffect(activeTab.language, settings) {
        val editor = editorRef ?: return@LaunchedEffect
        val typeface = when (settings.fontFamily) {
            EditorFontFamily.MONOSPACE -> Typeface.MONOSPACE
            EditorFontFamily.SANS_SERIF -> Typeface.SANS_SERIF
            EditorFontFamily.SERIF -> Typeface.SERIF
        }
        editor.typefaceText = typeface
        editor.typefaceLineNumber = typeface
        editor.setTextSize(settings.fontSizeSp.toFloat())
        editor.isWordwrap = settings.wordWrap
        editor.isLineNumberEnabled = settings.showLineNumbers
        editor.setPinLineNumber(true)
        editor.isHighlightCurrentLine = true
        editor.isHighlightCurrentBlock = true
        editor.tabWidth = settings.tabSize
        editor.props.autoIndent = settings.autoIndent
        editor.props.symbolPairAutoCompletion = settings.bracketMatching
        editor.props.enableRoundTextBackground = true

        val scheme = TextMateSetup.applyTheme(settings.themeId)
        editor.colorScheme = scheme ?: EditorColorScheme()
    }

    LaunchedEffect(jumpToLineRequest) {
        val req = jumpToLineRequest ?: return@LaunchedEffect
        val editor = editorRef ?: return@LaunchedEffect
        if (req.first == activeTab.filePath) {
            val safeLine = (req.second - 1).coerceIn(0, (editor.lineCount - 1).coerceAtLeast(0))
            editor.jumpToLine(safeLine)
            onJumpHandled()
        }
    }

    LaunchedEffect(findQuery, caseSensitive, useRegex, showFindBar, activeTab.filePath) {
        val editor = editorRef ?: return@LaunchedEffect
        if (!showFindBar || findQuery.isBlank()) {
            editor.searcher.stopSearch()
            matchCountText = ""
        } else {
            runCatching {
                editor.searcher.search(findQuery, EditorSearcher.SearchOptions(caseSensitive, useRegex))
                val total = editor.searcher.matchedPositionCount
                val idx = editor.searcher.currentMatchedPositionIndex
                matchCountText = if (total > 0) "${idx + 1}/$total" else "0/0"
            }.onFailure { matchCountText = "err" }
        }
    }

    DisposableEffect(Unit) {
        onDispose { editorRef?.release() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.editorBg)
            .onPreviewKeyEvent { ev ->
                if (ev.nativeKeyEvent.action == KeyEvent.ACTION_DOWN && ev.isCtrlPressed) {
                    when (ev.key) {
                        Key.S -> { onSaveDocument(); true }
                        Key.P -> { onOpenCommandPalette(); true }
                        Key.Grave -> { onToggleTerminal(); true }
                        Key.Z -> { if (ev.isShiftPressed) editorRef?.redo() else editorRef?.undo(); true }
                        Key.Y -> { editorRef?.redo(); true }
                        else -> false
                    }
                } else false
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(colors.tabBarBg)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(activeTab.relativePath, color = colors.textMuted, fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1)
            IconButton(onClick = { editorRef?.undo() }, enabled = canUndo, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Undo, "Undo", tint = if (canUndo) colors.textPrimary else colors.textMuted.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = { editorRef?.redo() }, enabled = canRedo, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Redo, "Redo", tint = if (canRedo) colors.textPrimary else colors.textMuted.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onFormatDocument, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.FormatAlignLeft, "Format Document", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
            }
        }

        if (showFindBar) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.sideBarBg)
                    .border(0.5.dp, colors.border)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CompactSearchInput(findQuery, { findQuery = it }, "Find", Modifier.weight(1f))
                    if (matchCountText.isNotEmpty()) Text(matchCountText, color = colors.textMuted, fontSize = 11.sp)
                    ToggleChip("Aa", caseSensitive) { caseSensitive = !caseSensitive }
                    ToggleChip(".*", useRegex) { useRegex = !useRegex }
                    IconButton(onClick = {
                        editorRef?.searcher?.gotoPrevious()
                        val total = editorRef?.searcher?.matchedPositionCount ?: 0
                        val idx = editorRef?.searcher?.currentMatchedPositionIndex ?: 0
                        if (total > 0) matchCountText = "${idx + 1}/$total"
                    }, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.ArrowUpward, "Prev", tint = colors.textPrimary, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = {
                        editorRef?.searcher?.gotoNext()
                        val total = editorRef?.searcher?.matchedPositionCount ?: 0
                        val idx = editorRef?.searcher?.currentMatchedPositionIndex ?: 0
                        if (total > 0) matchCountText = "${idx + 1}/$total"
                    }, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.ArrowDownward, "Next", tint = colors.textPrimary, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onCloseFindBar, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.Close, "Close", tint = colors.textMuted, modifier = Modifier.size(15.dp))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CompactSearchInput(replaceQuery, { replaceQuery = it }, "Replace", Modifier.weight(1f))
                    Text(
                        text = "Replace",
                        color = colors.textPrimary,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .background(colors.selectionBg, RoundedCornerShape(4.dp))
                            .clickable { runCatching { editorRef?.searcher?.replaceCurrentMatch(replaceQuery) } }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Text(
                        text = "All",
                        color = colors.textPrimary,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .background(colors.selectionBg, RoundedCornerShape(4.dp))
                            .clickable { runCatching { editorRef?.searcher?.replaceAll(replaceQuery) } }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                factory = { ctx ->
                    CodeEditor(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        isFocusableInTouchMode = true
                        isFocusable = true
                        setTextSize(settings.fontSizeSp.toFloat())
                        subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
                            canUndo = this.canUndo()
                            canRedo = this.canRedo()
                            if (!suppressChangeCallback) {
                                onContentChanged(activeFilePathState.value, this.text.toString())
                            }
                        }
                        subscribeEvent(SelectionChangeEvent::class.java) { event, _ ->
                            val line = event.left.line + 1
                            val col = event.left.column + 1
                            currentCursorLine = line
                            onCursorChanged(activeFilePathState.value, line, col)
                        }
                        editorRef = this
                    }
                }
            )

            if (settings.showMinimap) {
                CodeMinimapRail(
                    content = activeTab.content,
                    currentLine = currentCursorLine,
                    onJumpToLine = { lineIndex -> editorRef?.jumpToLine(lineIndex) },
                    modifier = Modifier.width(44.dp).fillMaxHeight()
                )
            }
        }

        if (settings.showExtraKeyBar) {
            ExtraCodingSymbolBar { symbol ->
                editorRef?.let { ed ->
                    if (symbol == "TAB") ed.insertText(" ".repeat(settings.tabSize), settings.tabSize)
                    else ed.insertText(symbol, symbol.length)
                }
            }
        }
    }
}

@Composable
private fun CodeMinimapRail(
    content: String,
    currentLine: Int,
    onJumpToLine: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVCodeColors.current
    val lines = remember(content) { content.lines() }
    Canvas(
        modifier = modifier
            .background(colors.editorBg)
            .border(0.5.dp, colors.border)
            .pointerInput(lines.size) {
                detectTapGestures { offset ->
                    if (lines.isNotEmpty() && size.height > 0) {
                        val ratio = (offset.y / size.height).coerceIn(0f, 1f)
                        onJumpToLine((ratio * (lines.size - 1)).toInt())
                    }
                }
            }
    ) {
        val total = lines.size.coerceAtLeast(1)
        val step = (size.height / total.coerceAtLeast(40)).coerceIn(2f, 4f)
        val maxRenderLines = (size.height / step).toInt().coerceAtLeast(1)
        val stride = (total.toFloat() / maxRenderLines).coerceAtLeast(1f)
        var y = 0f; var idx = 0f
        while (idx < total && y < size.height) {
            val lineText = lines[idx.toInt()]
            val trimmed = lineText.trim()
            if (trimmed.isNotEmpty()) {
                val indentRatio = (lineText.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0) / 40f).coerceIn(0f, 0.4f)
                val lenRatio = (trimmed.length / 80f).coerceIn(0.08f, 0.9f - indentRatio)
                val barColor = when {
                    trimmed.startsWith("//") || trimmed.startsWith("#") || trimmed.startsWith("<!--") -> colors.success.copy(alpha = 0.45f)
                    trimmed.startsWith("import") || trimmed.startsWith("fun ") || trimmed.startsWith("function") || trimmed.startsWith("class ") -> colors.accent.copy(alpha = 0.6f)
                    else -> colors.textMuted.copy(alpha = 0.45f)
                }
                drawRect(barColor, Offset(size.width * indentRatio + 3f, y), Size(size.width * lenRatio, (step - 0.6f).coerceAtLeast(1f)))
            }
            y += step; idx += stride
        }
        val activeRatio = ((currentLine - 1).toFloat() / total).coerceIn(0f, 1f)
        drawRect(colors.accent.copy(alpha = 0.25f), Offset(0f, activeRatio * (size.height - 14f)), Size(size.width, 14f))
    }
}

@Composable
private fun ExtraCodingSymbolBar(onInsert: (String) -> Unit) {
    val colors = LocalVCodeColors.current
    val symbols = listOf("TAB", "{", "}", "(", ")", "[", "]", "<", ">", "=", ";", ":", "\"", "'", "/", "\\", "|", "&", "!", "$", "_")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(colors.sideBarBg)
            .border(0.5.dp, colors.border)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        symbols.forEach { sym ->
            Box(
                modifier = Modifier
                    .background(colors.tabInactiveBg, RoundedCornerShape(4.dp))
                    .clickable { onInsert(sym) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(sym, color = colors.textPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun CompactSearchInput(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val colors = LocalVCodeColors.current
    Box(
        modifier = modifier
            .height(28.dp)
            .background(colors.editorBg, RoundedCornerShape(4.dp))
            .border(0.5.dp, colors.border, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) Text(placeholder, color = colors.textMuted, fontSize = 12.sp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = colors.textPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ToggleChip(label: String, active: Boolean, onClick: () -> Unit) {
    val colors = LocalVCodeColors.current
    Box(
        modifier = Modifier
            .background(if (active) colors.accent.copy(alpha = 0.25f) else Color.Transparent, RoundedCornerShape(4.dp))
            .border(0.5.dp, if (active) colors.accent else colors.border, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(label, color = if (active) colors.accent else colors.textMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}
