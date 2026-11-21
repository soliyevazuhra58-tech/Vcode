package com.vcode.ide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vcode.ide.domain.model.CommandItem
import com.vcode.ide.domain.model.FileNode
import com.vcode.ide.ui.theme.LocalVCodeColors

@Composable
fun CommandPaletteDialog(commands: List<CommandItem>, projectFiles: List<FileNode>, onOpenFile: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalVCodeColors.current
    var query by remember { mutableStateOf(">") }
    val isCommandMode = query.startsWith(">")
    val filter = query.removePrefix(">").trim()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().background(colors.sideBarBg, RoundedCornerShape(8.dp)).border(1.dp, colors.border, RoundedCornerShape(8.dp)).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().background(colors.editorBg, RoundedCornerShape(4.dp)).border(1.dp, colors.accent, RoundedCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = 8.dp)) {
                if (query.isEmpty()) Text("Type '>' for commands or filename to open...", color = colors.textMuted, fontSize = 13.sp)
                BasicTextField(value = query, onValueChange = { query = it }, singleLine = true, textStyle = TextStyle(color = colors.textPrimary, fontSize = 13.sp), cursorBrush = SolidColor(colors.accent), modifier = Modifier.fillMaxWidth())
            }
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                if (isCommandMode) {
                    val filtered = commands.filter { filter.isEmpty() || it.title.contains(filter, true) || it.subtitle.contains(filter, true) }
                    items(filtered, key = { it.id }) { cmd ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { onDismiss(); cmd.action() }.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cmd.title, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(cmd.subtitle, color = colors.textMuted, fontSize = 11.sp)
                            }
                            if (cmd.shortcut.isNotEmpty()) Text(cmd.shortcut, color = colors.accent, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                } else {
                    val filteredFiles = projectFiles.filter { !it.isDirectory && (filter.isEmpty() || it.relativePath.contains(filter, true)) }
                    items(filteredFiles, key = { it.path }) { node ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { onDismiss(); onOpenFile(node.path) }.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FileExtensionBadge(node.name)
                            Column {
                                Text(node.name, color = colors.textPrimary, fontSize = 13.sp)
                                Text(node.relativePath, color = colors.textMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
