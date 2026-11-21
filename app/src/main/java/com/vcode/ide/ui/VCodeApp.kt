package com.vcode.ide.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vcode.ide.domain.model.BottomPanelTab
import com.vcode.ide.domain.model.EditorTab
import com.vcode.ide.domain.model.FileNode
import com.vcode.ide.domain.model.SidebarTab
import com.vcode.ide.ui.components.ActivitySidebar
import com.vcode.ide.ui.components.BottomPanel
import com.vcode.ide.ui.components.CommandPaletteDialog
import com.vcode.ide.ui.components.ExtensionsPane
import com.vcode.ide.ui.components.FileExplorerPane
import com.vcode.ide.ui.components.FileExtensionBadge
import com.vcode.ide.ui.components.LanguagePickerDialog
import com.vcode.ide.ui.components.LivePreviewPane
import com.vcode.ide.ui.components.NewProjectDialog
import com.vcode.ide.ui.components.RecentProjectsDialog
import com.vcode.ide.ui.components.SearchPane
import com.vcode.ide.ui.components.SettingsPane
import com.vcode.ide.ui.components.SourceControlPane
import com.vcode.ide.ui.components.StatusBar
import com.vcode.ide.ui.editor.SoraEditorContainer
import com.vcode.ide.ui.theme.LocalVCodeColors
import com.vcode.ide.ui.theme.VCodeTheme
import kotlinx.coroutines.launch

@Composable
fun VCodeApp(viewModel: VCodeViewModel) {
    val state by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val terminalSessions by viewModel.terminalSessions.collectAsState()
    val activeTerminalId by viewModel.activeTerminalId.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg -> snackbarHostState.showSnackbar(msg) }
    }

    var pendingImportDir by remember { mutableStateOf<String?>(null) }
    var pendingExportSource by remember { mutableStateOf<String?>(null) }

    val importFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val dir = pendingImportDir ?: state.currentProject?.rootPath
        if (uri != null && dir != null) viewModel.importFile(uri, dir)
    }
    val importFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) viewModel.importProjectFolder(uri)
    }
    val exportFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri: Uri? ->
        val src = pendingExportSource
        if (uri != null && src != null) viewModel.exportFile(src, uri)
    }
    val exportZipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
        if (uri != null) viewModel.exportProjectZip(uri)
    }

    val triggerImportFile: (String) -> Unit = { targetDir -> pendingImportDir = targetDir; importFileLauncher.launch(arrayOf("*/*")) }
    val triggerImportFolder: () -> Unit = { importFolderLauncher.launch(null) }
    val triggerExportFile: (FileNode) -> Unit = { node -> pendingExportSource = node.path; exportFileLauncher.launch(node.name) }
    val triggerExportZip: () -> Unit = { exportZipLauncher.launch("${(state.currentProject?.name ?: "vcode_project").replace(" ", "_")}.zip") }

    VCodeTheme(themeId = settings.themeId, accentColorHex = settings.accentColorHex) {
        val colors = LocalVCodeColors.current

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().background(colors.editorBg).statusBarsPadding().navigationBarsPadding().imePadding()
        ) {
            val isExpandedScreen = maxWidth >= 720.dp
            val isWideSplitScreen = maxWidth >= 900.dp
            val drawerState = rememberDrawerState(DrawerValue.Closed)

            BackHandler(enabled = drawerState.isOpen || state.showCommandPalette || state.showNewProjectDialog || state.showRecentProjectsDialog || state.showLanguagePickerDialog || state.showFindInEditor || state.isPreviewVisible || state.isBottomPanelVisible) {
                if (drawerState.isOpen) coroutineScope.launch { drawerState.close() } else viewModel.handleBackPress()
            }

            val sidebarContent: @Composable () -> Unit = {
                when (state.activeSidebarTab) {
                    SidebarTab.EXPLORER -> FileExplorerPane(
                        project = state.currentProject, fileTree = state.fileTree, activeFilePath = state.activeTabPath, clipboard = state.fileClipboard,
                        onToggleFolder = viewModel::toggleFolderExpanded,
                        onOpenFile = { path -> viewModel.openFile(path); if (!isExpandedScreen) coroutineScope.launch { drawerState.close() } },
                        onCreateFile = viewModel::createFile, onCreateFolder = viewModel::createFolder, onRenameNode = viewModel::renameNode,
                        onDeleteNode = viewModel::deleteNode, onCopyNode = viewModel::copyToClipboard, onPasteInto = viewModel::pasteInto,
                        onImportFileInto = triggerImportFile, onImportFolder = triggerImportFolder, onExportFile = triggerExportFile,
                        onExportProjectZip = triggerExportZip, onOpenNewProject = { viewModel.toggleNewProjectDialog(true) },
                        onOpenRecentProjects = { viewModel.toggleRecentProjectsDialog(true) }, onRefresh = viewModel::refreshFileTree
                    )
                    SidebarTab.SEARCH -> SearchPane(
                        query = state.searchQuery, replaceQuery = state.searchReplaceQuery, caseSensitive = state.searchCaseSensitive,
                        useRegex = state.searchUseRegex, matches = state.searchMatches,
                        onQueryChange = { viewModel.runProjectSearch(query = it) }, onReplaceQueryChange = viewModel::updateSearchReplaceQuery,
                        onToggleCase = { viewModel.runProjectSearch(caseSensitive = !state.searchCaseSensitive) },
                        onToggleRegex = { viewModel.runProjectSearch(useRegex = !state.searchUseRegex) }, onReplaceAll = viewModel::replaceAcrossProject,
                        onMatchClick = { match -> viewModel.openFile(match.filePath, match.line); if (!isExpandedScreen) coroutineScope.launch { drawerState.close() } }
                    )
                    SidebarTab.SOURCE_CONTROL -> SourceControlPane(
                        gitState = state.gitState, onInitRepo = viewModel::gitInit, onRefresh = viewModel::refreshGitStatus,
                        onStageFile = viewModel::gitStage, onUnstageFile = viewModel::gitUnstage, onStageAll = viewModel::gitStageAll,
                        onCommit = viewModel::gitCommit, onSelectDiff = viewModel::loadGitDiff,
                        onCreateBranch = viewModel::gitCreateBranch, onCheckoutBranch = viewModel::gitCheckoutBranch
                    )
                    SidebarTab.EXTENSIONS -> ExtensionsPane(enabledExtensions = settings.enabledExtensions, onToggleExtension = viewModel::toggleExtension)
                    SidebarTab.SETTINGS -> SettingsPane(settings = settings, onUpdateSettings = viewModel::updateSettings)
                    SidebarTab.PREVIEW -> {}
                }
            }

            val mainWorkbench: @Composable () -> Unit = {
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    containerColor = colors.editorBg,
                    floatingActionButton = {
                        if (!isExpandedScreen && !state.isBottomPanelVisible && !state.isPreviewVisible) {
                            FloatingActionButton(
                                onClick = { viewModel.toggleCommandPalette(true) },
                                containerColor = colors.accent, contentColor = Color.White, shape = RoundedCornerShape(14.dp),
                                elevation = FloatingActionButtonDefaults.elevation(4.dp), modifier = Modifier.padding(bottom = 26.dp).size(46.dp)
                            ) { Icon(Icons.Default.PlayArrow, "Command Palette", modifier = Modifier.size(22.dp)) }
                        }
                    }
                ) { innerPadding ->
                    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        TopIdeBar(
                            projectName = state.currentProject?.name ?: "VCode",
                            showMenuButton = !isExpandedScreen, isPreviewVisible = state.isPreviewVisible,
                            isTerminalVisible = state.isBottomPanelVisible, hasDirtyTab = state.activeTab?.isDirty == true,
                            onMenuClick = { coroutineScope.launch { drawerState.open() } },
                            onCommandPaletteClick = { viewModel.toggleCommandPalette(true) },
                            onFindClick = { viewModel.toggleFindInEditor() }, onSaveClick = { viewModel.saveFile() },
                            onRunClick = { viewModel.runActiveFileInTerminal() }, onPreviewClick = { viewModel.toggleLivePreview() },
                            onTerminalClick = { viewModel.selectBottomTab(BottomPanelTab.TERMINAL) }
                        )

                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            if (isExpandedScreen) {
                                ActivitySidebar(
                                    activeTab = state.activeSidebarTab, isSidebarVisible = state.isSidebarVisible,
                                    isPreviewVisible = state.isPreviewVisible, isTerminalVisible = state.isBottomPanelVisible,
                                    gitChangeCount = state.gitState.changes.size, onSelectTab = viewModel::selectSidebarTab,
                                    onToggleTerminal = { viewModel.selectBottomTab(BottomPanelTab.TERMINAL) },
                                    onOpenCommandPalette = { viewModel.toggleCommandPalette(true) }
                                )
                                if (state.isSidebarVisible) {
                                    Box(modifier = Modifier.width(260.dp).fillMaxHeight().border(0.5.dp, colors.border)) { sidebarContent() }
                                }
                            }

                            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                EditorTabsBar(state.openTabs, state.activeTabPath, viewModel::selectTab, viewModel::closeTab)
                                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    if (state.isPreviewVisible) {
                                        if (isWideSplitScreen) {
                                            Row(modifier = Modifier.fillMaxSize()) {
                                                SoraEditorContainer(
                                                    state.activeTab, settings, state.showFindInEditor, state.jumpToLineRequest, viewModel::clearJumpRequest,
                                                    viewModel::onEditorContentChanged, viewModel::onCursorChanged, { viewModel.toggleFindInEditor(false) },
                                                    viewModel::formatActiveDocument, { viewModel.saveFile() }, { viewModel.toggleCommandPalette(true) },
                                                    { viewModel.selectBottomTab(BottomPanelTab.TERMINAL) }, Modifier.weight(1f).fillMaxHeight()
                                                )
                                                LivePreviewPane(
                                                    state.currentProject?.rootPath, state.previewEntryPath, state.previewRefreshToken, state.viewportConfig,
                                                    state.consoleLogs, viewModel::refreshPreview, { viewModel.toggleLivePreview(false) },
                                                    viewModel::updateViewportDevice, viewModel::updateCustomViewport, viewModel::toggleViewportOrientation,
                                                    viewModel::onWebConsoleMessage, viewModel::clearConsoleLogs, Modifier.weight(1f).fillMaxHeight()
                                                )
                                            }
                                        } else {
                                            Column(modifier = Modifier.fillMaxSize()) {
                                                SoraEditorContainer(
                                                    state.activeTab, settings, state.showFindInEditor, state.jumpToLineRequest, viewModel::clearJumpRequest,
                                                    viewModel::onEditorContentChanged, viewModel::onCursorChanged, { viewModel.toggleFindInEditor(false) },
                                                    viewModel::formatActiveDocument, { viewModel.saveFile() }, { viewModel.toggleCommandPalette(true) },
                                                    { viewModel.selectBottomTab(BottomPanelTab.TERMINAL) }, Modifier.weight(0.46f).fillMaxWidth()
                                                )
                                                LivePreviewPane(
                                                    state.currentProject?.rootPath, state.previewEntryPath, state.previewRefreshToken, state.viewportConfig,
                                                    state.consoleLogs, viewModel::refreshPreview, { viewModel.toggleLivePreview(false) },
                                                    viewModel::updateViewportDevice, viewModel::updateCustomViewport, viewModel::toggleViewportOrientation,
                                                    viewModel::onWebConsoleMessage, viewModel::clearConsoleLogs, Modifier.weight(0.54f).fillMaxWidth()
                                                )
                                            }
                                        }
                                    } else {
                                        SoraEditorContainer(
                                            state.activeTab, settings, state.showFindInEditor, state.jumpToLineRequest, viewModel::clearJumpRequest,
                                            viewModel::onEditorContentChanged, viewModel::onCursorChanged, { viewModel.toggleFindInEditor(false) },
                                            viewModel::formatActiveDocument, { viewModel.saveFile() }, { viewModel.toggleCommandPalette(true) },
                                            { viewModel.selectBottomTab(BottomPanelTab.TERMINAL) }, Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                if (state.isBottomPanelVisible) {
                                    BottomPanel(
                                        activeTab = state.activeBottomTab, heightDp = state.bottomPanelHeightDp,
                                        sessions = terminalSessions, activeSessionId = activeTerminalId,
                                        outputLogs = state.outputLogs, problems = state.problems,
                                        onSelectTab = viewModel::selectBottomTab, onClosePanel = { viewModel.toggleBottomPanel() },
                                        onResizeDelta = viewModel::resizeBottomPanel,
                                        onExecuteCommand = { cmd -> viewModel.terminalManager.executeCommand(cmd) { viewModel.refreshFileTree(); viewModel.refreshGitStatus() } },
                                        onCreateTerminalSession = { state.currentProject?.rootPath?.let { viewModel.terminalManager.createSession(it) } },
                                        onSelectTerminalSession = viewModel.terminalManager::selectSession,
                                        onCloseTerminalSession = { id -> state.currentProject?.rootPath?.let { viewModel.terminalManager.closeSession(id, it) } },
                                        onClearTerminal = viewModel.terminalManager::clearActiveSession,
                                        onSendCtrlC = viewModel.terminalManager::sendInterrupt,
                                        onClearOutput = viewModel::clearOutputLogs, onProblemClick = viewModel::jumpToProblem
                                    )
                                }
                            }
                        }

                        StatusBar(
                            branchName = state.gitState.currentBranch, gitInitialized = state.gitState.isInitialized,
                            problems = state.problems, activeTab = state.activeTab, tabSize = settings.tabSize,
                            autoSaveStatus = state.autoSaveStatus,
                            onClickBranch = { viewModel.selectSidebarTab(SidebarTab.SOURCE_CONTROL); if (!isExpandedScreen) coroutineScope.launch { drawerState.open() } },
                            onClickProblems = { viewModel.selectBottomTab(BottomPanelTab.PROBLEMS) },
                            onClickLanguage = { viewModel.toggleLanguagePickerDialog(true) }
                        )
                    }
                }
            }

            if (isExpandedScreen) mainWorkbench()
            else {
                ModalNavigationDrawer(
                    drawerState = drawerState, gesturesEnabled = drawerState.isOpen,
                    drawerContent = {
                        ModalDrawerSheet(drawerContainerColor = colors.sideBarBg, modifier = Modifier.width(310.dp)) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                ActivitySidebar(
                                    activeTab = state.activeSidebarTab, isSidebarVisible = true, isPreviewVisible = state.isPreviewVisible,
                                    isTerminalVisible = state.isBottomPanelVisible, gitChangeCount = state.gitState.changes.size,
                                    onSelectTab = { tab ->
                                        if (tab == SidebarTab.PREVIEW) { coroutineScope.launch { drawerState.close() }; viewModel.toggleLivePreview(true) }
                                        else { viewModel.selectSidebarTab(tab); viewModel.setSidebarVisible(true) }
                                    },
                                    onToggleTerminal = { coroutineScope.launch { drawerState.close() }; viewModel.selectBottomTab(BottomPanelTab.TERMINAL) },
                                    onOpenCommandPalette = { coroutineScope.launch { drawerState.close() }; viewModel.toggleCommandPalette(true) }
                                )
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) { sidebarContent() }
                            }
                        }
                    },
                    content = mainWorkbench
                )
            }

            if (state.showCommandPalette) {
                var allFiles by remember { mutableStateOf<List<FileNode>>(emptyList()) }
                LaunchedEffect(state.currentProject?.rootPath) { allFiles = viewModel.getAllProjectFiles() }
                CommandPaletteDialog(
                    commands = viewModel.buildCommandPaletteItems(triggerExportZip, { state.currentProject?.rootPath?.let(triggerImportFile) }, triggerImportFolder),
                    projectFiles = allFiles,
                    onOpenFile = { path -> viewModel.openFile(path) },
                    onDismiss = { viewModel.toggleCommandPalette(false) }
                )
            }
            if (state.showNewProjectDialog) NewProjectDialog({ viewModel.toggleNewProjectDialog(false) }, viewModel::createProject)
            if (state.showRecentProjectsDialog) {
                RecentProjectsDialog(state.recentProjects, state.currentProject?.id, viewModel::openProject, viewModel::deleteProject, { viewModel.toggleNewProjectDialog(true) }, triggerImportFolder) {
                    viewModel.toggleRecentProjectsDialog(false)
                }
            }
            if (state.showLanguagePickerDialog && state.activeTab != null) {
                LanguagePickerDialog(state.activeTab!!.language, { mode -> viewModel.setTabLanguage(state.activeTab!!.filePath, mode) }) {
                    viewModel.toggleLanguagePickerDialog(false)
                }
            }
        }
    }
}

@Composable
private fun TopIdeBar(
    projectName: String, showMenuButton: Boolean, isPreviewVisible: Boolean, isTerminalVisible: Boolean, hasDirtyTab: Boolean,
    onMenuClick: () -> Unit, onCommandPaletteClick: () -> Unit, onFindClick: () -> Unit, onSaveClick: () -> Unit,
    onRunClick: () -> Unit, onPreviewClick: () -> Unit, onTerminalClick: () -> Unit
) {
    val colors = LocalVCodeColors.current
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp).background(colors.activityBarBg).border(0.5.dp, colors.border).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (showMenuButton) {
            IconButton(onClick = onMenuClick, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Menu, "Menu", tint = colors.textPrimary, modifier = Modifier.size(19.dp)) }
        }
        Row(
            modifier = Modifier.weight(1f).height(28.dp).background(colors.editorBg, RoundedCornerShape(5.dp)).border(0.5.dp, colors.border, RoundedCornerShape(5.dp)).clickable(onClick = onCommandPaletteClick).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("VCode", color = colors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("• $projectName", color = colors.textSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        }
        IconButton(onClick = onFindClick, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Search, "Find", tint = colors.textSecondary, modifier = Modifier.size(17.dp)) }
        IconButton(onClick = onSaveClick, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Save, "Save", tint = if (hasDirtyTab) colors.accent else colors.textSecondary, modifier = Modifier.size(17.dp)) }
        IconButton(onClick = onRunClick, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.PlayArrow, "Run", tint = colors.success, modifier = Modifier.size(19.dp)) }
        IconButton(onClick = onPreviewClick, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Web, "Preview", tint = if (isPreviewVisible) colors.accent else colors.textSecondary, modifier = Modifier.size(17.dp)) }
        IconButton(onClick = onTerminalClick, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Terminal, "Terminal", tint = if (isTerminalVisible) colors.accent else colors.textSecondary, modifier = Modifier.size(17.dp)) }
    }
}

@Composable
private fun EditorTabsBar(tabs: List<EditorTab>, activeTabPath: String?, onSelectTab: (String) -> Unit, onCloseTab: (String) -> Unit) {
    val colors = LocalVCodeColors.current
    if (tabs.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth().height(34.dp).background(colors.tabBarBg).border(0.5.dp, colors.border).horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val isActive = tab.filePath == activeTabPath
            Box(modifier = Modifier.fillMaxHeight().background(if (isActive) colors.tabActiveBg else colors.tabInactiveBg).clickable { onSelectTab(tab.filePath) }) {
                if (isActive) Box(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().height(2.dp).background(colors.accent))
                Row(modifier = Modifier.fillMaxHeight().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FileExtensionBadge(tab.title)
                    Text(if (tab.isDirty) "${tab.title} •" else tab.title, color = if (isActive) colors.textPrimary else colors.textMuted, fontSize = 12.sp, fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal, maxLines = 1)
                    Icon(Icons.Default.Close, "Close Tab", tint = colors.textMuted, modifier = Modifier.size(14.dp).clickable { onCloseTab(tab.filePath) })
                }
                Box(modifier = Modifier.align(Alignment.CenterEnd).width(0.5.dp).fillMaxHeight().background(colors.border))
            }
        }
    }
}
