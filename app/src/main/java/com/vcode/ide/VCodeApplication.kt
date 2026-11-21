package com.vcode.ide

import android.app.Application
import com.vcode.ide.data.local.AppDatabase
import com.vcode.ide.data.local.SettingsRepository
import com.vcode.ide.data.project.ProjectRepository
import com.vcode.ide.ui.editor.TextMateSetup

class VCodeApplication : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val settingsRepository by lazy { SettingsRepository(this) }
    val projectRepository by lazy { ProjectRepository(this, database.projectDao()) }

    override fun onCreate() {
        super.onCreate()
        TextMateSetup.init(this)
    }
}
