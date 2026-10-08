package com.zhehr.auralis

import android.app.Application
import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.zhehr.auralis.data.AuralisDatabase
import com.zhehr.auralis.data.LibraryRepository
import com.zhehr.auralis.data.MediaScanner
import com.zhehr.auralis.data.SettingsStore
import com.zhehr.auralis.playback.PlayerController
import com.zhehr.auralis.ui.components.ArtworkLoader

class AuralisApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Hand-wired dependencies. Hilt can replace this later without touching the screens' logic. */
class AppContainer(context: Context) {
    private val app = context.applicationContext
    val db = AuralisDatabase.create(app)
    val repo = LibraryRepository(db)
    val scanner = MediaScanner(app, db)
    val settings = SettingsStore(app)
    val player = PlayerController(app, repo, settings)
    val artwork = ArtworkLoader(app)
}

val LocalContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }
