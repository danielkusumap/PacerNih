package com.daniel.runcoach.engine.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Single JSON encoding for workouts, used for template storage and session snapshots. */
object WorkoutJson {

    val json: Json = Json {
        prettyPrint = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    fun encode(workout: Workout): String = json.encodeToString(workout)

    fun decode(text: String): Workout = json.decodeFromString(text)
}
