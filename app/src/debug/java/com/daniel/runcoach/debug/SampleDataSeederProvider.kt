package com.daniel.runcoach.debug

/** Debug builds get the real seeder; release builds never compile this file. */
fun provideSampleDataSeeder(): SampleDataSeeder = DebugSampleDataSeeder
