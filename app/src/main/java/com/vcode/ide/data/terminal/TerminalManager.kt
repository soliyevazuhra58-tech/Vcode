package com.vcode.ide.data.terminal

import android.content.Context
import com.vcode.ide.data.git.GitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class TerminalSessionUi(
    val id: String,
    val title: String,
    val workingDir: String,
    val lines: List<String>,
    val isRunningProcess: Boolean,
    val history: List<String>
)

class TerminalManager(
    private val context: Context,
    private val gitRepository: GitRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private data class SessionRuntime(
        val id: String,
        var title: String,
        var workingDir: File,
        val lines: MutableList<String> = mutableListOf(),
        val history: MutableList<String> = mutableListOf(),
        var activeProcess: Process? = null,
        var processWriter: BufferedWriter? = null
    )

    private val runtimes = ConcurrentHashMap<String, SessionRuntime>()
    private val orderedIds = mutableListOf<String>()
    private val _sessions = MutableStateFlow<List<TerminalSessionUi>>(emptyList())
    val sessions: StateFlow<List<TerminalSessionUi>> = _sessions.asStateFlow()
    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    fun ensureSession(projectRoot: String) {
        if (orderedIds.isEmpty()) createSession(projectRoot)
    }

    fun setProjectDirectory(projectRoot: String) {
        val active = _activeSessionId.value?.let { runtimes[it] } ?: return
        val dir = File(projectRoot)
        if (dir.exists() && dir.isDirectory && active.workingDir.absolutePath != dir.absolutePath) {
            active.workingDir = dir
            appendLine(active, "\u001B[36m[workspace]\u001B[0m switched to ${dir.name}")
        }
    }

    fun createSession(projectRoot: String): String {
        val id = UUID.randomUUID().toString().take(6)
        val index = orderedIds.size + 1
        val dir = File(projectRoot).takeIf { it.exists() && it.isDirectory } ?: context.filesDir
        val runtime = SessionRuntime(id, "sh #$index", dir).apply {
            lines += "\u001B[36mVCode Android Shell\u001B[0m (/system/bin/sh)"
            lines += "\u001B[90mBuilt-ins: cd, clear, help, git <cmd>, node <file.js>, js <expr>, or any Android binary.\u001B[0m"
        }
        runtimes[id] = runtime
        orderedIds += id
        _activeSessionId.value = id
        publish()
        return id
    }

    fun selectSession(sessionId: String) {
        if (runtimes.containsKey(sessionId)) _activeSessionId.value = sessionId
    }

    fun closeSession(sessionId: String, projectRoot: String) {
        runtimes.remove(sessionId)?.activeProcess?.destroyForcibly()
        orderedIds.remove(sessionId)
        if (orderedIds.isEmpty()) createSession(projectRoot)
        else {
            if (_activeSessionId.value == sessionId) _activeSessionId.value = orderedIds.last()
            publish()
        }
    }

    fun clearActiveSession() {
        val runtime = _activeSessionId.value?.let { runtimes[it] } ?: return
        synchronized(runtime.lines) { runtime.lines.clear() }
        publish()
    }

    fun sendInterrupt() {
        val runtime = _activeSessionId.value?.let { runtimes[it] } ?: return
        val proc = runtime.activeProcess
        if (proc != null && proc.isAlive) {
            proc.destroyForcibly()
            runtime.activeProcess = null
            runtime.processWriter = null
            appendLine(runtime, "\u001B[31m^C (process terminated)\u001B[0m")
        }
    }

    fun executeCommand(rawInput: String, onWorkspaceMutated: () -> Unit = {}) {
        val runtime = _activeSessionId.value?.let { runtimes[it] } ?: return
        val input = rawInput.trimEnd('\r', '\n')
        val running = runtime.activeProcess
        if (running != null && running.isAlive) {
            scope.launch {
                runCatching {
                    runtime.processWriter?.write(input + "\n")
                    runtime.processWriter?.flush()
                    appendLine(runtime, input)
                }
            }
            return
        }

        val cmd = input.trim()
        val promptPath = runtime.workingDir.name.ifBlank { "/" }
        appendLine(runtime, "\u001B[32m$promptPath $\u001B[0m $cmd")
        if (cmd.isEmpty()) return
        if (runtime.history.lastOrNull() != cmd) {
            runtime.history += cmd
            if (runtime.history.size > 200) runtime.history.removeAt(0)
        }

        when {
            cmd == "clear" || cmd == "cls" -> { synchronized(runtime.lines) { runtime.lines.clear() }; publish(); return }
            cmd == "help" -> {
                appendLine(runtime, "\u001B[33mVCode Terminal Runtime:\u001B[0m")
                appendLine(runtime, "  - Real /system/bin/sh commands (ls, pwd, cat, mkdir, rm, cp, mv, grep, find, uname, env)")
                appendLine(runtime, "  - Built-in \u001B[36mgit\u001B[0m (init | status | add . | commit -m \"msg\" | log | branch)")
                appendLine(runtime, "  - Built-in \u001B[36mnode <file.js>\u001B[0m / \u001B[36mjs <code>\u001B[0m via Mozilla Rhino JS engine")
                return
            }
            cmd == "cd" || cmd.startsWith("cd ") -> { handleCd(runtime, cmd.removePrefix("cd").trim()); return }
            cmd == "pwd" -> { appendLine(runtime, runtime.workingDir.absolutePath); return }
            cmd == "git" || cmd.startsWith("git ") -> {
                scope.launch { handleBuiltInGit(runtime, cmd.removePrefix("git").trim()); onWorkspaceMutated() }
                return
            }
            cmd.startsWith("node ") || cmd.startsWith("js ") -> {
                scope.launch { handleBuiltInJs(runtime, cmd) }
                return
            }
        }

        scope.launch { runRealShellCommand(runtime, cmd, onWorkspaceMutated) }
    }

    private fun handleCd(runtime: SessionRuntime, arg: String) {
        val target = when {
            arg.isBlank() || arg == "~" -> context.filesDir
            arg == "-" -> runtime.workingDir.parentFile ?: runtime.workingDir
            arg.startsWith("/") -> File(arg)
            else -> File(runtime.workingDir, arg)
        }.canonicalFile
        if (target.exists() && target.isDirectory) { runtime.workingDir = target; publish() }
        else appendLine(runtime, "\u001B[31mcd: $arg: No such directory\u001B[0m")
    }

    private suspend fun handleBuiltInGit(runtime: SessionRuntime, argsRaw: String) {
        val tokens = tokenizeArgs(argsRaw)
        val sub = tokens.firstOrNull() ?: ""
        val root = runtime.workingDir.absolutePath
        runCatching {
            when (sub) {
                "", "help" -> appendLine(runtime, "usage: git <init|status|add|commit|log|branch>")
                "init" -> appendLine(runtime, gitRepository.initRepo(root))
                "status" -> {
                    val st = gitRepository.inspect(root)
                    if (!st.isInitialized) appendLine(runtime, "\u001B[31mfatal: not a git repository\u001B[0m")
                    else {
                        appendLine(runtime, "On branch \u001B[36m${st.currentBranch}\u001B[0m")
                        if (st.changes.isEmpty()) appendLine(runtime, "nothing to commit, working tree clean")
                        else st.changes.forEach { appendLine(runtime, "  ${if (it.isStaged) "[staged]" else "[unstaged]"} ${it.status}: ${it.relativePath}") }
                    }
                }
                "add" -> {
                    val target = tokens.getOrNull(1) ?: "."
                    if (target == "." || target == "-A" || target == "--all") gitRepository.stageAll(root)
                    else gitRepository.stageFile(root, target)
                    appendLine(runtime, "Staged $target")
                }
                "commit" -> {
                    val mIdx = tokens.indexOf("-m")
                    val msg = if (mIdx >= 0 && mIdx + 1 < tokens.size) tokens[mIdx + 1] else "Update workspace"
                    appendLine(runtime, "\u001B[32m${gitRepository.commit(root, msg)}\u001B[0m")
                }
                "log" -> {
                    val st = gitRepository.inspect(root)
                    if (st.commits.isEmpty()) appendLine(runtime, "No commits yet.")
                    else st.commits.forEach { appendLine(runtime, "\u001B[33m${it.shortId}\u001B[0m ${it.message}") }
                }
                "branch" -> {
                    val newBranch = tokens.getOrNull(1)
                    if (newBranch != null) { gitRepository.createAndCheckoutBranch(root, newBranch); appendLine(runtime, "Switched to $newBranch") }
                    else gitRepository.inspect(root).branches.forEach { appendLine(runtime, it) }
                }
                else -> appendLine(runtime, "\u001B[31mgit: unsupported subcommand '$sub'\u001B[0m")
            }
        }.onFailure { appendLine(runtime, "\u001B[31mgit error: ${it.message}\u001B[0m") }
    }

    private fun handleBuiltInJs(runtime: SessionRuntime, cmd: String) {
        val isNode = cmd.startsWith("node ")
        val arg = if (isNode) cmd.removePrefix("node ").trim() else cmd.removePrefix("js ").trim()
        val scriptSource = if (isNode) {
            val file = if (arg.startsWith("/")) File(arg) else File(runtime.workingDir, arg)
            if (!file.exists() || !file.isFile) { appendLine(runtime, "\u001B[31mnode: cannot find '${file.path}'\u001B[0m"); return }
            file.readText()
        } else arg

        val cx = org.mozilla.javascript.Context.enter()
        try {
            cx.optimizationLevel = -1
            cx.languageVersion = org.mozilla.javascript.Context.VERSION_ES6
            val scopeObj: Scriptable = cx.initStandardObjects()
            val consoleObj = cx.newObject(scopeObj)
            val logFn = object : org.mozilla.javascript.BaseFunction() {
                override fun call(cx: org.mozilla.javascript.Context, scope: Scriptable, thisObj: Scriptable, args: Array<out Any?>): Any {
                    appendLine(runtime, args.joinToString(" ") { org.mozilla.javascript.Context.toString(it) })
                    return org.mozilla.javascript.Context.getUndefinedValue()
                }
            }
            listOf("log", "info", "warn", "error").forEach { ScriptableObject.putProperty(consoleObj, it, logFn) }
            ScriptableObject.putProperty(scopeObj, "console", consoleObj)
            val result = cx.evaluateString(scopeObj, scriptSource, if (isNode) arg else "<eval>", 1, null)
            if (!isNode && result != null && result != org.mozilla.javascript.Context.getUndefinedValue()) {
                appendLine(runtime, "\u001B[36m=> ${org.mozilla.javascript.Context.toString(result)}\u001B[0m")
            }
        } catch (t: Throwable) {
            appendLine(runtime, "\u001B[31mJS Error: ${t.message}\u001B[0m")
        } finally {
            org.mozilla.javascript.Context.exit()
        }
    }

    private fun runRealShellCommand(runtime: SessionRuntime, cmd: String, onWorkspaceMutated: () -> Unit) {
        try {
            val pb = ProcessBuilder("/system/bin/sh", "-c", cmd).directory(runtime.workingDir)
            pb.environment().apply {
                put("HOME", context.filesDir.absolutePath)
                put("TMPDIR", context.cacheDir.absolutePath)
                put("TERM", "xterm-256color")
                put("PROJECT_DIR", runtime.workingDir.absolutePath)
            }
            val process = pb.start()
            runtime.activeProcess = process
            runtime.processWriter = BufferedWriter(OutputStreamWriter(process.outputStream))
            publish()

            val t1 = Thread {
                BufferedReader(InputStreamReader(process.inputStream)).use { r ->
                    var line: String?
                    while (r.readLine().also { line = it } != null) appendLine(runtime, line!!)
                }
            }
            val t2 = Thread {
                BufferedReader(InputStreamReader(process.errorStream)).use { r ->
                    var line: String?
                    while (r.readLine().also { line = it } != null) appendLine(runtime, "\u001B[31m${line!!}\u001B[0m")
                }
            }
            t1.start(); t2.start()
            val code = process.waitFor()
            t1.join(1000); t2.join(1000)
            if (code != 0) appendLine(runtime, "\u001B[90m[exit code $code]\u001B[0m")
            onWorkspaceMutated()
        } catch (e: Exception) {
            appendLine(runtime, "\u001B[31mShell error: ${e.message}\u001B[0m")
        } finally {
            runtime.activeProcess = null
            runtime.processWriter = null
            publish()
        }
    }

    private fun appendLine(runtime: SessionRuntime, text: String) {
        synchronized(runtime.lines) {
            runtime.lines.addAll(text.split('\n'))
            while (runtime.lines.size > 800) runtime.lines.removeAt(0)
        }
        publish()
    }

    private fun publish() {
        _sessions.value = orderedIds.mapNotNull { id ->
            val r = runtimes[id] ?: return@mapNotNull null
            val snapshot = synchronized(r.lines) { r.lines.toList() }
            TerminalSessionUi(r.id, r.title, r.workingDir.absolutePath, snapshot, r.activeProcess?.isAlive == true, r.history.toList())
        }
    }

    private fun tokenizeArgs(input: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuote = false
        var quoteChar = '"'
        for (ch in input) {
            when {
                inQuote && ch == quoteChar -> inQuote = false
                !inQuote && (ch == '"' || ch == '\'') -> { inQuote = true; quoteChar = ch }
                !inQuote && ch.isWhitespace() -> if (current.isNotEmpty()) { result += current.toString(); current.setLength(0) }
                else -> current.append(ch)
            }
        }
        if (current.isNotEmpty()) result += current.toString()
        return result
    }
}
