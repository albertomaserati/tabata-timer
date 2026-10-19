package com.amasesoft.tabatatimer.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

internal val StorageJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    coerceInputValues = true
}

/** DataStore serializer storing a single `@Serializable` object as JSON. */
internal class JsonOkioSerializer<T>(
    private val serializer: KSerializer<T>,
    override val defaultValue: T,
) : OkioSerializer<T> {

    override suspend fun readFrom(source: BufferedSource): T = try {
        StorageJson.decodeFromString(serializer, source.readUtf8())
    } catch (e: SerializationException) {
        throw CorruptionException("Cannot decode ${serializer.descriptor.serialName}", e)
    } catch (e: IllegalArgumentException) {
        throw CorruptionException("Cannot decode ${serializer.descriptor.serialName}", e)
    }

    override suspend fun writeTo(t: T, sink: BufferedSink) {
        sink.writeUtf8(StorageJson.encodeToString(serializer, t))
    }
}

/**
 * Creates a JSON-backed DataStore in [directory]. Must be called once per file per process
 * (DataStore requires a single active instance per file): [com.amasesoft.tabatatimer.di.AppContainer]
 * guarantees it.
 */
internal fun <T> createJsonDataStore(
    directory: String,
    fileName: String,
    serializer: KSerializer<T>,
    defaultValue: T,
): DataStore<T> {
    val path = "$directory/$fileName".toPath()
    return DataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = JsonOkioSerializer(serializer, defaultValue),
            producePath = { path },
        ),
        // An unreadable file must not brick the app: keep a copy for diagnosis/recovery,
        // then fall back to defaults.
        corruptionHandler = ReplaceFileCorruptionHandler {
            backupCorruptedFile(path)
            defaultValue
        },
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    )
}

/** Copies a corrupted file to `<name>.bak` (overwriting an older backup); best effort. */
internal fun backupCorruptedFile(path: Path, fileSystem: FileSystem = FileSystem.SYSTEM) {
    runCatching {
        val backup = "$path.bak".toPath()
        fileSystem.delete(backup, mustExist = false)
        fileSystem.copy(path, backup)
    }
}

/** Read errors (I/O, not corruption) fall back to [default] instead of crashing the collector. */
internal fun <T> Flow<T>.orDefaultOnIoError(default: () -> T): Flow<T> = catch { e ->
    if (e is IOException) emit(default()) else throw e
}
