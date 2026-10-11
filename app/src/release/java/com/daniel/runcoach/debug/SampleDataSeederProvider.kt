package com.daniel.runcoach.debug

/** Release builds get the no-op seeder; the debug source set provides the real one. */
fun provideSampleDataSeeder(): SampleDataSeeder = NoopSampleDataSeeder
