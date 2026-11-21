package com.vcode.ide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vcode.ide.domain.model.EditorTab
import com.vcode.ide.domain.model.ProblemItem
import com.vcode.ide.domain.model.ProblemSeverity
import com.vcode.ide.ui.theme.LocalVCodeColors

@Composable
fun StatusBar(
    branchName: String,
    gitInitialized: Boolean,
    problems: List<ProblemItem>,
    activeTab: EditorTab?,
    tabSize: Int,
    autoSaveStatus: String,
    onClickBranch: () -> Unit,
    onClickProblems: () -> Unit,
    onClickLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVCodeColors.current
    val errorCount = problems.count { it.severity == ProblemSeverity.ERROR }
    val warnCount = problems.count { it.severity == ProblemSeverity.WARNING }

    Row(
        modifier = modifier.fillMaxWidth().height(24.dp).background(colors.statusBarBg).horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.clickable(onClick = onClickBranch), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.AccountTree, null, tint = colors.statusBarFg, modifier = Modifier.size(13.dp))
            Text(if (gitInitialized) branchName else "no-git", color = colors.statusBarFg, fontSize = 11.sp)
        }
        Row(modifier = Modifier.clickable(onClick = onClickProblems), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Icon(Icons.Default.ErrorOutline, null, tint = colors.statusBarFg, modifier = Modifier.size(12.dp))
                Text("$errorCount", color = colors.statusBarFg, fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Icon(Icons.Default.WarningAmber, null, tint = colors.statusBarFg, modifier = Modifier.size(12.dp))
                Text("$warnCount", color = colors.statusBarFg, fontSize = 11.sp)
            }
        }
        Spacer(modifier = Modifier.weight(1f, fill = false))
        if (activeTab != null) {
            Text("Ln ${activeTab.cursorLine}, Col ${activeTab.cursorColumn}", color = colors.statusBarFg, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text("Spaces: $tabSize", color = colors.statusBarFg, fontSize = 11.sp)
            Text("UTF-8", color = colors.statusBarFg, fontSize = 11.sp)
            Text(activeTab.language.displayName, color = colors.statusBarFg, fontSize = 11.sp, modifier = Modifier.clickable(onClick = onClickLanguage))
        }
        Text(autoSaveStatus, color = colors.statusBarFg.copy(alpha = 0.9f), fontSize = 11.sp)
    }
}
