package com.vcode.ide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vcode.ide.data.terminal.TerminalSessionUi
import com.vcode.ide.domain.model.BottomPanelTab
import com.vcode.ide.domain.model.OutputEntry
import com.vcode.ide.domain.model.ProblemItem
import com.vcode.ide.domain.model.ProblemSeverity
import com.vcode.ide.ui.theme.LocalVCodeColors

@Composable
fun BottomPanel(
    activeTab: BottomPanelTab,
    heightDp: Float,
    sessions: List<TerminalSessionUi>,
    activeSessionId: String?,
    outputLogs: List<OutputEntry>,
    problems: List<ProblemItem>,
    onSelectTab: (BottomPanelTab) -> Unit,
    onClosePanel: () -> Unit,
    onResizeDelta: (Float) -> Unit,
    onExecuteCommand: (String) -> Unit,
    onCreateTerminalSession: () -> Unit,
    onSelectTerminalSession: (String) -> Unit,
    onCloseTerminalSession: (String) -> Unit,
    onClearTerminal: () -> Unit,
    onSendCtrlC: () -> Unit,
    onClearOutput: () -> Unit,
    onProblemClick: (ProblemItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVCodeColors.current
    val density = LocalDensity.current

    Column(modifier = modifier.fillMaxWidth().height(heightDp.dp).background(colors.panelBg).border(0.5.dp, colors.border)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(8.dp).background(colors.panelBg).pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount -> onResizeDelta(with(density) { (-dragAmount).toDp().value }) }
            },
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.width(42.dp).height(3.dp).background(colors.border, RoundedCornerShape(2.dp)))
        }

        Row(modifier = Modifier.fillMaxWidth().height(32.dp).background(colors.sideBarBg).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            BottomPanelTab.entries.forEach { tab ->
                val selected = activeTab == tab
                val badgeCount = when (tab) {
                    BottomPanelTab.PROBLEMS -> problems.size
                    BottomPanelTab.TERMINAL -> sessions.size
                    else -> 0
                }
                Row(modifier = Modifier.clickable { onSelectTab(tab) }.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(tab.title.uppercase(), color = if (selected) colors.textPrimary else colors.textMuted, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                    if (badgeCount > 0) {
                        Box(
                            modifier = Modifier.background(if (tab == BottomPanelTab.PROBLEMS && problems.any { it.severity == ProblemSeverity.ERROR }) colors.error.copy(alpha = 0.25f) else colors.selectionBg, RoundedCornerShape(8.dp)).padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(badgeCount.toString(), color = colors.textPrimary, fontSize = 9.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            if (activeTab == BottomPanelTab.TERMINAL) {
                IconButton(onClick = onCreateTerminalSession, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.Add, "New Terminal", tint = colors.textSecondary, modifier = Modifier.size(16.dp)) }
                IconButton(onClick = onClearTerminal, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.DeleteSweep, "Clear", tint = colors.textSecondary, modifier = Modifier.size(16.dp)) }
            } else if (activeTab == BottomPanelTab.OUTPUT) {
                IconButton(onClick = onClearOutput, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.DeleteSweep, "Clear Output", tint = colors.textSecondary, modifier = Modifier.size(16.dp)) }
            }
            IconButton(onClick = onClosePanel, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.Close, "Close", tint = colors.textMuted, modifier = Modifier.size(16.dp)) }
        }

        when (activeTab) {
            BottomPanelTab.TERMINAL -> TerminalView(sessions, sessions.firstOrNull { it.id == activeSessionId } ?: sessions.firstOrNull(), onSelectTerminalSession, onCloseTerminalSession, onExecuteCommand, onSendCtrlC, Modifier.weight(1f))
            BottomPanelTab.OUTPUT -> OutputView(outputLogs, Modifier.weight(1f))
            BottomPanelTab.PROBLEMS -> ProblemsView(problems, onProblemClick, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TerminalView(
    sessions: List<TerminalSessionUi>,
    activeSession: TerminalSessionUi?,
    onSelectSession: (String) -> Unit,
    onCloseSession: (String) -> Unit,
    onExecuteCommand: (String) -> Unit,
    onSendCtrlC: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVCodeColors.current
    var commandInput by remember { mutableStateOf("") }
    var historyCursor by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()
    val lines = activeSession?.lines ?: emptyList()

    LaunchedEffect(lines.size, activeSession?.id) {
        if (lines.isNotEmpty()) listState.scrollToItem(lines.lastIndex)
    }

    Column(modifier = modifier.fillMaxSize().background(Color(0xFF0E1014))) {
        if (sessions.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).background(Color(0xFF161920)).padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                sessions.forEach { sess ->
                    val isSel = sess.id == activeSession?.id
                    Row(
                        modifier = Modifier.background(if (isSel) Color(0xFF262B36) else Color.Transparent, RoundedCornerShape(4.dp)).clickable { onSelectSession(sess.id) }.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(sess.title, color = if (isSel) Color.White else Color(0xFF9CA3AF), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        if (sessions.size > 1) {
                            Icon(Icons.Default.Close, "Close", tint = Color(0xFF9CA3AF), modifier = Modifier.size(12.dp).clickable { onCloseSession(sess.id) })
                        }
                    }
                }
            }
        }
        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            items(lines) { raw -> Text(parseAnsiLine(raw), fontSize = 12.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp) }
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF151820)).border(0.5.dp, colors.border).padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val folderTag = activeSession?.workingDir?.substringAfterLast('/')?.ifBlank { "/" } ?: "~"
            Text("$folderTag $", color = Color(0xFF4ADE80), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.weight(1f)) {
                if (commandInput.isEmpty()) Text(if (activeSession?.isRunningProcess == true) "Send stdin..." else "ls, pwd, git status, node main.js...", color = Color(0xFF6B7280), fontSize = 12.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicTextField(
                    value = commandInput, onValueChange = { commandInput = it }, singleLine = true,
                    textStyle = TextStyle(color = Color(0xFFF3F4F6), fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(Color(0xFF38BDF8)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onExecuteCommand(commandInput); commandInput = ""; historyCursor = -1 }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            IconButton(onClick = {
                val hist = activeSession?.history ?: return@IconButton
                if (hist.isEmpty()) return@IconButton
                val nextIdx = if (historyCursor == -1) hist.lastIndex else (historyCursor - 1).coerceAtLeast(0)
                historyCursor = nextIdx; commandInput = hist[nextIdx]
            }, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.ArrowUpward, "History Up", tint = Color(0xFF9CA3AF), modifier = Modifier.size(15.dp)) }
            IconButton(onClick = {
                val hist = activeSession?.history ?: return@IconButton
                if (historyCursor == -1) return@IconButton
                if (historyCursor >= hist.lastIndex) { historyCursor = -1; commandInput = "" }
                else { historyCursor += 1; commandInput = hist[historyCursor] }
            }, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.ArrowDownward, "History Down", tint = Color(0xFF9CA3AF), modifier = Modifier.size(15.dp)) }
            if (activeSession?.isRunningProcess == true) {
                IconButton(onClick = onSendCtrlC, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.Stop, "Ctrl+C", tint = Color(0xFFF87171), modifier = Modifier.size(16.dp)) }
            }
            IconButton(onClick = { onExecuteCommand(commandInput); commandInput = ""; historyCursor = -1 }, modifier = Modifier.size(26.dp)) {
                Icon(Icons.AutoMirrored.Filled.Send, "Run", tint = Color(0xFF38BDF8), modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
private fun OutputView(logs: List<OutputEntry>, modifier: Modifier = Modifier) {
    val colors = LocalVCodeColors.current
    if (logs.isEmpty()) { Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No output logs yet.", color = colors.textMuted, fontSize = 12.sp) }; return }
    LazyColumn(modifier = modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(logs, key = { it.id }) { entry ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("[${entry.tag}]", color = if (entry.isError) colors.error else colors.accent, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text(entry.message, color = if (entry.isError) colors.error else colors.textPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun ProblemsView(problems: List<ProblemItem>, onProblemClick: (ProblemItem) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalVCodeColors.current
    if (problems.isEmpty()) { Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No problems detected in open files.", color = colors.success, fontSize = 12.sp) }; return }
    LazyColumn(modifier = modifier.fillMaxSize().padding(vertical = 4.dp)) {
        items(problems) { problem ->
            Row(modifier = Modifier.fillMaxWidth().clickable { onProblemClick(problem) }.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val (icon, tint) = when (problem.severity) {
                    ProblemSeverity.ERROR -> Icons.Default.Error to colors.error
                    ProblemSeverity.WARNING -> Icons.Default.Warning to colors.warning
                    ProblemSeverity.INFO -> Icons.Default.Info to colors.accent
                }
                Icon(icon, null, tint = tint, modifier = Modifier.size(15.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(problem.message, color = colors.textPrimary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${problem.fileName} [${problem.line}:${problem.column}] • ${problem.source}", color = colors.textMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

private fun parseAnsiLine(raw: String): AnnotatedString {
    val regex = Regex("\u001B\\[([0-9;]+)m")
    return buildAnnotatedString {
        var currentColor = Color(0xFFE5E7EB)
        var lastIndex = 0
        regex.findAll(raw).forEach { match ->
            if (match.range.first > lastIndex) { pushStyle(SpanStyle(color = currentColor)); append(raw.substring(lastIndex, match.range.first)); pop() }
            val code = match.groupValues[1].split(";").lastOrNull()?.toIntOrNull() ?: 0
            currentColor = when (code) {
                0 -> Color(0xFFE5E7EB); 31, 91 -> Color(0xFFF87171); 32, 92 -> Color(0xFF4ADE80)
                33, 93 -> Color(0xFFFACC15); 34, 94 -> Color(0xFF60A5FA); 35, 95 -> Color(0xFFC084FC)
                36, 96 -> Color(0xFF22D3EE); 90 -> Color(0xFF9CA3AF); else -> currentColor
            }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < raw.length) { pushStyle(SpanStyle(color = currentColor)); append(raw.substring(lastIndex)); pop() }
    }
}
