package com.vcode.ide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vcode.ide.domain.model.SearchMatch
import com.vcode.ide.ui.theme.LocalVCodeColors

@Composable
fun SearchPane(
    query: String,
    replaceQuery: String,
    caseSensitive: Boolean,
    useRegex: Boolean,
    matches: List<SearchMatch>,
    onQueryChange: (String) -> Unit,
    onReplaceQueryChange: (String) -> Unit,
    onToggleCase: () -> Unit,
    onToggleRegex: () -> Unit,
    onReplaceAll: () -> Unit,
    onMatchClick: (SearchMatch) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVCodeColors.current
    Column(modifier = modifier.fillMaxSize().background(colors.sideBarBg)) {
        Box(modifier = Modifier.fillMaxWidth().height(36.dp).background(colors.sideBarHeaderBg).padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
            Text("SEARCH", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        }
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchFieldBox(query, onQueryChange, "Search in workspace...")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterPill("Aa Match Case", caseSensitive, onToggleCase)
                FilterPill(".* Regex", useRegex, onToggleRegex)
            }
            SearchFieldBox(replaceQuery, onReplaceQueryChange, "Replace with...")
            Button(
                onClick = onReplaceAll,
                enabled = query.isNotBlank() && matches.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth().height(34.dp)
            ) {
                Text("Replace All (${matches.size})", fontSize = 12.sp)
            }
        }
        Text("${matches.size} result(s)", color = colors.textMuted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(matches) { match ->
                Column(modifier = Modifier.fillMaxWidth().clickable { onMatchClick(match) }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(match.relativePath, color = colors.accent, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(":${match.line}", color = colors.textMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Text(match.linePreview, color = colors.textPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun SearchFieldBox(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    val colors = LocalVCodeColors.current
    Box(
        modifier = Modifier.fillMaxWidth().height(32.dp).background(colors.editorBg, RoundedCornerShape(4.dp)).border(0.5.dp, colors.border, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) Text(placeholder, color = colors.textMuted, fontSize = 12.sp)
        BasicTextField(value = value, onValueChange = onValueChange, singleLine = true, textStyle = TextStyle(color = colors.textPrimary, fontSize = 12.sp), cursorBrush = SolidColor(colors.accent), modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun FilterPill(label: String, active: Boolean, onClick: () -> Unit) {
    val colors = LocalVCodeColors.current
    Box(
        modifier = Modifier.background(if (active) colors.accent.copy(alpha = 0.2f) else colors.editorBg, RoundedCornerShape(4.dp)).border(0.5.dp, if (active) colors.accent else colors.border, RoundedCornerShape(4.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, color = if (active) colors.accent else colors.textMuted, fontSize = 11.sp)
    }
}
