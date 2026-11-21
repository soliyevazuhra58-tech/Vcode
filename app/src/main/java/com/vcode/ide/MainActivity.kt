package com.vcode.ide

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vcode.ide.data.git.GitRepository
import com.vcode.ide.data.terminal.TerminalManager
import com.vcode.ide.ui.VCodeApp
import com.vcode.ide.ui.VCodeViewModel

class MainActivity : ComponentActivity() {
    private lateinit var viewModelRef: VCodeViewModel
    private var pendingShortcutAction by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingShortcutAction = intent?.getStringExtra("vcode_shortcut")

        val app = application as VCodeApplication
        val gitRepository = GitRepository()
        val terminalManager = TerminalManager(applicationContext, gitRepository)

        setContent {
            val vm: VCodeViewModel = viewModel(
                factory = VCodeViewModel.Factory(
                    projectRepository = app.projectRepository,
                    settingsRepository = app.settingsRepository,
                    terminalManager = terminalManager,
                    gitRepository = gitRepository
                )
            )
            viewModelRef = vm
            VCodeApp(
                viewModel = vm,
                shortcutAction = pendingShortcutAction,
                onShortcutHandled = { pendingShortcutAction = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingShortcutAction = intent.getStringExtra("vcode_shortcut")
    }

    override fun onPause() {
        super.onPause()
        if (::viewModelRef.isInitialized) viewModelRef.flushAllUnsavedTabs()
    }
}
