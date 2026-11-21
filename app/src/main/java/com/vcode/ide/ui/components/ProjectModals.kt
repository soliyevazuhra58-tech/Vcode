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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vcode.ide.data.project.TemplateProvider
import com.vcode.ide.domain.model.LanguageMode
import com.vcode.ide.domain.model.ProjectTemplate
import com.vcode.ide.domain.model.VCodeProject
import com.vcode.ide.ui.theme.LocalVCodeColors

@Composable
fun NewProjectDialog(onDismiss: () -> Unit, onCreate: (name: String, template: ProjectTemplate) -> Unit) {
    val colors = LocalVCodeColors.current
    var projectName by remember { mutableStateOf("MyVCodeApp") }
    var selectedTemplate by remember { mutableStateOf(TemplateProvider.templates.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Project from Template") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = projectName, onValueChange = { projectName = it }, label = { Text("Project Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Select Template", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                LazyColumn(modifier = Modifier.heightIn(max = 240.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TemplateProvider.templates) { tpl ->
                        val selected = tpl.id == selectedTemplate.id
                        Row(
                            modifier = Modifier.fillMaxWidth().background(if (selected) colors.selectionBg else colors.editorBg, RoundedCornerShape(6.dp)).border(0.5.dp, if (selected) colors.accent else colors.border, RoundedCornerShape(6.dp)).clickable { selectedTemplate = tpl }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.background(colors.accent.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(tpl.badge, color = colors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text(tpl.name, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(tpl.subtitle, color = colors.textMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onCreate(projectName.ifBlank { "NewProject" }, selectedTemplate) }) { Text("Create Project") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun RecentProjectsDialog(
    projects: List<VCodeProject>,
    activeProjectId: String?,
    onSelectProject: (VCodeProject) -> Unit,
    onDeleteProject: (VCodeProject) -> Unit,
    onNewProject: () -> Unit,
    onImportFolder: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalVCodeColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Projects & Workspaces") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("+ New Project", color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.background(colors.accent.copy(alpha = 0.15f), RoundedCornerShape(4.dp)).clickable { onDismiss(); onNewProject() }.padding(horizontal = 10.dp, vertical = 6.dp))
                    Text("Import Folder", color = colors.textPrimary, fontSize = 12.sp, modifier = Modifier.background(colors.tabInactiveBg, RoundedCornerShape(4.dp)).clickable { onDismiss(); onImportFolder() }.padding(horizontal = 10.dp, vertical = 6.dp))
                }
                LazyColumn(modifier = Modifier.heightIn(max = 260.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(projects, key = { it.id }) { proj ->
                        val active = proj.id == activeProjectId
                        Row(
                            modifier = Modifier.fillMaxWidth().background(if (active) colors.selectionBg else colors.editorBg, RoundedCornerShape(6.dp)).border(0.5.dp, if (active) colors.accent else colors.border, RoundedCornerShape(6.dp)).clickable { onDismiss(); onSelectProject(proj) }.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(proj.name, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(proj.templateId, color = colors.textMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                            if (projects.size > 1) {
                                IconButton(onClick = { onDeleteProject(proj) }, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.DeleteOutline, "Delete", tint = colors.error, modifier = Modifier.size(16.dp)) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun LanguagePickerDialog(currentLanguage: LanguageMode, onSelect: (LanguageMode) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalVCodeColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Language Mode") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(LanguageMode.entries) { mode ->
                    Row(
                        modifier = Modifier.fillMaxWidth().background(if (mode == currentLanguage) colors.selectionBg else colors.editorBg, RoundedCornerShape(4.dp)).clickable { onSelect(mode) }.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(mode.displayName, color = colors.textPrimary, fontSize = 13.sp)
                        Text(mode.badge, color = mode.badgeColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
