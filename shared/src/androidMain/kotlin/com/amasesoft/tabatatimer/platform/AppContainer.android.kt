package com.amasesoft.tabatatimer.platform

import android.content.Context
import com.amasesoft.tabatatimer.di.AppContainer
import java.io.File

@Volatile
private var instance: AppContainer? = null

/** Process-wide [AppContainer]; survives Activity re-creation. */
fun appContainer(context: Context): AppContainer = instance ?: synchronized(AppContainer::class) {
    instance ?: AppContainer(
        File(context.applicationContext.filesDir, "datastore").apply { mkdirs() }.absolutePath,
    ).also { instance = it }
}
