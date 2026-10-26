package com.amasesoft.tabatatimer

import androidx.compose.ui.window.ComposeUIViewController
import com.amasesoft.tabatatimer.di.AppContainer
import com.amasesoft.tabatatimer.di.AppVersion
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** Process-wide container: created once, on first use. */
private val appContainer: AppContainer by lazy { AppContainer(documentsDirectory(), appVersion()) }

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

private fun appVersion(): AppVersion {
    val info = NSBundle.mainBundle.infoDictionary
    return AppVersion(
        name = info?.get("CFBundleShortVersionString") as? String ?: "",
        build = info?.get("CFBundleVersion") as? String ?: "",
    )
}

fun MainViewController() = ComposeUIViewController { App(appContainer) }
