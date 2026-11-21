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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vcode.ide.domain.model.AppSettings
import com.vcode.ide.domain.model.EditorFontFamily
import com.vcode.ide.domain.model.EditorThemeId
import com.vcode.ide.ui.theme.LocalVCodeColors

@Composable
fun SettingsPane(settings: AppSettings, onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalVCodeColors.current
    val accentChoices = listOf(0xFF007ACC, 0xFF38BDF8, 0xFF8B5CF6, 0xFF10B981, 0xFFF59E0B, 0xFFEF4444)

    Column(modifier = modifier.fillMaxSize().background(colors.sideBarBg)) {
        Box(modifier = Modifier.fillMaxWidth().height(36.dp).background(colors.sideBarHeaderBg).padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
            Text("SETTINGS", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        }
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Color Theme", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                EditorThemeId.entries.forEach { theme ->
                    val selected = settings.themeId == theme
                    Row(
                        modifier = Modifier.fillMaxWidth().background(if (selected) colors.selectionBg else colors.editorBg, RoundedCornerShape(4.dp)).border(0.5.dp, if (selected) colors.accent else colors.border, RoundedCornerShape(4.dp)).clickable { onUpdateSettings { it.copy(themeId = theme) } }.padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(theme.displayName, color = colors.textPrimary, fontSize = 12.sp)
                        Text(if (theme.isDark) "Dark" else "Light", color = colors.textMuted, fontSize = 10.sp)
                    }
                }
            }
            Text("Accent Color", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                accentChoices.forEach { hex ->
                    Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(Color(hex)).border(if (settings.accentColorHex == hex) 2.dp else 0.dp, Color.White, CircleShape).clickable { onUpdateSettings { it.copy(accentColorHex = hex) } })
                }
            }
            Text("Editor Font", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EditorFontFamily.entries.forEach { font ->
                    val selected = settings.fontFamily == font
                    Box(modifier = Modifier.background(if (selected) colors.accent.copy(alpha = 0.25f) else colors.editorBg, RoundedCornerShape(4.dp)).border(0.5.dp, if (selected) colors.accent else colors.border, RoundedCornerShape(4.dp)).clickable { onUpdateSettings { it.copy(fontFamily = font) } }.padding(horizontal = 8.dp, vertical = 5.dp)) {
                        Text(font.displayName.substringBefore(" "), color = colors.textPrimary, fontSize = 11.sp)
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Font Size: ${settings.fontSizeSp}sp", color = colors.textPrimary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallStepBtn("-") { onUpdateSettings { it.copy(fontSizeSp = (it.fontSizeSp - 1).coerceAtLeast(10)) } }
                    SmallStepBtn("+") { onUpdateSettings { it.copy(fontSizeSp = (it.fontSizeSp + 1).coerceAtMost(24)) } }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Tab Size: ${settings.tabSize} spaces", color = colors.textPrimary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallStepBtn("2") { onUpdateSettings { it.copy(tabSize = 2) } }
                    SmallStepBtn("4") { onUpdateSettings { it.copy(tabSize = 4) } }
                }
            }
            SettingSwitch("Word Wrap", settings.wordWrap) { v -> onUpdateSettings { it.copy(wordWrap = v) } }
            SettingSwitch("Line Numbers", settings.showLineNumbers) { v -> onUpdateSettings { it.copy(showLineNumbers = v) } }
            SettingSwitch("Code Minimap", settings.showMinimap) { v -> onUpdateSettings { it.copy(showMinimap = v) } }
            SettingSwitch("Auto Save", settings.autoSave) { v -> onUpdateSettings { it.copy(autoSave = v) } }
            SettingSwitch("Auto Indent", settings.autoIndent) { v -> onUpdateSettings { it.copy(autoIndent = v) } }
            SettingSwitch("Bracket Matching", settings.bracketMatching) { v -> onUpdateSettings { it.copy(bracketMatching = v) } }
            SettingSwitch("Extra Symbol Toolbar", settings.showExtraKeyBar) { v -> onUpdateSettings { it.copy(showExtraKeyBar = v) } }
        }
    }
}

@Composable
private fun SmallStepBtn(label: String, onClick: () -> Unit) {
    val colors = LocalVCodeColors.current
    Box(modifier = Modifier.background(colors.editorBg, RoundedCornerShape(4.dp)).border(0.5.dp, colors.border, RoundedCornerShape(4.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(label, color = colors.textPrimary, fontSize = 12.sp)
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalVCodeColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = colors.textPrimary, fontSize = 12.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
