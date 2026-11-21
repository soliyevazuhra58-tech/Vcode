package com.vcode.ide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vcode.ide.data.git.GitRepository
import com.vcode.ide.data.terminal.TerminalManager
import com.vcode.ide.ui.VCodeApp
import com.vcode.ide.ui.VCodeViewModel

class MainActivity : ComponentActivity() {
    private lateinit var viewModelRef: VCodeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
            VCodeApp(viewModel = vm)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::viewModelRef.isInitialized) viewModelRef.flushAllUnsavedTabs()
    }
}
