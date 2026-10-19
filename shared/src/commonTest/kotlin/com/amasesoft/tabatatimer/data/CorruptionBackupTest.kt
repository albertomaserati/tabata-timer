package com.amasesoft.tabatatimer.data

import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class CorruptionBackupTest {

    @Test
    fun corruptedFileIsCopiedToBak() {
        val fs = FileSystem.SYSTEM
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "tabata-test-${Random.nextLong().toULong()}"
        fs.createDirectories(dir)
        try {
            val file = dir / "workouts.json"
            fs.write(file) { writeUtf8("{not json") }
            backupCorruptedFile(file, fs)
            assertEquals("{not json", fs.read("$file.bak".toPath()) { readUtf8() })
            // A second corruption overwrites the previous backup.
            fs.write(file) { writeUtf8("again") }
            backupCorruptedFile(file, fs)
            assertEquals("again", fs.read("$file.bak".toPath()) { readUtf8() })
        } finally {
            fs.deleteRecursively(dir)
        }
    }
}
