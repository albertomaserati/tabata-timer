package com.amasesoft.tabatatimer

import androidx.compose.ui.window.ComposeUIViewController
import com.amasesoft.tabatatimer.di.AppContainer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** Process-wide container: created once, on first use. */
private val appContainer: AppContainer by lazy { AppContainer(documentsDirectory()) }

@OptIn(ExperimentalForeignApi::class)
private fun documentsDirectory(): String {
    val url = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return requireNotNull(url?.path) { "Documents directory not available" }
}

fun MainViewController() = ComposeUIViewController { App(appContainer) }
