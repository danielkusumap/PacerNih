package com.daniel.runcoach.ui.editor

/** Parses `m:ss` (e.g. "6:30") to seconds; null when unparsable. */
internal fun parsePace(text: String): Int? {
    val parts = text.trim().split(":")
    return when (parts.size) {
        1 -> parts[0].toIntOrNull()
        2 -> {
            val m = parts[0].toIntOrNull() ?: return null
            val s = parts[1].toIntOrNull() ?: return null
            m * 60 + s
        }

        else -> null
    }
}

internal fun formatPace(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

/** Save is allowed only with a name and no validation errors (P2.2 AC). */
internal fun editorCanSave(name: String, errorCount: Int): Boolean = name.isNotBlank() && errorCount == 0
