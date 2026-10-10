# PROGRESS

Catatan kemajuan per fase. Format: apa yang jadi, apa yang belum, bug yang diketahui.

## Status P0 (diperbarui 2026-10-10, sinkron dengan Lampiran C)
- **Selesai & ter-commit:** P0.1, P0.2, P0.3, P0.4, P0.4b, P0.5, P0.6, P0.7, P0.8, P0.9, P0.10, P0.11.
- **Gerbang P0 tercapai** (semua AC P0 hijau). P1 (runtime Android) boleh dimulai.
  Catatan: P0.11 dibatasi ke vektor milik P0 (Q-008); F7 & KmMarker live menyusul (D-013).
- **Tersedia sekarang:** 6.14 (Katalog Rumus + V1–V22) dan 8.3 (Info Segmen).
- **Repo:** Git di-init (`main`), `.gitignore` + `.gitattributes` (LF). Q-007 (remote) masih open.
- **Versi:** tech stack menuntut versi **stabil**; AGP `9.5.0-alpha09` masih alpha dari template — alasannya di `DECISIONS.md` D-010.
- **Test:** `:engine` 159 unit test hijau; coverage LINE **93.5%** (gate 85%); `:app:assembleDebug` sukses.
- **Berikutnya:** FASE P1 — P1.2 (`LocationSource`); P1.1 sudah selesai (lihat bawah).

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

## P0.5 PaceCalculator — selesai (2026-10-10)
- `PaceCalculator` (pure): konversi speed<->pace (F1), pace rata-rata (F9), lingkup evaluasi (F10),
  projected pace (F11), required pace + aturan "tidak realistis" (F12).
- `LivePaceEstimator` (stateful): pace live jendela 15 dtk (F8) — Metode A Doppler berbobot waktu,
  Metode B fallback jarak/waktu dengan interpolasi; kecepatan < 0.3 m/s = berhenti (pace null).
- `roundHalfUp` ditaruh bersama di `engine/Rounding.kt` (6.14.0).
- Test: **85 hijau**. Vektor V5, V6, V7, V8 masuk `FormulaVectorsTest`; PaceCalculatorTest (7) dan
  LivePaceEstimatorTest (5) termasuk uji stabilitas terhadap noise.
- Catatan desain: bobot pace live & pemakaian ulang ambang 150 s/km — lihat `DECISIONS.md` D-012.

Belum: P0.6 dst.
Bug diketahui: -

## P0.6 WorkoutRunner (state machine) + countdown — selesai (2026-10-10)
- `RunnerPhase` (IDLE..STOPPED) + `RunnerState`; keluaran `StateFlow<RunnerState>` + `Flow<AlertEvent>`
  (plus daftar `emittedEvents` untuk tes/log S15).
- State machine 6.5: IDLE -> WAITING_GPS -> COUNTDOWN -> RUNNING <-> PAUSED -> STOPPED. **Tidak pernah
  berhenti otomatis**; setelah rencana selesai masuk segmen `Open` sampai Stop.
- `StepCursor` (F27): repeat `times` + `untilTotalDistanceM` (cek setelah tiap child), plus `peek`/`hasNext`.
- Transisi segmen jarak via interpolasi (F6) dan waktu via tick; moving clock (F5) dipasok ke `DistanceTracker`;
  pause/resume (waktu tidak jalan), skip, extend, stop. Sisa jarak (`D1-B`) terbawa ke segmen berikutnya.
- Countdown & cue (F15): countdown awal N..1, pre-warning (>=30 dtk), countdown 3-2-1 segmen waktu, cue jarak
  tepat sekali; tidak ada cue untuk segmen < 2xC atau tanpa segmen berikutnya.
- `GoalDistanceReached` sekali; `WorkoutPlanCompleted` lalu segmen Open; GPS lost/recovered; KmMarker.
- Test: **101 hijau**. Vektor V21, V22 hijau (WorkoutRunnerTest).
- Penyimpangan sementara yang dicatat: pemisahan jarak proporsional F7 pada batas waktu dan waktu KmMarker live
  (non-interpolasi) — lihat `DECISIONS.md` D-013.

Belum: P0.8 dst.
Bug diketahui: pemisahan jarak proporsional F7 (batas waktu) belum diimplementasikan (D-013).

## P0.8 SegmentEvaluator + KmSplitCalculator — selesai (2026-10-10)
- `SegmentEvaluator` (6.8, F17): penilaian memakai pace dibulatkan (`roundHalfUp`), status PASS/BORDERLINE/
  FAIL/NO_TARGET/NO_DATA/DILEWATI, selisih `deltaSec`, margin GPS `marginSec`/`marginKmSec`, cek per-km.
- `KmSplitCalculator` (6.9, F18): baris km dari kurva jarak workout dengan interpolasi; baris sisa (>=10 m),
  pace sisa null bila <50 m; status diberi hanya bila jendela km sepenuhnya di satu segmen bertarget;
  km tercepat disorot.
- Test: **116 hijau**. Vektor V9, V10, V11, V12 hijau (FormulaVectorsTest).
- Catatan: `marginSec` diekspos sebagai fungsi murni agar V11 (margin terbatas, segmen 40 m) bisa diuji —
  lihat `DECISIONS.md` D-014.

Belum: P0.7 dst.
Bug diketahui: pemisahan jarak proporsional F7 (batas waktu) & KmMarker live masih non-interpolasi (D-013).

## P0.7 AlertEngine dan PhraseBuilder — selesai (2026-10-10)
- `PhraseBuilder` (6.7, F20): teks ID & EN untuk semua jenis event; satuan pace/waktu/jarak mengikuti F20
  (F20 menang atas tabel ilustrasi 6.7). Termasuk V20.
- `AlertEngine` (6.7, F13/F14): status STABLE/TOO_SLOW/TOO_FAST dengan gerbang F14, hysteresis 3 dtk/km,
  hold 5 dtk, cooldown ulang 25 dtk, dan BackToStable sekali. `AlertPriority` memberi urutan prioritas 6.7
  (Countdown tertinggi; SegmentEnd mengalahkan TooSlow).
- Test: **137 hijau**. Vektor V13, V14, V15 (best effort) juga ditambahkan. AlertEngineTest menutup skenario
  (a)-(h) AC.
- Cakupan & sisa vektor: lihat `DECISIONS.md` D-015 dan `OPEN_QUESTIONS.md` Q-008.

Belum: P0.9 dst.
Bug diketahui: (seperti P0.8) F7 & KmMarker live (D-013); antrean suara TTS di service P1.4.

## P0.9 TrackSimulator dan golden test — selesai (2026-10-10)
- `TrackSimulator` (P0.9): generator track sintetis deterministik (seed tetap, 1 Hz) untuk 8 skenario
  (`STEADY`, `SLOW_DOWN`, `SPEED_UP`, `INTERVALS`, `GPS_JUMP`, `STOP_AND_GO`, `TUNNEL_GAP`, `NOISY`).
- **14 golden test** (`GoldenTest`): workout x skenario -> ringkasan event tersimpan di
  `engine/src/test/resources/golden/*.txt`. Regenerasi lewat env `RC_UPDATE_GOLDEN=1`.
- Coverage diukur dengan plugin `jacoco` bawaan Gradle (tanpa dependency eksternal); gate **LINE >= 85%**,
  hasil saat ini **93.5%** (976/1044 baris). `:engine:test` ~10 detik (< 30 detik).
- Test: **158 hijau**.
- Keputusan teknis: lihat `DECISIONS.md` D-016.

Belum: P0.11 dst.
Bug diketahui: (seperti P0.8) F7 & KmMarker live (D-013).

## P0.11 Audit vektor rumus — selesai (2026-10-10)
- `VectorAuditTest` (refleksi atas nama test `vNN_...`) gagal bila ada vektor milik P0 yang belum punya test.
- Cakupan P0 (keputusan Q-008): **V2–V15, V20–V22** — semuanya hijau. V1, V16–V19 menyusul di task-nya
  (P1.7 / P2.10 / P2.5 / P3.7).
- Test: **159 hijau**.

## Gerbang P0 — TERCAPAI (2026-10-10)
Semua task P0 selesai dan AC hijau; boleh lanjut ke FASE P1. Sisa yang sifatnya lintas-fase:
- Pemisahan jarak proporsional F7 (batas waktu) & `KmMarker` live non-interpolasi (D-013).
- Antrean suara TTS (flush Countdown, buang alert basi >5 dtk) & tick countdown >700 ms → service P1.4.

## P1.1 Izin, Onboarding (S01), Panduan Baterai (S14) — selesai (2026-10-10)
- `:app` kini memakai Navigation Compose; start = S01, plus S14 dan placeholder `home` (P1.7).
- **S01**: status izin lokasi & notifikasi (diberikan/ditolak) + status GPS perangkat, penjelasan singkat tiap izin,
  aksi beri izin / buka pengaturan sistem / buka pengaturan lokasi, tombol **Mulai** hanya aktif bila lokasi diberikan.
- **S14**: langkah per merek (Xiaomi/OPPO/Vivo/Samsung/lainnya) + tombol ke pengaturan baterai & aplikasi,
  menampilkan status pengecualian optimasi baterai bila bisa dideteksi.
- Izin runtime: `ACCESS_FINE/COARSE_LOCATION` + `POST_NOTIFICATIONS` (API 33+). **Tidak** meminta
  `ACCESS_BACKGROUND_LOCATION`. Teks UI di `strings.xml` (values=ID, values-en=EN), `@Preview` light & dark.
- AC P1.1 terpenuhi (perilaku izin; tidak ada jalur crash karena denial). Detail: `DECISIONS.md` D-017.
- Verifikasi: `:app:assembleDebug` sukses.

