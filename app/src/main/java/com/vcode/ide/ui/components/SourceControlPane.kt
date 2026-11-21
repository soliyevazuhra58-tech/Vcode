package com.vcode.ide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vcode.ide.domain.model.GitState
import com.vcode.ide.ui.theme.LocalVCodeColors

@Composable
fun SourceControlPane(
    gitState: GitState,
    onInitRepo: () -> Unit,
    onRefresh: () -> Unit,
    onStageFile: (String) -> Unit,
    onUnstageFile: (String) -> Unit,
    onStageAll: () -> Unit,
    onCommit: (String) -> Unit,
    onSelectDiff: (String) -> Unit,
    onCreateBranch: (String) -> Unit,
    onCheckoutBranch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVCodeColors.current
    var commitMessage by remember { mutableStateOf("") }
    var showBranchDialog by remember { mutableStateOf(false) }
    var branchMenuOpen by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(colors.sideBarBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(36.dp).background(colors.sideBarHeaderBg).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SOURCE CONTROL", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, modifier = Modifier.weight(1f))
            if (gitState.isInitialized) {
                IconButton(onClick = onRefresh, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Refresh, "Refresh Git", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }

        if (!gitState.isInitialized) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("This workspace does not have a Git repository yet.", color = colors.textSecondary, fontSize = 12.sp)
                Button(onClick = onInitRepo, colors = ButtonDefaults.buttonColors(containerColor = colors.accent), shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Initialize Repository (JGit)", fontSize = 12.sp)
                }
            }
            return@Column
        }

        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier.weight(1f).background(colors.editorBg, RoundedCornerShape(4.dp)).border(0.5.dp, colors.border, RoundedCornerShape(4.dp)).clickable { branchMenuOpen = true }.padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("Branch: ${gitState.currentBranch}", color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    DropdownMenu(expanded = branchMenuOpen, onDismissRequest = { branchMenuOpen = false }) {
                        gitState.branches.forEach { b ->
                            DropdownMenuItem(text = { Text(b) }, onClick = { branchMenuOpen = false; onCheckoutBranch(b) })
                        }
                    }
                }
                Box(
                    modifier = Modifier.background(colors.tabInactiveBg, RoundedCornerShape(4.dp)).clickable { showBranchDialog = true }.padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("+ Branch", color = colors.textPrimary, fontSize = 11.sp)
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth().height(54.dp).background(colors.editorBg, RoundedCornerShape(4.dp)).border(0.5.dp, colors.border, RoundedCornerShape(4.dp)).padding(8.dp)
            ) {
                if (commitMessage.isEmpty()) Text("Commit message...", color = colors.textMuted, fontSize = 12.sp)
                BasicTextField(value = commitMessage, onValueChange = { commitMessage = it }, textStyle = TextStyle(color = colors.textPrimary, fontSize = 12.sp), cursorBrush = SolidColor(colors.accent), modifier = Modifier.fillMaxSize())
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { onCommit(commitMessage); commitMessage = "" },
                    enabled = gitState.changes.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Commit", fontSize = 12.sp)
                }
                Button(
                    onClick = onStageAll,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.tabInactiveBg),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Stage All", color = colors.textPrimary, fontSize = 11.sp)
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            item { Text("CHANGES (${gitState.changes.size})", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) }
            items(gitState.changes) { ch ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onSelectDiff(ch.relativePath) }.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(ch.relativePath, color = colors.textPrimary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(if (ch.isStaged) "S" else ch.status.take(1), color = if (ch.isStaged) colors.success else colors.warning, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(horizontal = 6.dp))
                    IconButton(onClick = { if (ch.isStaged) onUnstageFile(ch.relativePath) else onStageFile(ch.relativePath) }, modifier = Modifier.size(22.dp)) {
                        Icon(if (ch.isStaged) Icons.Default.Remove else Icons.Default.Add, "Toggle Stage", tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                    }
                }
            }
            if (gitState.selectedDiff.isNotBlank()) {
                item {
                    Text("DIFF PREVIEW", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).background(colors.editorBg, RoundedCornerShape(4.dp)).border(0.5.dp, colors.border, RoundedCornerShape(4.dp)).horizontalScroll(rememberScrollState()).padding(8.dp)) {
                        Text(gitState.selectedDiff, color = colors.textSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            if (gitState.commits.isNotEmpty()) {
                item { Text("COMMITS (${gitState.commits.size})", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 4.dp)) }
                items(gitState.commits) { c ->
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(c.shortId, color = colors.warning, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text(c.message, color = colors.textPrimary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    if (showBranchDialog) {
        NamePromptDialog("Create Git Branch", "", "feature/responsive-layout", "Create & Switch", { showBranchDialog = false }) { name ->
            if (name.isNotBlank()) onCreateBranch(name); showBranchDialog = false
        }
    }
}
