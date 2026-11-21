package com.vcode.ide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vcode.ide.data.project.TemplateProvider
import com.vcode.ide.ui.theme.LocalVCodeColors

@Composable
fun ExtensionsPane(enabledExtensions: Set<String>, onToggleExtension: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalVCodeColors.current
    Column(modifier = modifier.fillMaxSize().background(colors.sideBarBg)) {
        Box(modifier = Modifier.fillMaxWidth().height(36.dp).background(colors.sideBarHeaderBg).padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
            Text("EXTENSIONS", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        }
        LazyColumn(modifier = Modifier.weight(1f).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(TemplateProvider.builtInExtensions, key = { it.id }) { ext ->
                val enabled = ext.id in enabledExtensions
                Column(
                    modifier = Modifier.fillMaxWidth().background(colors.editorBg, RoundedCornerShape(6.dp)).border(0.5.dp, colors.border, RoundedCornerShape(6.dp)).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ext.name, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("${ext.publisher} • v${ext.version} • ${ext.category}", color = colors.accent, fontSize = 10.sp)
                        }
                        Switch(checked = enabled, onCheckedChange = { onToggleExtension(ext.id) })
                    }
                    Text(ext.description, color = colors.textMuted, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }
        }
    }
}
