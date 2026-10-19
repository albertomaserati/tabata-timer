package com.amasesoft.tabatatimer.domain

/** Formats seconds as `m:ss`, or `h:mm:ss` from one hour on. */
fun formatSeconds(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    return if (hours > 0) {
        "$hours:${minutes.pad2()}:${seconds.pad2()}"
    } else {
        "$minutes:${seconds.pad2()}"
    }
}

private fun Int.pad2(): String = toString().padStart(2, '0')
