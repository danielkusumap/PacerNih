# PROGRESS

Catatan kemajuan per fase. Format: apa yang jadi, apa yang belum, bug yang diketahui.

## P0.1 Setup proyek — selesai (2026-10-10)
Yang jadi:
- Proyek multi-modul: `:app` (Android, Compose) + `:engine` (Kotlin/JVM murni, tanpa Android).
- `:engine` memakai plugin JVM + kotlinx.serialization, JVM toolchain 25.
- `:app` memakai built-in Kotlin AGP 9 + plugin Compose; Material 3, Navigation Compose, lifecycle, activity-compose.
- Version catalog `gradle/libs.versions.toml` terisi (AGP 9.5.0-alpha09, Kotlin 2.4.21, Compose BOM 2026.09.00, dst).
- `MainActivity` + `RunCoachTheme` minimal sebagai bukti Compose berjalan.
- `docs/PROGRESS.md`, `docs/DECISIONS.md`, `docs/OPEN_QUESTIONS.md` dibuat.
- AC hijau: `gradlew :engine:test :app:assembleDebug`.

Belum: -

## P0.2 Domain model dan validasi workout — selesai (2026-10-10)
Yang jadi:
- `Workout`, `Block` (Segment/Repeat), `Limit` (Distance/Duration/Open), `SegmentKind`.
- Serialisasi JSON (`WorkoutJson`, diskriminator `type`); round-trip untuk kelima contoh (bagian 5).
- `WorkoutValidator` dengan kode error spesifik (`WorkoutErrorCode`) dan path yang menunjuk lokasi masalah.
- `WorkoutExpander`: `expandFinite`, `stepCountOrNull`, `lazySteps` (untuk `untilTotalDistanceM` yang diekspansi malas).
- 27 unit test hijau: WorkoutSerializationTest (5), WorkoutValidatorTest (16), WorkoutExpanderTest (6).

Belum: -

## P0.3 Filter sample GPS — selesai (2026-10-10)
Yang jadi:
- `EngineConfig` (data class) sebagai satu-satunya rumah angka ambang; berisi konstanta 6.1 yang dipakai sejauh ini.
- `LocationSample` (model) dan `haversineM` (gps) sebagai fungsi jarak bersama.
- `SampleFilter` (6.2): buang sample dengan akurasi > `maxAccuracyM`, timestamp tidak naik, atau kecepatan tersirat > `maxSpeedMps`; `droppedCount` tersedia untuk statistik kualitas GPS.
- 13 unit test baru (SampleFilterTest 10, HaversineTest 3). Total engine 40 test hijau.

Catatan: aturan jitter diam (F3 di 6.14) belum diimplementasikan karena 6.14 belum ada.
Bug diketahui: -
