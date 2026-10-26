package com.amasesoft.tabatatimer.platform

import android.content.Context
import com.amasesoft.tabatatimer.di.AppContainer
import com.amasesoft.tabatatimer.di.AppVersion
import java.io.File

@Volatile
private var instance: AppContainer? = null

/** Process-wide [AppContainer]; survives Activity re-creation. */
fun appContainer(context: Context): AppContainer = instance ?: synchronized(AppContainer::class) {
    instance ?: AppContainer(
        File(context.applicationContext.filesDir, "datastore").apply { mkdirs() }.absolutePath,
        appVersion(context),
    ).also { instance = it }
}

private fun appVersion(context: Context): AppVersion {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    return AppVersion(
        name = info.versionName.orEmpty(),
        build = info.longVersionCode.toString(), // API 28+, matches minSdk
    )
}
