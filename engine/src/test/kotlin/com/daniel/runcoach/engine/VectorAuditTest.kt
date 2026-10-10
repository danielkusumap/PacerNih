package com.daniel.runcoach.engine

import com.daniel.runcoach.engine.gps.FormulaVectorsTest
import com.daniel.runcoach.engine.runner.WorkoutRunnerTest
import com.daniel.runcoach.engine.voice.PhraseBuilderTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Audit for the 6.14.2 formula vectors (task P0.11). It reflects over the vector tests and fails if any
 * vector that belongs to phase P0 lacks a test named `vNN_...`.
 *
 * Per the owner's decision on Q-008, the audit is scoped to the P0 vectors for now: V2–V15 and V20–V22.
 * V1 (display formatting, P1.7), V16–V17 (weekly stats, P2.10), V18 (default session name, P2.5), and
 * V19 (distance calibration, P3.7) join this set when their tasks land.
 */
class VectorAuditTest {

    private val p0Vectors: Set<String> =
        (2..15).map { "V%02d".format(it) }.toSet() + setOf("V20", "V21", "V22")

    @Test
    fun `every P0 vector has a test`() {
        val vectorTestClasses = listOf(
            FormulaVectorsTest::class.java,
            PhraseBuilderTest::class.java,
            WorkoutRunnerTest::class.java,
        )
        val pattern = Regex("^v(\\d{2})_")
        val found = vectorTestClasses
            .flatMap { it.declaredMethods.toList() }
            .mapNotNull { pattern.find(it.name)?.groupValues?.get(1)?.let { id -> "V$id" } }
            .toSet()

        val missing = p0Vectors - found
        assertTrue(missing.isEmpty(), "vectors without a test: ${missing.sorted()}")
    }
}
