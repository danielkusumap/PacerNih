package com.daniel.runcoach.debug

import android.content.Context

/**
 * Debug-only sample data (B1). Auto-seeding is disabled; the data is only filled on demand from S15. The real
 * implementation lives in `src/debug/`; release builds only ever see [NoopSampleDataSeeder].
 */
interface SampleDataSeeder {

    /** Fills the sample data (S15 "Isi data contoh"). */
    suspend fun fill(context: Context)

    /** Removes only rows whose id starts with `mock_`; real user data is untouched. */
    suspend fun clearSample(context: Context)
}

/** Release/no-op implementation: does nothing. */
object NoopSampleDataSeeder : SampleDataSeeder {
    override suspend fun fill(context: Context) = Unit
    override suspend fun clearSample(context: Context) = Unit
}
