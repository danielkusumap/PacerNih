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
- **Berikutnya:** FASE P3 step by step — P3.1–P3.3 selesai; selanjutnya P3.4 (Export GPX).

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

## P1.2 LocationSource — selesai (2026-10-10)
- `LocationSource` (interface) + `FusedLocationSource` (Play Services, 1 Hz, `PRIORITY_HIGH_ACCURACY`) +
  fallback `AndroidLocationSource` (`LocationManager.GPS_PROVIDER`, 1 Hz); `LocationSourceFactory` memilih
  berdasar ketersediaan Play Services.
- Konversi `android.location.Location` -> `LocationSample` (`elapsedRealtimeNanos`, `speed`, `speedAccuracyMetersPerSecond`).
- Helper engine `gpsFixQuality` (S04: SEARCHING / READY / WEAK, berbasis `GOOD_FIX_ACCURACY_M`) + test.
- Dependency: `play-services-location` 21.4.0 (whitelist) dan `:app` kini bergantung pada `:engine` (D-018).
- Verifikasi: `:app:assembleDebug` sukses; `:engine` **162** test hijau.
- **Belum diuji di perangkat nyata** (AC "sampel ~1 Hz" butuh device).

## P1.3 RunForegroundService — selesai (2026-10-10)
- `RunForegroundService` (tipe `location`): memegang `WorkoutRunner` + `LocationSource`, partial wake lock,
  notifikasi ongoing (durasi, jarak, pace, label segmen) diperbarui tiap detik, aksi **Stop** di notifikasi.
  Berhenti hanya saat Stop (START_NOT_STICKY, tidak auto-restart).
- Izin ditambah: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `WAKE_LOCK`.
- `Formatters` (aturan #14, 6.13): pace `m:ss`, durasi `mm:ss`/`h:mm:ss`, jarak km 2 desimal (locale), delta bertanda.
- Layar placeholder `home` punya tombol Mulai/Stop sesi uji (sementara sampai S02/P2.3) agar AC bisa diuji manual.
- **Uji device** (Realme RMX1971, Android 11 / API 30, via adb): service `isForeground=true`, notifikasi update
  tiap detik (title = label segmen, text "durasi · jarak · pace"). **Layar mati 5 menit**: durasi 00:20 → 05:27,
  jarak 0.10 km, service tetap hidup (tidak dibunuh OEM); aksi **Stop** menghentikan service & menghapus notifikasi.
  Catatan: device ter-charge USB saat uji (uji tanpa charge lebih ketat, menyusul).
- Detail: `DECISIONS.md` D-019.

## P1.4 Voice, beep, haptic — selesai (2026-10-11)
- `TtsSpeaker` (TTS + audio focus `GAIN_TRANSIENT_MAY_DUCK`, queue tunggal, `QUEUE_FLUSH` untuk Countdown,
  fallback English bila voice ID tidak ada), `BeepPlayer` (`ToneGenerator`: tick & end tone),
  `Haptics` (pola countdown / ganti segmen / terlalu cepat / lambat).
- `RunAnnouncer`: `AlertEvent` -> teks (`PhraseBuilder`) + beep + getar; gaya countdown beep/suara; bahasa ID/EN
  (default BEEP & ID; disambung ke S12/P2.11). Di-wire ke `RunForegroundService` (collect `runner.events`).
- Payload event diperkaya: `PreSegmentWarning(nextKind)`, `KmMarker(km, timeSec, paceSecPerKm)` (D-022).
- Izin `VIBRATE` ditambahkan ke manifest (tanpa itu proses crash — ketemu & diperbaiki saat uji device).
- **Uji device (Realme/Android 11):** service foreground, notifikasi update, engine TTS ter-load, **tanpa crash**;
  Stop menghentikan service. Audibilitas (ducking/getar) belum bisa diverifikasi via adb.
- Belum: wiring `AlertEngine` (TooSlow/TooFast/BackToStable) ke announcer; teks `SegmentEnd` (butuh hasil evaluator).

## P1.5 Persistensi sesi dan pemulihan — selesai (2026-10-11)
- Room 2.8.5 + KSP 2.3.12 di `:app`: `RunDatabase` (`exportSchema=true`, skema di `app/schemas`), entitas
  `TrackPoint` (idx, diindeks per `sessionId`, tanpa FK) dan `ActiveRunSnapshot` (id=1).
- `RunPersistence`: snapshot + batch track point ditulis tiap ~5 dtk dari service; dibersihkan saat Stop.
- **Pemulihan berbasis replay**: resume membangun `WorkoutRunner` baru lalu memutar ulang semua track point
  (deterministik) → jarak & progres segmen pulih, lalu lanjut dengan sample live (D-023).
- D04 dialog di layar Home: mendeteksi snapshot → **Lanjutkan / Buang**.
- **Uji device**: jalan → **kill proses** → buka lagi → D04 muncul → Resume → service foreground, notifikasi
  lanjut (`00:06` → `00:36`, jarak terbawa), tanpa crash.
- Catatan: moving time saat resume ikut menghitung jeda proses mati (batas metode replay, D-023).

## P1.6 Simulator in-app (S15) — selesai (2026-10-11)
- `SimulatedLocationSource` (implementasi `LocationSource`) memakai `TrackSimulator` engine.
- `SimulationController` menjalankan `WorkoutRunner` pada **clock sintetis**, kecepatan 1x/10x/60x.
- Layar **S15** (hanya build debug): pilih skenario & kecepatan, jalankan workout interval, tampilkan
  ringkasan berformat sama dengan golden test. Tombol masuk hanya muncul di build debug.
- **Uji device**: INTERVALS di **60x** → ringkasan sama persis dengan `golden/interval_intervals.txt`
  (`totalDistanceM=6375`, `movingMs=1999000`, `planCompleted=true`, hitungan event identik). **AC P1.6 terpenuhi.**

## P1.7 Layar Lari (S05) — selesai (2026-10-11)
- `RunStateHolder` (`StateFlow<RunUiState>`): jembatan in-process antara service dan UI.
- Service kini juga menjalankan **`AlertEngine`** tiap detik (menutup catatan P1.4): status koreksi + required
  pace ikut dipublikasikan, dan alert koreksi (TooSlow/TooFast/BackToStable) diteruskan ke announcer.
- `RunScreen` (S05) stateless: blok A–F (header + indikator GPS, segmen + progress + sisa, hero pace live +
  target + badge STABIL/LAMBAT/CEPAT, metrik segmen + required pace, total, preview segmen berikutnya),
  state dsb. `@Preview` untuk tiap state (Running/Too slow/Too fast/Paused/Waiting GPS/Plan completed), light & dark.
- `RunnerState` + `nextSegment`; `Formatters` dipakai untuk semua angka. Layar tetap menyala mengikuti setting
  (`keepScreenOn`, default mati sampai S12/P2.11).
- **Uji device**: mulai sesi → S05 tampil & update (header, segmen 1/1, `--:--`, total, Next, Stop), tanpa crash.
- Catatan: `elapsedScope` untuk projected/required masih didekati dari waktu segmen penuh (bukan per-km) — lihat D-025.

## P1.8 Countdown UI (S05) — selesai (2026-10-11)
- Sinyal ke UI ditambahkan ke `RunnerState`: `segmentCountdownSec` (banner 3-2-1 segmen waktu),
  `distanceCueActive` + `distanceCueNextKind` (banner "Siap-siap") — dihitung deterministik dari state.
- S05: **overlay countdown awal** (nama workout, angka besar, tombol **Batal**) saat phase `COUNTDOWN`;
  **banner** "Ganti ke X dalam N" (segmen waktu) dan "Siap-siap: ganti ke X" (cue jarak) yang tidak menutupi pace.
- Service: aksi `ACTION_CANCEL_COUNTDOWN` (`RunForegroundService.cancelCountdown`) → `runner.onCancelCountdown()`.
- `@Preview` countdown ditambahkan. **Uji device**: mulai sesi → overlay tampil (`Lari Bebas`, `3`, `Cancel`) lalu
  transisi ke RUNNING setelah 5 dtk; Stop menghentikan service.
- Catatan: karena S04 belum ada (P2.4), **Batal** mengembalikan ke phase WAITING_GPS (belum menavigasi keluar).

## P1.9 Kontrol dan sheet layar lari — selesai (2026-10-11)
- Aksi service: `ACTION_PAUSE`, `ACTION_RESUME_RUN`, `ACTION_SKIP`, `ACTION_EXTEND` (meter/detik),
  plus `ACTION_CANCEL_COUNTDOWN`.
- S05 blok H: **Jeda/Lanjut**, **Lewati segmen**, **Perpanjang** (dialog +100 m/+500 m atau +30 s/+1 mnt),
  **Daftar segmen** (D10), **Stop**. Dialog back (`BackHandler`) "Sesi masih berjalan" → Tetap lari / Stop.
- D10 memakai `steps` (hasil expand workout) di `RunUiState`; status baris: Selesai / Sedang berjalan / Belum dimulai.
- **Uji device**: Pause → PAUSED + Resume; Resume lanjut; Daftar segmen terbuka; Stop menghentikan service; tanpa crash.
- **Ditunda (cross-task, sengaja):**
  - "Stop menyimpan sesi (`Session` sebelum S06)" → butuh tabel `Session` + S06 (**P2.5**).
  - Chip hasil segmen (blok G, LULUS/GAGAL) & status hasil per baris D10 → butuh evaluasi segmen di-wire (**P2.8**).

## Gerbang P1 — SELESAI (2026-10-11)
P1.1–P1.9 selesai dan diuji di device (Realme/Android 11) secara bergantian. Sisa lintas-fase: F7/KmMarker (D-013),
dan item P1.9 yang menunggu P2.5/P2.8 (di atas). FASE P2 dikerjakan **step by step**.

## P2.1 Template workout (CRUD) + Lari Bebas bawaan — selesai (2026-10-11)
- Room `WorkoutTemplate` (tanpa FK): `WorkoutTemplateEntity` + `WorkoutTemplateDao`; database naik ke **v2** dengan
  `MIGRATION_1_2` eksplisit (tanpa destructive fallback).
- `WorkoutTemplateRepository`: create / update / duplicate / delete / markUsed + `ensureBuiltIn` (template bawaan
  **Lari Bebas** tidak bisa diedit/dihapus). `Flow<List<WorkoutTemplate>>` disiapkan untuk UI.
- **Instrumented test di device** (Realme/Android 11): CRUD + duplikat + proteksi built-in + migrasi → **2/2 hijau**.
- Menghapus template tidak menyentuh riwayat (belum ada tabel sesi; akan diverifikasi penuh di P2.6).

## P2.2 Editor Workout (S03, D01, D02, D11) — selesai (2026-10-11)
- Engine: `WorkoutEstimator` (F24) + `WorkoutPresets` (stable run, progressive tempo, interval, run/walk); unit test hijau.
- **S03**: nama + target jarak total, daftar item (Segmen + Repeat + anak menjorok), tambah/duplikat/hapus/urut,
  validasi live (`WorkoutValidator`), ringkasan (F24), Simpan ke repository, dialog "buang perubahan".
- **D01 Editor Segmen** (sheet): label, tipe, batas (Jarak/Durasi/Tanpa batas), target pace, margin terlalu cepat,
  per-km check; tombol ⓘ ke D11. **D02 Preset Generator** (sheet): 4 preset. **D11 Info** (sheet): 12 topik
  (6 tipe segmen + 6 istilah) dari `strings.xml` ID & EN.
- Engine test: **168 hijau** (+6). `:app` build sukses.
- **Uji device (parsial)**: editor terbuka; "Add segment" menambah item; sheet D01 terbuka ("Segment editor");
  Save hanya aktif bila nama terisi & tidak ada error. Verifikasi tap tombol **ⓘ** D11 via uiautomator belum tuntas
  (glyph ⓘ tidak ter-encode) → sisa uji manual.
- **Test otomatis sesuai AC** (semua hijau):
  - AC1 (D11 ID & EN dari strings): `EditorInfoContentTest` — 12 topik, teks ID & EN non-kosong (2/2).
  - AC3 (tidak bisa simpan jika tidak valid): `EditorLogicTest` (lokal) + `EditorUiTest.saveIsDisabledUntilNameIsEntered`.
  - AC2 (buat via UI): `EditorUiTest.addSegmentEnterNameAndSavePersistsTemplate` — add segmen + nama + Simpan → template tersimpan (2/2).
  - AC4/AC5 (preset lolos validator; progressive tempo 5 km 7:30 −20 dtk): `WorkoutPresetsTest` (engine).
  - Tombol ⓘ dipastikan ada lewat cek manual (glyph). Total: engine 168, app lokal 2, instrumented 7.

## P2.3 Beranda (S02) — selesai (2026-10-11)
- `BerandaScreen`: tombol **Lari Bebas** (start cepat), **daftar template** (nama, jumlah segmen, estimasi jarak/durasi,
  range target, aksi Mulai / Edit / Duplikat / Hapus), **FAB Buat workout**, **state kosong**, dan **banner
  "Sesi belum selesai"** (Lanjutkan / Buang) yang hanya muncul bila ada snapshot.
- `EditorScreen` kini mendukung **update** (tombol Edit) — Simpan memilih create vs update; `EditorRequestHolder`
  membawa Workout yang diedit ke route editor.
- **Test**: `BerandaUiTest` 3/3 (state kosong; banner absen tanpa snapshot; banner tampil dengan snapshot) +
  P2.1/P2.2 tests → **instrumented 10/10 hijau**. Engine 168, app lokal 2.
- **Uji device**: buat template lewat UI → muncul di Beranda dengan aksinya. Catatan: tombol aksi editor bisa
  tertutup keyboard saat field nama fokus (perlu scroll) — kandidat perbaikan `imePadding`.
- Kartu ringkasan minggu ini menunggu **P2.10**. **D04** (dialog) digabung jadi banner S02 (D-030).

## P2.4 Pra-start (S04) — selesai (2026-10-11)
- `PraStartScreen`: ringkasan segmen (setelah expand), **status GPS live** (`LocationSource` → `GpsFixQuality`:
  Siap ≤12 m / Mencari sinyal / Lemah), pengaturan cepat (**bahasa voice** ID/EN, **countdown** 0/3/5/10),
  tombol **Mulai** (aktif hanya bila izin lokasi ada **dan** GPS siap) atau **Tunggu GPS** + **Mulai saja**.
- Beranda kini menuju **S04** (bukan langsung start); service menerima `start_countdown_sec` dan `voice_lang`
  via intent extra (`EngineConfig.startCountdownSec` + bahasa `RunAnnouncer`).
- Test: `PraStartUiTest` 1/1 (render ringkasan/GPS/quick settings). Alur start & gating izin diuji manual di device.

## P2.5 Selesai & Simpan (S06) + nama default — selesai (2026-10-11)
- Engine: `DefaultSessionNamer` (6.12; batas jam 03:59…21:00 × ID/EN) + `SessionResultsCalculator`
  (segmen/split/best effort dari kurva jarak–waktu). Engine **173 test** hijau.
- Room **v3**: tabel `Session`, `SegmentResult`, `KmSplit`, `BestEffort` (tanpa FK; indeks `sessionId`) + `MIGRATION_2_3`.
- `SessionRepository`: simpan bundle transaksional, `updateForm` (nama kosong → default; catatan ≤500),
  `newRecords` (lencana), `delete` cascade (tanpa baris yatim).
- Service saat Stop: bangun kurva dari track point → hitung hasil → **simpan Session dulu**, baru S06; snapshot
  aktif dihapus (track point disimpan untuk riwayat).
- **S06 `FinishedScreen`**: ringkasan, lencana rekor, form nama/RPE/catatan; Simpan/Lewati/Buang; catatan >500 ditolak.
- Test: `SessionRepositoryTest` 2/2 (save, default name, limit 500, hapus tanpa yatim, migrasi v3).
- **Instrumented 13/13 hijau**; engine 173; app lokal 2.
- Catatan: P2.4 & P2.5 di-commit bersamaan karena berbagi file (service/AppNavHost/strings).

## P2.6 Riwayat (S07) + Edit Sesi (D03) — selesai (2026-10-11)
- Bottom bar (tab **Latihan**/**Riwayat**) ditambahkan; S07 & S02 memakainya (Statistik/Pengaturan menyusul).
- S07: daftar sesi (terbaru dulu), pencarian nama, header bulan (nama bulan · total km · jumlah sesi), item dengan
  chip hasil ("x/y lulus"/"Tanpa target"), aksi **Edit (D03)** & **Hapus (D09)**; kosong: "Belum ada sesi".
- D03: nama (kosong → default), RPE 1-10, catatan (≤500), toggle **kecualikan dari rekor**. Simpan via `updateForm`.
- Hapus sesi = `deleteSessionCascade` (tanpa baris yatim).
- Test: `HistoryUiTest` (list + edit/delete) hijau.

## P2.7 Komponen chart (Canvas) — selesai (2026-10-11)
- `BarChart` & `LineChart` reusable (Compose Canvas, **tanpa dependency chart**): sumbu label, **garis target/tangga**,
  **garis vertikal putus** di batas segmen, **titik sorotan**, **tap → nilai**, data kosong, nilai null (garis putus).
- `@Preview` semua varian (bar/line, normal/kosong/gap, light & dark).
- Test: `ChartsRenderTest` (render normal & kosong) hijau.

## P2.8 Detail Sesi (S08) — selesai (2026-10-11)
- Header (nama + snapshot workout, tanggal, statistik, hasil segmen, status rencana, kartu catatan/RPE) +
  tab **Segmen / Split km / Grafik**. Tab Peta menunggu **P3.1**.
- Segmen: dari `SegmentResult` (batas, target, hasil, pace, selisih, status; BORDERLINE beri keterangan).
- Split km: dari `KmSplit` (baris sisa parsial, km tercepat disorot).
- Grafik: `BarChart` pace per km (dengan garis target) + `LineChart` pace per km dengan **target tangga** & batas segmen.
- Test: `HistoryUiTest.detailShowsHeaderAndTabs` hijau.
- **Gap dicatat:** status **DILEWATI** belum dipersistensi (jalur skip runner belum menulis hasil) → menyusul;
  lencana rekor belum ditampilkan di daftar S07 (baru di S06).

## Status P2 (diperbarui 2026-10-11)
- Selesai: P2.1, P2.2, P2.3, P2.4, P2.5, P2.6, P2.7, P2.8.
- Berikutnya: P2.9 (Rekor S10/S11) → P2.10 (Ringkasan Mingguan S09) → P2.11 (Pengaturan S12).
- Instrumented **16/16 hijau**; engine 173; app lokal 2.

## P2.9 Rekor Pribadi (S10, S11) — selesai (2026-10-11)
- Engine `RecordProgression` (F22) + test (progresi waktu, sesi dikecualikan, hapus pemegang rekor menurunkan ke
  berikutnya, recent 5). Engine **177 test** hijau.
- App `RecordRepository`: `currentRecords` (min per jarak, join sesi non-excluded), `topN`, `recentRecords`, `series`.
- Tab **Statistik** (bottom bar 3 tab) sub-tab **Mingguan** (placeholder) & **Rekor (S10)**; **S11** detail rekor
  (rekor saat ini + posisi awal, grafik best effort per sesi, Top 10, toggle waktu/pace).
- Test: `RecordRepositoryTest` 2/2 (hapus menurunkan rekor; excluded diabaikan; jarak belum ditempuh → null).
- **Instrumented 18/18 hijau**.
- Gap: lencana rekor di daftar S07 belum ada (di S06 sudah).

## P2.10 Ringkasan Mingguan (S09) + kartu Beranda — selesai (2026-10-11)
- Engine `WeeklyStatsAggregator` (F21) + test: **V16** pace rata-rata berbobot jarak (5100/15=340), **V17** batas
  minggu Minggu 23:30 vs Senin 00:30 (zona Jakarta), minggu kosong (nol, pace null, 7 hari), delta vs minggu lalu.
  Engine **181 test** hijau.
- Sub-tab **Mingguan (S09)**: navigator minggu (< / > / Minggu ini), kartu angka (jarak, waktu, sesi, pace rata-rata,
  sesi terpanjang, % segmen lulus, selisih vs minggu lalu), 3 grafik (harian, 12 minggu, pace mingguan dgn garis putus),
  dan daftar sesi minggu itu (tap → S08).
- **Kartu ringkasan minggu ini** di Beranda (tap → S09).
- Perbaikan kompatibilitas: `Instant.atZone().toLocalDate()` menggantikan `LocalDate.ofInstant` (API 34) agar jalan di API 30.
- Gap: daftar "rekor minggu ini" di S09 belum ditampilkan.

## P2.11 Pengaturan (S12) — selesai (2026-10-11)
- DataStore `SettingsRepository`: tema, bahasa voice, countdown (awal/segmen/cue/gaya), margin terlalu cepat, getar,
  auto-pause, layar menyala, km marker, alert pace.
- S12: Tampilan (tema + layar menyala), Voice (bahasa, **uji suara**, **status paket suara**, km marker, alert pace),
  Countdown (awal/segmen/cue/gaya), Alert (margin, getar), Tracking (auto-pause), Data→S13, Bantuan (baterai→S14, versi),
  Pengembang→S15 (debug).
- Wiring: **tema → `RunCoachTheme`** (langsung tanpa restart, persist), **layar menyala → S05**, **bahasa voice + countdown
  awal → S04/service**. Tab **Pengaturan** (bottom bar 4 tab).
- Test: `SettingsRepositoryTest` (persist) + `SettingsUiTest` (render + status paket suara). **Instrumented 20/20 hijau**.
- Gap: sebagian setelan dipersistensi tapi wiring fungsional menyusul (auto-pause = P3.3; cue/margin/vibrate/kmMarker/
  paceAlerts belum disalurkan ke engine/service).

## Status P2 — SELESAI (2026-10-11)
- P2.1–P2.11 selesai. **Instrumented 20/20 hijau; engine 181; app lokal 2.**
- Berikutnya: FASE P3 (peta, export GPX, auto-pause, backup, kalibrasi, pengingat).

## P3.1 Peta rute di Detail Sesi — selesai (2026-10-11)
- osmdroid 6.1.20 (tile OpenStreetMap, **tanpa API key**); User-Agent diset di `RunCoachApp`; izin `INTERNET`.
- `RouteMapView`: polyline rute + marker start/finish + marker batas segmen; offline tetap menggambar rute (tile kosong);
  latar mengikuti tema. Tab **Peta** di S08 (tampil bila ada track point).
- Test: `HistoryUiTest.detailMapTabRendersWithoutCrash` (device, osmdroid render).

## P3.2 Peta live (S05) — selesai (2026-10-11)
- Toggle **Peta** di S05 (default **mati**, setelan `liveMap`); menampilkan `RouteMapView` dengan track live (buffer 400 titik).
- Display-only (tidak memengaruhi engine/akurasi); render berhenti saat layar mati (pause via lifecycle).
- Test: `LiveMapTest` (render tanpa crash). **Instrumented 22/22 hijau**.
- Gap: uji offline & mode gelap masih manual; marker batas segmen perkiraan; track live mulai kosong saat resume.

## P3.3 Auto-pause — selesai (2026-10-11)
- Engine: deteksi di `WorkoutRunner` (pakai `speedMps` sample atau kecepatan tersirat): pause bila < 0.6 m/s selama
  3 dtk; resume bila >= 1.0 m/s selama 2 dtk; **tidak retroaktif** (Asumsi 17). Event `AutoPaused`/`AutoResumed`;
  default **OFF** (`EngineConfig.autoPauseEnabled`).
- Wiring service dari settings: `autoPause`, `cueDistanceM`, `startCountdown`, `countdownStyle`, `vibrate`; announcer
  mengucap "Dijeda."/"Dilanjutkan."; label S05 **DIJEDA OTOMATIS**.
- Engine test `AutoPauseTest`: stopAndGo → moving 21 dtk dari 30 dtk; default off. Engine **183** hijau.
- **Uji instrumented device TIDAK dijalankan ronde ini**: device Realme terputus (hanya emulator offline). Tersisa manual.




