# PROGRESS

Catatan kemajuan per fase. Format: apa yang jadi, apa yang belum, bug yang diketahui.

## Status P0 (diperbarui 2026-10-10, sinkron dengan Lampiran C)
- **Selesai & ter-commit:** P0.1, P0.2, P0.3, P0.4, P0.4b, P0.10.
- **Tersedia sekarang:** 6.14 (Katalog Rumus + V1–V22) dan 8.3 (Info Segmen). Q-001/Q-002/Q-003/Q-005 ditutup.
- **Urutan yang disarankan (Lampiran C):** P0.4b -> P0.5 -> P0.6 -> P0.8 -> P0.7 -> P0.9 -> P0.11.
- **Belum:** P0.5, P0.6, P0.7, P0.8, P0.9, P0.11.
- **Gerbang P0 belum tercapai**, jadi P1 (runtime Android) belum dimulai.
- **Repo:** Git di-init (`main`), `.gitignore` + `.gitattributes` (LF). Q-007 (remote) masih open.
- **Versi:** tech stack kini menuntut versi **stabil**; AGP `9.5.0-alpha09` masih alpha dari template — alasannya dicatat di `DECISIONS.md` D-010.
- **Test:** `:engine` 69 unit test hijau; `:app:assembleDebug` sukses.

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

Catatan: aturan jitter diam (F3) dan reset referensi belum ada di versi ini; diperbaiki di **P0.4b** (6.14 kini tersedia).
Bug diketahui: -

## P0.4 DistanceTracker dan interpolasi batas — selesai (2026-10-10)
Yang jadi:
- `DistanceTracker` (6.3): akumulasi jarak Haversine antar-sample, batas jarak kumulatif, interpolasi waktu
  `t = t1 + (boundary - d1)/(d2 - d1) * (t2 - t1)`, banyak batas dalam satu sample (loop), sisa jarak
  (overshoot) tetap di `totalDistanceM` sehingga terbawa ke segmen berikutnya.
- Gap GPS tidak butuh perlakuan khusus di tracker: loncatan tidak masuk akal sudah dibuang `SampleFilter`,
  loncatan wajar (terowongan) dihitung sebagai garis lurus.
- 9 unit test baru (DistanceTrackerTest). Total engine 49 test hijau.

Belum: -
Bug diketahui: -

## P0.10 BestEffortCalculator — selesai (2026-10-10)
Catatan urutan: P0.5 (PaceCalculator) dan P0.7-P0.9, P0.11 ditunda karena bergantung pada 6.14 (Q-001),
maka dilanjutkan ke P0.10 yang mandiri.
Yang jadi:
- `BestEffortKey` (K1/K5/K10/HM/FM), `DistTimePoint`, `BestEffort`.
- `BestEffortCalculator.compute` (6.10): sliding window dua-pointer O(n) atas kurva jarak kumulatif/waktu
  berjalan, interpolasi titik awal window, memakai waktu berjalan (bukan wall clock).
- Effort lebih cepat dari `recordMinValidPaceSecPerKm` (anomali GPS) dibuang.
- 7 unit test baru (BestEffortCalculatorTest). Total engine 56 test hijau.

Belum: dilanjutkan sesuai urutan P0.4b -> P0.5 -> P0.6 -> P0.8 -> P0.7 -> P0.9 -> P0.11 (6.14 & 8.3 kini tersedia).
Bug diketahui: -

## P0.4b Retrofit kode ke 6.14 — selesai (2026-10-10)
- `SampleFilter` (F3) kini mengembalikan `FilteredSample(outcome, stepM)` dengan empat hasil: **MOVING**,
  **STATIONARY** (jitter: speed < 0.3 dan d < 5 m → tanpa jarak, referensi tidak bergeser), **RESET**
  (referensi di-reset setelah 3 penolakan kecepatan berturut-turut, F3.7), **REJECTED**. Sample pertama
  jadi referensi tanpa jarak (F3.6); gap wajar > 10 dtk tetap dihitung garis lurus (F3.8).
- `DistanceTracker`: `add(mtMs, filtered)` menerima **moving clock** dari pemanggil (F5/F6) dan jarak
  mentah dari filter, menerapkan `calibrationFactor` (F4, clamp 0.95–1.05). Sisa jarak = `D1 - B` (F6).
- `haversineM` memakai `R = 6371008.8` (F2) — diverifikasi vektor V2.
- `EngineConfig` (C.2): tambah `stationarySpeedMps`, `stationaryMaxDispM`, `speedRejectResetCount`,
  `calibrationMin`/`_MAX`.
- Test: **69 hijau**. Vektor V2, V3, V4 masuk `FormulaVectorsTest`; test jitter diam, reset referensi,
  gap, dan kalibrasi ditambahkan. Test P0.3/P0.4 lama diperbarui ke API baru (lihat `DECISIONS.md` D-011).
- D-007 (filter & tracker terpisah) dipertahankan.

Belum: P0.5 dst.
Bug diketahui: -

