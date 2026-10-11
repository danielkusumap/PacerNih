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

/** F2: moves an item within a list (top level, or a Repeat's children). Returns the same list when out of range. */
internal fun <T> moveItem(list: List<T>, from: Int, to: Int): List<T> {
    if (from !in list.indices || to !in list.indices || from == to) return list
    val result = list.toMutableList()
    val moved = result.removeAt(from)
    result.add(to, moved)
    return result
}

/** F2: duplicates a Repeat block (children copied) right below the original. */
internal fun duplicateRepeat(repeat: com.daniel.runcoach.engine.model.Repeat): com.daniel.runcoach.engine.model.Repeat =
    repeat.copy(children = repeat.children.map { it.copy() })

/** Hand-off for the editor route: the workout to edit, or null for a new one. */
object EditorRequestHolder {
    var initial: com.daniel.runcoach.engine.model.Workout? = null
}
