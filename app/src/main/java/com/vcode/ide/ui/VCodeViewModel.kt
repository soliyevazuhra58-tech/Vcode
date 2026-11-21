package com.vcode.ide.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vcode.ide.data.diagnostics.DiagnosticsAnalyzer
import com.vcode.ide.data.diagnostics.FormatterEngine
import com.vcode.ide.data.git.GitRepository
import com.vcode.ide.data.local.SettingsRepository
import com.vcode.ide.data.project.ProjectRepository
import com.vcode.ide.data.terminal.TerminalManager
import com.vcode.ide.data.terminal.TerminalSessionUi
import com.vcode.ide.domain.model.AppSettings
import com.vcode.ide.domain.model.BottomPanelTab
import com.vcode.ide.domain.model.ClipboardMode
import com.vcode.ide.domain.model.CommandItem
import com.vcode.ide.domain.model.ConsoleLogEntry
import com.vcode.ide.domain.model.EditorTab
import com.vcode.ide.domain.model.EditorThemeId
import com.vcode.ide.domain.model.FileClipboard
import com.vcode.ide.domain.model.FileNode
import com.vcode.ide.domain.model.GitState
import com.vcode.ide.domain.model.LanguageMode
import com.vcode.ide.domain.model.OutputEntry
import com.vcode.ide.domain.model.ProblemItem
import com.vcode.ide.domain.model.ProjectTemplate
import com.vcode.ide.domain.model.SearchMatch
import com.vcode.ide.domain.model.SidebarTab
import com.vcode.ide.domain.model.VCodeProject
import com.vcode.ide.domain.model.ViewportConfig
import com.vcode.ide.domain.model.ViewportDeviceType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicLong

data class VCodeUiState(
    val currentProject: VCodeProject? = null,
    val recentProjects: List<VCodeProject> = emptyList(),
    val fileTree: List<FileNode> = emptyList(),
    val expandedFolders: Set<String> = emptySet(),
    val fileClipboard: FileClipboard? = null,
    val openTabs: List<EditorTab> = emptyList(),
    val activeTabPath: String? = null,
    val activeSidebarTab: SidebarTab = SidebarTab.EXPLORER,
    val isSidebarVisible: Boolean = true,
    val isBottomPanelVisible: Boolean = false,
    val activeBottomTab: BottomPanelTab = BottomPanelTab.TERMINAL,
    val bottomPanelHeightDp: Float = 240f,
    val isPreviewVisible: Boolean = false,
    val previewEntryPath: String? = null,
    val previewRefreshToken: Long = 0L,
    val viewportConfig: ViewportConfig = ViewportConfig(),
    val consoleLogs: List<ConsoleLogEntry> = emptyList(),
    val outputLogs: List<OutputEntry> = emptyList(),
    val problems: List<ProblemItem> = emptyList(),
    val searchQuery: String = "",
    val searchReplaceQuery: String = "",
    val searchCaseSensitive: Boolean = false,
    val searchUseRegex: Boolean = false,
    val searchMatches: List<SearchMatch> = emptyList(),
    val gitState: GitState = GitState(),
    val showFindInEditor: Boolean = false,
    val showCommandPalette: Boolean = false,
    val showNewProjectDialog: Boolean = false,
    val showRecentProjectsDialog: Boolean = false,
    val showLanguagePickerDialog: Boolean = false,
    val jumpToLineRequest: Pair<String, Int>? = null,
    val autoSaveStatus: String = "Saved"
) {
    val activeTab: EditorTab? get() = openTabs.firstOrNull { it.filePath == activeTabPath }
}

class VCodeViewModel(
    private val projectRepository: ProjectRepository,
    private val settingsRepository: SettingsRepository,
    private val gitRepository: GitRepository,
    val terminalManager: TerminalManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VCodeUiState())
    val uiState: StateFlow<VCodeUiState> = _uiState.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val terminalSessions: StateFlow<List<TerminalSessionUi>> = terminalManager.sessions
    val activeTerminalId: StateFlow<String?> = terminalManager.activeSessionId

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    private val autoSaveJobs = mutableMapOf<String, Job>()
    private val idCounter = AtomicLong(1L)

    init {
        viewModelScope.launch {
            projectRepository.observeProjects().collect { projects ->
                _uiState.update { it.copy(recentProjects = projects) }
            }
        }
        viewModelScope.launch {
            val defaultProject = projectRepository.ensureDefaultProject()
            openProject(defaultProject)
        }
    }

    fun openProject(project: VCodeProject) {
        viewModelScope.launch {
            flushAllUnsavedTabs()
            projectRepository.touchProject(project.id)
            terminalManager.ensureSession(project.rootPath)
            terminalManager.setProjectDirectory(project.rootPath)

            val recentTabs = projectRepository.getRecentTabs(project.id)
            val tabs = mutableListOf<EditorTab>()
            for (rt in recentTabs) {
                val file = File(rt.filePath)
                if (file.exists() && file.isFile) {
                    val content = projectRepository.readFile(file.absolutePath)
                    val rel = file.relativeTo(File(project.rootPath)).path.replace(File.separatorChar, '/')
                    tabs += EditorTab(file.absolutePath, rel, file.name, content, false, LanguageMode.detect(file.name), rt.cursorLine, rt.cursorColumn, idCounter.incrementAndGet())
                }
            }

            if (tabs.isEmpty()) {
                val allFiles = projectRepository.listAllProjectFiles(project.rootPath)
                val firstFile = allFiles.firstOrNull { it.name == "index.html" }
                    ?: allFiles.firstOrNull { it.name.endsWith(".kt") || it.name.endsWith(".js") }
                    ?: allFiles.firstOrNull()
                if (firstFile != null) {
                    val content = projectRepository.readFile(firstFile.path)
                    tabs += EditorTab(firstFile.path, firstFile.relativePath, firstFile.name, content, false, LanguageMode.detect(firstFile.name), version = idCounter.incrementAndGet())
                    projectRepository.saveRecentTab(project.id, firstFile.path, 1, 1)
                }
            }

            val htmlEntry = projectRepository.listAllProjectFiles(project.rootPath)
                .firstOrNull { it.name.endsWith(".html", ignoreCase = true) }?.path

            _uiState.update {
                it.copy(
                    currentProject = project,
                    openTabs = tabs,
                    activeTabPath = tabs.lastOrNull()?.filePath,
                    expandedFolders = emptySet(),
                    previewEntryPath = htmlEntry,
                    searchMatches = emptyList()
                )
            }
            refreshFileTree()
            refreshGitStatus()
            recomputeProblems()
            appendOutput("Workspace", "Opened project '${project.name}'")
        }
    }

    fun createProject(name: String, template: ProjectTemplate) {
        viewModelScope.launch {
            val proj = projectRepository.createProjectFromTemplate(name, template)
            _uiState.update { it.copy(showNewProjectDialog = false) }
            openProject(proj)
            notify("Created project '${proj.name}'")
        }
    }

    fun importProjectFolder(uri: Uri) {
        viewModelScope.launch {
            runCatching { projectRepository.importFolderFromSaf(uri) }
                .onSuccess { proj -> openProject(proj); notify("Imported '${proj.name}'") }
                .onFailure { err -> notify("Import failed: ${err.message}") }
        }
    }

    fun deleteProject(project: VCodeProject) {
        viewModelScope.launch {
            projectRepository.deleteProject(project.id)
            if (_uiState.value.currentProject?.id == project.id) {
                openProject(projectRepository.ensureDefaultProject())
            }
            notify("Deleted '${project.name}'")
        }
    }

    fun refreshFileTree() {
        val root = _uiState.value.currentProject?.rootPath ?: return
        val expanded = _uiState.value.expandedFolders
        viewModelScope.launch {
            val tree = projectRepository.buildFileTree(root, expanded)
            _uiState.update { it.copy(fileTree = tree) }
        }
    }

    fun toggleFolderExpanded(folderPath: String) {
        _uiState.update { state ->
            val next = if (folderPath in state.expandedFolders) state.expandedFolders - folderPath else state.expandedFolders + folderPath
            state.copy(expandedFolders = next)
        }
        refreshFileTree()
    }

    fun openFile(filePath: String, jumpToLine: Int? = null) {
        val project = _uiState.value.currentProject ?: return
        val file = File(filePath)
        if (!file.exists() || !file.isFile) return

        val existing = _uiState.value.openTabs.firstOrNull { it.filePath == filePath }
        if (existing != null) {
            _uiState.update { it.copy(activeTabPath = filePath, jumpToLineRequest = if (jumpToLine != null) filePath to jumpToLine else null) }
            return
        }

        viewModelScope.launch {
            val content = projectRepository.readFile(filePath)
            val rel = file.relativeTo(File(project.rootPath)).path.replace(File.separatorChar, '/')
            val newTab = EditorTab(filePath, rel, file.name, content, false, LanguageMode.detect(file.name), jumpToLine ?: 1, 1, idCounter.incrementAndGet())
            projectRepository.saveRecentTab(project.id, filePath, jumpToLine ?: 1, 1)
            _uiState.update {
                it.copy(openTabs = it.openTabs + newTab, activeTabPath = filePath, jumpToLineRequest = if (jumpToLine != null) filePath to jumpToLine else null)
            }
            recomputeProblems()
        }
    }

    fun selectTab(filePath: String) { _uiState.update { it.copy(activeTabPath = filePath) } }

    fun closeTab(filePath: String) {
        viewModelScope.launch {
            val tab = _uiState.value.openTabs.firstOrNull { it.filePath == filePath }
            if (tab != null && tab.isDirty) projectRepository.writeFile(tab.filePath, tab.content)
            projectRepository.removeRecentTab(filePath)
            _uiState.update { state ->
                val remaining = state.openTabs.filterNot { it.filePath == filePath }
                val nextActive = if (state.activeTabPath == filePath) remaining.lastOrNull()?.filePath else state.activeTabPath
                state.copy(openTabs = remaining, activeTabPath = nextActive)
            }
            recomputeProblems()
        }
    }

    fun onEditorContentChanged(filePath: String, newContent: String) {
        _uiState.update { state ->
            state.copy(
                openTabs = state.openTabs.map { if (it.filePath == filePath) it.copy(content = newContent, isDirty = true) else it },
                autoSaveStatus = "Modified"
            )
        }
        val currentSettings = settings.value
        if (currentSettings.autoSave) {
            autoSaveJobs[filePath]?.cancel()
            autoSaveJobs[filePath] = viewModelScope.launch {
                delay(currentSettings.autoSaveDelayMs)
                saveFile(filePath, silent = true)
            }
        }
    }

    fun onCursorChanged(filePath: String, line: Int, column: Int) {
        _uiState.update { state ->
            state.copy(openTabs = state.openTabs.map { if (it.filePath == filePath) it.copy(cursorLine = line, cursorColumn = column) else it })
        }
    }

    fun saveFile(filePath: String? = _uiState.value.activeTabPath, silent: Boolean = false) {
        val targetPath = filePath ?: return
        val tab = _uiState.value.openTabs.firstOrNull { it.filePath == targetPath } ?: return
        viewModelScope.launch {
            projectRepository.writeFile(tab.filePath, tab.content)
            _uiState.value.currentProject?.let { proj -> projectRepository.saveRecentTab(proj.id, tab.filePath, tab.cursorLine, tab.cursorColumn) }
            _uiState.update { state ->
                state.copy(
                    openTabs = state.openTabs.map { if (it.filePath == targetPath) it.copy(isDirty = false) else it },
                    autoSaveStatus = "Saved",
                    previewRefreshToken = if (state.isPreviewVisible) idCounter.incrementAndGet() else state.previewRefreshToken
                )
            }
            recomputeProblems()
            refreshGitStatus()
            if (!silent) notify("Saved ${tab.title}")
        }
    }

    fun flushAllUnsavedTabs() {
        viewModelScope.launch {
            _uiState.value.openTabs.filter { it.isDirty }.forEach { tab -> projectRepository.writeFile(tab.filePath, tab.content) }
            _uiState.update { state -> state.copy(openTabs = state.openTabs.map { it.copy(isDirty = false) }, autoSaveStatus = "Saved") }
        }
    }

    fun setTabLanguage(filePath: String, language: LanguageMode) {
        _uiState.update { state ->
            state.copy(
                openTabs = state.openTabs.map { if (it.filePath == filePath) it.copy(language = language, version = idCounter.incrementAndGet()) else it },
                showLanguagePickerDialog = false
            )
        }
        recomputeProblems()
    }

    fun formatActiveDocument() {
        val tab = _uiState.value.activeTab ?: return
        val formatted = FormatterEngine.format(tab.content, tab.language, settings.value.tabSize)
        if (formatted != tab.content) {
            _uiState.update { state ->
                state.copy(openTabs = state.openTabs.map { if (it.filePath == tab.filePath) it.copy(content = formatted, isDirty = true, version = idCounter.incrementAndGet()) else it })
            }
            saveFile(tab.filePath, silent = true)
            notify("Formatted ${tab.title}")
        }
    }

    fun createFile(parentDir: String, name: String) {
        viewModelScope.launch {
            runCatching { projectRepository.createFile(parentDir, name) }
                .onSuccess { file ->
                    _uiState.update { it.copy(expandedFolders = it.expandedFolders + parentDir) }
                    refreshFileTree(); refreshGitStatus(); openFile(file.absolutePath); notify("Created ${file.name}")
                }
                .onFailure { notify("Create failed: ${it.message}") }
        }
    }

    fun createFolder(parentDir: String, name: String) {
        viewModelScope.launch {
            runCatching { projectRepository.createFolder(parentDir, name) }
                .onSuccess { dir ->
                    _uiState.update { it.copy(expandedFolders = it.expandedFolders + parentDir + dir.absolutePath) }
                    refreshFileTree(); notify("Created ${dir.name}")
                }
                .onFailure { notify("Create failed: ${it.message}") }
        }
    }

    fun renameNode(path: String, newName: String) {
        viewModelScope.launch {
            val renamed = projectRepository.renameNode(path, newName)
            if (renamed != null) {
                val root = _uiState.value.currentProject?.rootPath
                _uiState.update { state ->
                    val newTabs = state.openTabs.map { tab ->
                        if (tab.filePath == path) {
                            val rel = root?.let { renamed.relativeTo(File(it)).path.replace(File.separatorChar, '/') } ?: renamed.name
                            tab.copy(filePath = renamed.absolutePath, relativePath = rel, title = renamed.name, language = LanguageMode.detect(renamed.name), version = idCounter.incrementAndGet())
                        } else tab
                    }
                    state.copy(openTabs = newTabs, activeTabPath = if (state.activeTabPath == path) renamed.absolutePath else state.activeTabPath)
                }
                refreshFileTree(); refreshGitStatus(); notify("Renamed to ${renamed.name}")
            } else notify("Rename failed")
        }
    }

    fun deleteNode(path: String) {
        viewModelScope.launch {
            if (projectRepository.deleteNode(path)) {
                _uiState.update { state ->
                    val remaining = state.openTabs.filterNot { it.filePath == path || it.filePath.startsWith("$path/") }
                    val nextActive = if (state.activeTabPath != null && remaining.none { it.filePath == state.activeTabPath }) remaining.lastOrNull()?.filePath else state.activeTabPath
                    state.copy(openTabs = remaining, activeTabPath = nextActive)
                }
                refreshFileTree(); refreshGitStatus(); recomputeProblems(); notify("Deleted")
            }
        }
    }

    fun copyToClipboard(node: FileNode, mode: ClipboardMode) {
        _uiState.update { it.copy(fileClipboard = FileClipboard(node.path, node.name, mode)) }
        notify("${if (mode == ClipboardMode.COPY) "Copied" else "Cut"} ${node.name}")
    }

    fun pasteInto(targetDirPath: String) {
        val clip = _uiState.value.fileClipboard ?: return
        viewModelScope.launch {
            val result = projectRepository.copyOrMoveNode(clip.sourcePath, targetDirPath, clip.mode == ClipboardMode.CUT)
            if (result != null) {
                if (clip.mode == ClipboardMode.CUT) _uiState.update { it.copy(fileClipboard = null) }
                _uiState.update { it.copy(expandedFolders = it.expandedFolders + targetDirPath) }
                refreshFileTree(); refreshGitStatus(); notify("Pasted ${result.name}")
            } else notify("Paste failed")
        }
    }

    fun importFile(uri: Uri, targetDirPath: String) {
        viewModelScope.launch {
            runCatching { projectRepository.importFileFromUri(uri, targetDirPath) }
                .onSuccess { file -> refreshFileTree(); openFile(file.absolutePath); notify("Imported ${file.name}") }
                .onFailure { notify("Import failed: ${it.message}") }
        }
    }

    fun exportFile(sourcePath: String, destUri: Uri) {
        viewModelScope.launch {
            runCatching { projectRepository.exportFileToUri(sourcePath, destUri) }
                .onSuccess { notify("Exported file") }
                .onFailure { notify("Export failed: ${it.message}") }
        }
    }

    fun exportProjectZip(destUri: Uri) {
        val project = _uiState.value.currentProject ?: return
        viewModelScope.launch {
            flushAllUnsavedTabs()
            runCatching { projectRepository.exportProjectAsZip(project.rootPath, destUri) }
                .onSuccess { notify("Exported ${project.name}.zip"); appendOutput("Export", "ZIP saved") }
                .onFailure { notify("ZIP export failed: ${it.message}") }
        }
    }

    fun runProjectSearch(query: String = _uiState.value.searchQuery, caseSensitive: Boolean = _uiState.value.searchCaseSensitive, useRegex: Boolean = _uiState.value.searchUseRegex) {
        val root = _uiState.value.currentProject?.rootPath ?: return
        _uiState.update { it.copy(searchQuery = query, searchCaseSensitive = caseSensitive, searchUseRegex = useRegex) }
        viewModelScope.launch {
            val matches = projectRepository.searchInProject(root, query, caseSensitive, useRegex)
            _uiState.update { it.copy(searchMatches = matches) }
        }
    }

    fun updateSearchReplaceQuery(value: String) { _uiState.update { it.copy(searchReplaceQuery = value) } }

    fun replaceAcrossProject() {
        val state = _uiState.value
        val root = state.currentProject?.rootPath ?: return
        if (state.searchQuery.isBlank()) return
        viewModelScope.launch {
            val changed = projectRepository.replaceInProject(root, state.searchQuery, state.searchReplaceQuery, state.searchCaseSensitive, state.searchUseRegex)
            val reloadedTabs = state.openTabs.map { it.copy(content = projectRepository.readFile(it.filePath), isDirty = false, version = idCounter.incrementAndGet()) }
            _uiState.update { it.copy(openTabs = reloadedTabs) }
            runProjectSearch(); refreshGitStatus(); notify("Replaced in $changed file(s)")
        }
    }

    fun refreshGitStatus() {
        val root = _uiState.value.currentProject?.rootPath ?: return
        viewModelScope.launch { _uiState.update { it.copy(gitState = gitRepository.inspect(root)) } }
    }

    fun gitInit() {
        val root = _uiState.value.currentProject?.rootPath ?: return
        viewModelScope.launch { val msg = gitRepository.initRepo(root); refreshGitStatus(); appendOutput("Git", msg); notify("Initialized Git") }
    }

    fun gitStage(relPath: String) {
        val root = _uiState.value.currentProject?.rootPath ?: return
        viewModelScope.launch { gitRepository.stageFile(root, relPath); refreshGitStatus() }
    }

    fun gitUnstage(relPath: String) {
        val root = _uiState.value.currentProject?.rootPath ?: return
        viewModelScope.launch { gitRepository.unstageFile(root, relPath); refreshGitStatus() }
    }

    fun gitStageAll() {
        val root = _uiState.value.currentProject?.rootPath ?: return
        viewModelScope.launch { gitRepository.stageAll(root); refreshGitStatus() }
    }

    fun gitCommit(message: String) {
        val root = _uiState.value.currentProject?.rootPath ?: return
        viewModelScope.launch {
            runCatching { gitRepository.stageAll(root); gitRepository.commit(root, message) }
                .onSuccess { msg -> refreshGitStatus(); appendOutput("Git", msg); notify(msg) }
                .onFailure { notify("Commit failed: ${it.message}") }
        }
    }

    fun gitCreateBranch(name: String) {
        val root = _uiState.value.currentProject?.rootPath ?: return
        viewModelScope.launch {
            runCatching { gitRepository.createAndCheckoutBranch(root, name) }
                .onSuccess { refreshGitStatus(); notify("Switched to $name") }
                .onFailure { notify("Branch error: ${it.message}") }
        }
    }

    fun gitCheckoutBranch(name: String) {
        val root = _uiState.value.currentProject?.rootPath ?: return
        viewModelScope.launch {
            runCatching { gitRepository.checkoutBranch(root, name) }
                .onSuccess { refreshGitStatus(); notify("Checked out $name") }
                .onFailure { notify("Checkout error: ${it.message}") }
        }
    }

    fun loadGitDiff(relPath: String) {
        val root = _uiState.value.currentProject?.rootPath ?: return
        viewModelScope.launch {
            val diff = gitRepository.diffForFile(root, relPath)
            _uiState.update { it.copy(gitState = it.gitState.copy(selectedDiff = diff)) }
        }
    }

    fun selectSidebarTab(tab: SidebarTab) {
        if (tab == SidebarTab.PREVIEW) { toggleLivePreview(forceShow = true); return }
        _uiState.update { state ->
            if (state.activeSidebarTab == tab && state.isSidebarVisible) state.copy(isSidebarVisible = false)
            else state.copy(activeSidebarTab = tab, isSidebarVisible = true)
        }
    }

    fun setSidebarVisible(visible: Boolean) { _uiState.update { it.copy(isSidebarVisible = visible) } }

    fun selectBottomTab(tab: BottomPanelTab) {
        _uiState.update { state ->
            if (state.activeBottomTab == tab && state.isBottomPanelVisible) state.copy(isBottomPanelVisible = false)
            else state.copy(activeBottomTab = tab, isBottomPanelVisible = true)
        }
    }

    fun toggleBottomPanel() { _uiState.update { it.copy(isBottomPanelVisible = !it.isBottomPanelVisible) } }

    fun resizeBottomPanel(deltaDp: Float) {
        _uiState.update { it.copy(bottomPanelHeightDp = (it.bottomPanelHeightDp + deltaDp).coerceIn(140f, 460f)) }
    }

    fun toggleLivePreview(forceShow: Boolean? = null) {
        val root = _uiState.value.currentProject?.rootPath
        viewModelScope.launch {
            flushAllUnsavedTabs()
            val entry = _uiState.value.activeTab?.takeIf { it.language == LanguageMode.HTML }?.filePath
                ?: _uiState.value.previewEntryPath
                ?: root?.let { r -> projectRepository.listAllProjectFiles(r).firstOrNull { it.name.endsWith(".html", true) }?.path }
            _uiState.update { state ->
                state.copy(isPreviewVisible = forceShow ?: !state.isPreviewVisible, previewEntryPath = entry, previewRefreshToken = idCounter.incrementAndGet())
            }
        }
    }

    fun refreshPreview() {
        flushAllUnsavedTabs()
        _uiState.update { it.copy(previewRefreshToken = idCounter.incrementAndGet()) }
    }

    fun updateViewportDevice(type: ViewportDeviceType) {
        _uiState.update { state ->
            val current = state.viewportConfig
            val next = if (type == ViewportDeviceType.CUSTOM) current.copy(deviceType = type)
            else current.copy(deviceType = type, widthPx = if (type.defaultWidthDp > 0) type.defaultWidthDp else current.widthPx, heightPx = if (type.defaultHeightDp > 0) type.defaultHeightDp else current.heightPx)
            state.copy(viewportConfig = next)
        }
    }

    fun updateCustomViewport(widthPx: Int, heightPx: Int) {
        _uiState.update { state ->
            state.copy(viewportConfig = state.viewportConfig.copy(deviceType = ViewportDeviceType.CUSTOM, widthPx = widthPx.coerceIn(220, 2400), heightPx = heightPx.coerceIn(220, 2400)))
        }
    }

    fun toggleViewportOrientation() {
        _uiState.update { state -> state.copy(viewportConfig = state.viewportConfig.copy(isLandscape = !state.viewportConfig.isLandscape)) }
    }

    fun onWebConsoleMessage(level: String, message: String, source: String) {
        val entry = ConsoleLogEntry(idCounter.incrementAndGet(), level, message, source)
        _uiState.update { it.copy(consoleLogs = (it.consoleLogs + entry).takeLast(200)) }
        appendOutput("Preview/$level", message, isError = level == "ERROR")
    }

    fun clearConsoleLogs() { _uiState.update { it.copy(consoleLogs = emptyList()) } }

    fun appendOutput(tag: String, message: String, isError: Boolean = false) {
        val entry = OutputEntry(idCounter.incrementAndGet(), tag, message, isError)
        _uiState.update { it.copy(outputLogs = (it.outputLogs + entry).takeLast(300)) }
    }

    fun clearOutputLogs() { _uiState.update { it.copy(outputLogs = emptyList()) } }

    fun runActiveFileInTerminal() {
        val tab = _uiState.value.activeTab ?: return
        saveFile(tab.filePath, silent = true)
        _uiState.update { it.copy(isBottomPanelVisible = true, activeBottomTab = BottomPanelTab.TERMINAL) }
        when (tab.language) {
            LanguageMode.JAVASCRIPT -> terminalManager.executeCommand("node \"${tab.relativePath}\"") { refreshFileTree() }
            LanguageMode.SHELL -> terminalManager.executeCommand("sh \"${tab.relativePath}\"") { refreshFileTree() }
            LanguageMode.HTML -> toggleLivePreview(forceShow = true)
            else -> appendOutput("Run", "Checked ${tab.title} (${tab.language.displayName})")
        }
    }

    fun toggleFindInEditor(show: Boolean? = null) { _uiState.update { it.copy(showFindInEditor = show ?: !it.showFindInEditor) } }
    fun toggleCommandPalette(show: Boolean? = null) { _uiState.update { it.copy(showCommandPalette = show ?: !it.showCommandPalette) } }
    fun toggleNewProjectDialog(show: Boolean) { _uiState.update { it.copy(showNewProjectDialog = show) } }
    fun toggleRecentProjectsDialog(show: Boolean) { _uiState.update { it.copy(showRecentProjectsDialog = show) } }
    fun toggleLanguagePickerDialog(show: Boolean) { _uiState.update { it.copy(showLanguagePickerDialog = show) } }
    fun clearJumpRequest() { _uiState.update { it.copy(jumpToLineRequest = null) } }
    fun jumpToProblem(problem: ProblemItem) { openFile(problem.filePath, jumpToLine = problem.line) }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepository.updateSettings(transform) }
    }

    fun toggleExtension(extensionId: String) {
        updateSettings { s ->
            val next = if (extensionId in s.enabledExtensions) s.enabledExtensions - extensionId else s.enabledExtensions + extensionId
            s.copy(enabledExtensions = next)
        }
    }

    suspend fun getAllProjectFiles(): List<FileNode> {
        val root = _uiState.value.currentProject?.rootPath ?: return emptyList()
        return projectRepository.listAllProjectFiles(root)
    }

    fun buildCommandPaletteItems(
        onTriggerExportZip: () -> Unit,
        onTriggerImportFile: () -> Unit,
        onTriggerImportFolder: () -> Unit
    ): List<CommandItem> = listOf(
        CommandItem("cmd.save", "File: Save Active File", "Flush current editor buffer to disk", "Ctrl+S") { saveFile() },
        CommandItem("cmd.new_project", "File: New Project from Template", "Web, Polyglot, JavaScript, or Empty") { toggleNewProjectDialog(true) },
        CommandItem("cmd.open_recent", "File: Open Recent Project", "Switch between saved projects") { toggleRecentProjectsDialog(true) },
        CommandItem("cmd.import_file", "File: Import File from Storage", "Android Storage Access Framework") { onTriggerImportFile() },
        CommandItem("cmd.import_folder", "File: Import Folder from Storage", "Import external directory via SAF") { onTriggerImportFolder() },
        CommandItem("cmd.export_zip", "Project: Export Workspace as ZIP", "Create downloadable .zip archive") { onTriggerExportZip() },
        CommandItem("cmd.find", "Edit: Find and Replace", "Search & replace in current file", "Ctrl+F") { toggleFindInEditor(true) },
        CommandItem("cmd.format", "Edit: Format Document", "Auto-indent JSON, HTML, CSS, JS, Kotlin") { formatActiveDocument() },
        CommandItem("cmd.language", "Editor: Change Language Mode", "Switch syntax highlighting mode") { toggleLanguagePickerDialog(true) },
        CommandItem("cmd.terminal", "View: Toggle Integrated Terminal", "Show or hide real shell terminal", "Ctrl+`") {
            _uiState.update { it.copy(isBottomPanelVisible = !it.isBottomPanelVisible, activeBottomTab = BottomPanelTab.TERMINAL) }
        },
        CommandItem("cmd.problems", "View: Show Problems Panel", "Inspect syntax errors and warnings") {
            _uiState.update { it.copy(isBottomPanelVisible = true, activeBottomTab = BottomPanelTab.PROBLEMS) }
        },
        CommandItem("cmd.preview", "Preview: Toggle Live Browser Preview", "Split screen HTML/CSS/JS live preview") { toggleLivePreview() },
        CommandItem("cmd.run", "Run: Execute Active File", "Run JS in Rhino or HTML in Live Preview") { runActiveFileInTerminal() },
        CommandItem("cmd.git_init", "Git: Initialize Repository", "Create local .git repository with JGit") { gitInit() },
        CommandItem("cmd.theme_dark", "Theme: VS Code Dark+", "Switch to default dark theme") { updateSettings { it.copy(themeId = EditorThemeId.DARK_PLUS) } },
        CommandItem("cmd.theme_onedark", "Theme: One Dark Pro", "Switch to One Dark Pro theme") { updateSettings { it.copy(themeId = EditorThemeId.ONE_DARK_PRO) } },
        CommandItem("cmd.theme_dracula", "Theme: Dracula", "Switch to Dracula theme") { updateSettings { it.copy(themeId = EditorThemeId.DRACULA) } },
        CommandItem("cmd.theme_light", "Theme: VS Code Light+", "Switch to crisp light theme") { updateSettings { it.copy(themeId = EditorThemeId.LIGHT_PLUS) } },
        CommandItem("cmd.font_up", "Editor: Increase Font Size", "Bump code font size +1sp") { updateSettings { it.copy(fontSizeSp = (it.fontSizeSp + 1).coerceAtMost(24)) } },
        CommandItem("cmd.font_down", "Editor: Decrease Font Size", "Shrink code font size -1sp") { updateSettings { it.copy(fontSizeSp = (it.fontSizeSp - 1).coerceAtLeast(10)) } }
    )

    fun handleBackPress(): Boolean {
        val state = _uiState.value
        return when {
            state.showCommandPalette -> { toggleCommandPalette(false); true }
            state.showNewProjectDialog -> { toggleNewProjectDialog(false); true }
            state.showRecentProjectsDialog -> { toggleRecentProjectsDialog(false); true }
            state.showLanguagePickerDialog -> { toggleLanguagePickerDialog(false); true }
            state.showFindInEditor -> { toggleFindInEditor(false); true }
            state.isPreviewVisible -> { _uiState.update { it.copy(isPreviewVisible = false) }; true }
            state.isBottomPanelVisible -> { _uiState.update { it.copy(isBottomPanelVisible = false) }; true }
            else -> false
        }
    }

    private fun recomputeProblems() {
        val allProblems = _uiState.value.openTabs.flatMap { tab -> DiagnosticsAnalyzer.analyze(tab.filePath, tab.title, tab.language, tab.content) }
        _uiState.update { it.copy(problems = allProblems) }
    }

    private fun notify(message: String) { _snackbarEvents.tryEmit(message) }

    class Factory(
        private val projectRepository: ProjectRepository,
        private val settingsRepository: SettingsRepository,
        private val terminalManager: TerminalManager,
        private val gitRepository: GitRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VCodeViewModel(projectRepository, settingsRepository, gitRepository, terminalManager) as T
        }
    }
}
