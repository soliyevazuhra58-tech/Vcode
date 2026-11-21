package com.vcode.ide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DriveFolderUpload
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vcode.ide.domain.model.ClipboardMode
import com.vcode.ide.domain.model.FileClipboard
import com.vcode.ide.domain.model.FileNode
import com.vcode.ide.domain.model.LanguageMode
import com.vcode.ide.domain.model.VCodeProject
import com.vcode.ide.ui.theme.LocalVCodeColors

@Composable
fun FileExplorerPane(
    project: VCodeProject?,
    fileTree: List<FileNode>,
    activeFilePath: String?,
    clipboard: FileClipboard?,
    onToggleFolder: (String) -> Unit,
    onOpenFile: (String) -> Unit,
    onCreateFile: (parentDir: String, name: String) -> Unit,
    onCreateFolder: (parentDir: String, name: String) -> Unit,
    onRenameNode: (path: String, newName: String) -> Unit,
    onDeleteNode: (path: String) -> Unit,
    onCopyNode: (FileNode, ClipboardMode) -> Unit,
    onPasteInto: (targetDirPath: String) -> Unit,
    onImportFileInto: (targetDirPath: String) -> Unit,
    onImportFolder: () -> Unit,
    onExportFile: (FileNode) -> Unit,
    onExportProjectZip: () -> Unit,
    onOpenNewProject: () -> Unit,
    onOpenRecentProjects: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVCodeColors.current
    var showCreateFileForDir by remember { mutableStateOf<String?>(null) }
    var showCreateFolderForDir by remember { mutableStateOf<String?>(null) }
    var renameTarget by remember { mutableStateOf<FileNode?>(null) }
    var deleteTarget by remember { mutableStateOf<FileNode?>(null) }
    var headerMenuExpanded by remember { mutableStateOf(false) }

    val rootDir = project?.rootPath ?: ""

    Column(modifier = modifier.fillMaxSize().background(colors.sideBarBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(36.dp).background(colors.sideBarHeaderBg).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("EXPLORER", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, modifier = Modifier.weight(1f))
            if (rootDir.isNotEmpty()) {
                IconButton(onClick = { showCreateFileForDir = rootDir }, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.AutoMirrored.Filled.NoteAdd, "New File", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { showCreateFolderForDir = rootDir }, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.CreateNewFolder, "New Folder", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onRefresh, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Refresh, "Refresh", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                }
            }
            Box {
                IconButton(onClick = { headerMenuExpanded = true }, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.MoreVert, "Project Menu", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = headerMenuExpanded, onDismissRequest = { headerMenuExpanded = false }) {
                    DropdownMenuItem(text = { Text("New Project from Template") }, onClick = { headerMenuExpanded = false; onOpenNewProject() })
                    DropdownMenuItem(text = { Text("Recent Projects") }, onClick = { headerMenuExpanded = false; onOpenRecentProjects() })
                    DropdownMenuItem(text = { Text("Import Folder (Storage)") }, leadingIcon = { Icon(Icons.Default.DriveFolderUpload, null) }, onClick = { headerMenuExpanded = false; onImportFolder() })
                    if (rootDir.isNotEmpty()) {
                        DropdownMenuItem(text = { Text("Import File into Root") }, leadingIcon = { Icon(Icons.Default.UploadFile, null) }, onClick = { headerMenuExpanded = false; onImportFileInto(rootDir) })
                        DropdownMenuItem(text = { Text("Export Project as ZIP") }, leadingIcon = { Icon(Icons.Default.Archive, null) }, onClick = { headerMenuExpanded = false; onExportProjectZip() })
                        if (clipboard != null) {
                            DropdownMenuItem(text = { Text("Paste '${clipboard.fileName}' to Root") }, leadingIcon = { Icon(Icons.Default.ContentPaste, null) }, onClick = { headerMenuExpanded = false; onPasteInto(rootDir) })
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().clickable { onOpenRecentProjects() }.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(project?.name?.uppercase() ?: "NO WORKSPACE", color = colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text("Switch", color = colors.accent, fontSize = 11.sp)
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(fileTree, key = { it.path }) { node ->
                FileTreeRow(
                    node = node,
                    isSelected = node.path == activeFilePath,
                    hasClipboard = clipboard != null,
                    onClick = { if (node.isDirectory) onToggleFolder(node.path) else onOpenFile(node.path) },
                    onNewFile = { showCreateFileForDir = if (node.isDirectory) node.path else java.io.File(node.path).parent },
                    onNewFolder = { showCreateFolderForDir = if (node.isDirectory) node.path else java.io.File(node.path).parent },
                    onRename = { renameTarget = node },
                    onDelete = { deleteTarget = node },
                    onCopy = { onCopyNode(node, ClipboardMode.COPY) },
                    onCut = { onCopyNode(node, ClipboardMode.CUT) },
                    onPaste = { onPasteInto(if (node.isDirectory) node.path else java.io.File(node.path).parent ?: rootDir) },
                    onImportHere = { onImportFileInto(if (node.isDirectory) node.path else java.io.File(node.path).parent ?: rootDir) },
                    onExport = { onExportFile(node) }
                )
            }
        }
    }

    showCreateFileForDir?.let { dir ->
        NamePromptDialog("New File", "", "e.g. main.kt or styles.css", "Create", { showCreateFileForDir = null }) { name ->
            if (name.isNotBlank()) onCreateFile(dir, name); showCreateFileForDir = null
        }
    }
    showCreateFolderForDir?.let { dir ->
        NamePromptDialog("New Folder", "", "e.g. components", "Create", { showCreateFolderForDir = null }) { name ->
            if (name.isNotBlank()) onCreateFolder(dir, name); showCreateFolderForDir = null
        }
    }
    renameTarget?.let { node ->
        NamePromptDialog("Rename ${node.name}", node.name, "New name", "Rename", { renameTarget = null }) { name ->
            if (name.isNotBlank() && name != node.name) onRenameNode(node.path, name); renameTarget = null
        }
    }
    deleteTarget?.let { node ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete ${node.name}?") },
            text = { Text("This will permanently remove '${node.relativePath}' from the workspace.") },
            confirmButton = { TextButton(onClick = { onDeleteNode(node.path); deleteTarget = null }) { Text("Delete", color = colors.error) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun FileTreeRow(
    node: FileNode,
    isSelected: Boolean,
    hasClipboard: Boolean,
    onClick: () -> Unit,
    onNewFile: () -> Unit,
    onNewFolder: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onPaste: () -> Unit,
    onImportHere: () -> Unit,
    onExport: () -> Unit
) {
    val colors = LocalVCodeColors.current
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) colors.selectionBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = (8 + node.depth * 14).dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (node.isDirectory) {
            Icon(if (node.isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight, null, tint = colors.textMuted, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Icon(if (node.isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder, null, tint = Color(0xFFDCAD54), modifier = Modifier.size(16.dp))
        } else {
            Spacer(modifier = Modifier.width(17.dp))
            FileExtensionBadge(fileName = node.name)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(node.name, color = if (isSelected) Color.White else colors.textPrimary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Box {
            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.MoreVert, "Actions", tint = colors.textMuted, modifier = Modifier.size(14.dp))
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                if (node.isDirectory) {
                    DropdownMenuItem(text = { Text("New File Here") }, onClick = { menuExpanded = false; onNewFile() })
                    DropdownMenuItem(text = { Text("New Folder Here") }, onClick = { menuExpanded = false; onNewFolder() })
                    DropdownMenuItem(text = { Text("Import File Here") }, onClick = { menuExpanded = false; onImportHere() })
                } else {
                    DropdownMenuItem(text = { Text("Export File") }, onClick = { menuExpanded = false; onExport() })
                }
                DropdownMenuItem(text = { Text("Copy") }, onClick = { menuExpanded = false; onCopy() })
                DropdownMenuItem(text = { Text("Cut") }, onClick = { menuExpanded = false; onCut() })
                if (hasClipboard) DropdownMenuItem(text = { Text("Paste") }, onClick = { menuExpanded = false; onPaste() })
                DropdownMenuItem(text = { Text("Rename") }, onClick = { menuExpanded = false; onRename() })
                DropdownMenuItem(text = { Text("Delete", color = colors.error) }, onClick = { menuExpanded = false; onDelete() })
            }
        }
    }
}

@Composable
fun FileExtensionBadge(fileName: String) {
    val lang = LanguageMode.detect(fileName)
    Box(
        modifier = Modifier.background(lang.badgeColor.copy(alpha = 0.2f), RoundedCornerShape(3.dp)).padding(horizontal = 4.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(lang.badge.take(3), color = lang.badgeColor, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun NamePromptDialog(title: String, initialValue: String, placeholder: String, confirmLabel: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, placeholder = { Text(placeholder) }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(onClick = { onConfirm(text.trim()) }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
