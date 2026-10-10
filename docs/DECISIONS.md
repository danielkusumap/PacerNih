# DECISIONS

## D-001 `:engine` sebagai modul Kotlin/JVM murni
Engine dipisah agar bisa dites tanpa device. `:engine` tidak boleh mengimpor `android.*`
(aturan anti-slop #2) dan tidak boleh memanggil `System.currentTimeMillis()`.

## D-002 Adopsi built-in Kotlin AGP 9 (tanpa plugin kotlin-android)
AGP 9.0+ menyediakan Kotlin built-in; menerapkan `org.jetbrains.kotlin.android` justru error.
Konsekuensi:
- `kotlin.compilerOptions.jvmTarget` default mengikuti `android.compileOptions.targetCompatibility`
  (diset `VERSION_17`), jadi tidak perlu diset manual.
- KGP dinaikkan ke 2.4.21; plugin Compose dan serialization memakai versi KGP yang sama.
- Modul JVM murni (`:engine`) tetap memakai plugin `org.jetbrains.kotlin.jvm`.

## D-003 minSdk 26
Sesuai bagian 4 plan (min 26). Template Android Studio membawa 34; diubah ke 26.
Konsekuensi: API > 26 butuh guard (mis. `POST_NOTIFICATIONS` API 33, `VibrationEffect` API 26).

## D-004 appcompat + Material Components (Views) masih dipertahankan
Tema XML `Theme.RunCoach` masih berbasis `Theme.MaterialComponents.DayNight.NoActionBar`.
Belum dihapus agar diff kecil; Compose tetap jadi UI utama. Hapus saat tidak ada lagi
ketergantungan View (kemungkinan setelah UI Compose final).

## D-005 Build dari CLI butuh JDK 25
`gradle/gradle-daemon-jvm.properties` menetapkan toolchain daemon = 25 (sama dengan Android
Studio JBR). Contoh: set `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr` sebelum `gradlew`.

## D-006 `EngineConfig` berupa data class (bukan konstanta global)
Aturan anti-slop #5 meminta semua ambang di satu file. Bentuk `data class` dipilih agar nilai bisa
disetel per-run (mis. dari Pengaturan) tanpa menyentuh algoritma, dan mudah di-inject di test.
Untuk sekarang hanya konstanta 6.1 yang sudah dipakai (P0.1-P0.10) yang ditambahkan; sisanya
menyusul saat task terkait membutuhkannya. Nilai masih provisional menunggu 6.14 (Q-003).

## D-007 Pemisahan `SampleFilter` (penyaring) dan `DistanceTracker` (akumulator)
Penyaringan sample dan akumulasi jarak dipisah jadi dua kelas dengan tanggung jawab tunggal.
Konsekuensi: gap GPS / terowongan **tidak** butuh kode khusus di tracker. Loncatan tidak masuk akal
dibuang `SampleFilter` (aturan kecepatan tersirat), sedangkan loncatan wajar dihitung sebagai garis
lurus. Diuji sebagai integrasi filter+tracker (DistanceTrackerTest).

## D-008 `BestEffortCalculator`: anomali dibuang per-window, input = waktu berjalan
Interpretasi 6.10: window yang pace-nya lebih cepat dari `recordMinValidPaceSecPerKm` dianggap anomali
GPS dan **window itu** dibuang, bukan seluruh rekor jarak tersebut (window valid tercepat tetap dipakai).
Kontrak input: `DistTimePoint.elapsedMs` harus **waktu berjalan** (tanpa jeda); kalkulator tidak pernah
melihat wall clock. Algoritma dua-pointer O(n) dengan interpolasi titik awal window.

## D-009 Urutan task menyimpang dari plan (P0.10 sebelum P0.5)
Plan menganjurkan urutan prioritas, tetapi P0.5/P0.7/P0.8/P0.9/P0.11 bergantung pada 6.14 yang belum ada.
P0.10 (mandiri, angka di 6.10) dikerjakan lebih dulu agar ada kemajuan tanpa mengarang. P0.6 dan P0.8
ditunda karena detail tepinya (F15, pembulatan) berpotensi berubah. Tidak melompat fase (masih P0).

## D-010 Versi AGP masih alpha (9.5.0-alpha09)
Tech stack (bagian 4) diperbarui: pakai versi **stabil**; alpha/beta hanya bila terpaksa dan alasannya
wajib dicatat di sini. Alasan saat ini: proyek di-generate oleh Android Studio **preview** (build AI-262)
yang memasang AGP 9.5.0-alpha09, Gradle 9.6.0, compileSdk 37, dan toolchain daemon JDK 25. Menurunkan ke
AGP stabil menuntut penyelarasan Gradle/JDK dan berisiko memecah template; ditunda. Build tetap
reproducible lewat version catalog. Tindak lanjut: evaluasi naik ke AGP stabil sebelum rilis (mis. P4.3).

## D-011 Perubahan API P0.4b (FilteredSample + moving clock)
Sesuai Lampiran C.1 (D-007 boleh dipertahankan, tetapi aturan F3 wajib ada), `SampleFilter.accept` kini
mengembalikan `FilteredSample(outcome, stepM)` dengan empat hasil: MOVING, STATIONARY (jitter, tanpa
jarak, referensi tidak bergeser), RESET (referensi di-reset setelah 3 penolakan kecepatan), REJECTED.
`DistanceTracker.add(mtMs, filtered)` menerima **moving clock** (F5) dari pemanggil dan jarak mentah dari
filter, lalu menerapkan `calibrationFactor` (F4, clamp 0.95–1.05). `BoundaryCrossing` memakai `timeMs`
(moving clock), bukan `timeNanos`. Test P0.3/P0.4 diperbarui ke API baru, bukan dihapus.

## D-012 P0.5: `roundHalfUp` bersama, ambang realisme F12, bobot pace live
- `roundHalfUp` (6.14.0) ditaruh sekali di `engine/Rounding.kt` untuk dipakai P0.5/P0.8.
- F12 ("tidak realistis" bila lebih cepat dari 150 s/km) memakai ulang `recordMinValidPaceSecPerKm` (150),
  karena tabel 6.1 tidak punya konstanta terpisah; diberi komentar di kode.
- `LivePaceEstimator` memberi bobot waktu tiap sample secara trapesium (sample tengah = setengah jarak ke
  kedua tetangga), sehingga lima sample 1 Hz berbobot sama dan menghasilkan v̄ = 3.04 persis (vektor V5).
  Ini konsisten dengan F8 walau F8 menulis "Δt ke sample sebelumnya".

## D-013 P0.6: desain runner + penyimpangan sementara (F7, KmMarker)
- Keluaran runner: `StateFlow<RunnerState>` + `Flow<AlertEvent>` (`MutableSharedFlow`) dan daftar
  `emittedEvents` untuk tes/log S15.
- Moving clock (F5) dipasok pemanggil: `onTick(nowMs)` untuk waktu, timestamp sample untuk jarak; engine
  tetap tidak membaca jam sistem.
- `SegmentEnd` sementara membawa (index, segment) tanpa hasil evaluasi; hasil ditambahkan di P0.8.
- **Penyimpangan sementara (perlu diselaraskan):**
  - Batas **waktu** (F7) belum membagi jarak sample secara proporsional; jarak sample yang melintasi batas
    waktu seluruhnya masuk segmen lama, dan segmen berikutnya mulai dari total saat itu.
  - `KmMarker` live memakai waktu sample, bukan hasil interpolasi (F6).
  Keduanya belum diuji AC P0.6 dan akan diselaraskan saat P0.8 (split/interpolasi) atau saat ada vektor terkait.

## D-014 P0.8: evaluator dan split
- Penilaian segmen memakai pace dibulatkan (6.14.0). `SegmentEvaluator.marginSec`/`marginKmSec` diekspos
  sebagai fungsi murni sehingga vektor V11 (margin dibatasi 15) dapat diuji walaupun segmen 40 m (<50 m)
  berstatus DATA KURANG.
- `KmSplit` hanya diberi status/target bila jendela km `[(k-1)·1000, k·1000]` berada sepenuhnya di dalam satu
  segmen bertarget (F18); selain itu status kosong.
- Catatan: F7 (pemisahan jarak proporsional pada batas waktu) dan interpolasi `KmMarker` live tetap belum
  dikerjakan — keduanya urusan runner (D-013), bukan evaluator.

## D-015 P0.7: PhraseBuilder & AlertEngine (cakupan)
- `PhraseBuilder` mengikuti F20 (aturan menang atas tabel ilustrasi 6.7), mis. EN memakai "minutes/seconds".
- `AlertEngine` menaikkan TooSlow/TooFast/BackToStable dengan gerbang F14, hysteresis, hold, dan cooldown ulang.
- Antrean/prioritas suara diwakili `AlertPriority` (6.7). Pemotongan Countdown (flush), pembuangan alert basi >5 dtk,
  dan pembuangan tick countdown >700 ms dilakukan di lapisan pemutaran (service P1.4), belum di engine.
- Vektor yang belum hijau: V1 (format tampilan "5:33", ada di `Formatters.kt` :app, P1.7), V16–V17 (statistik
  mingguan, P2.10), V18 (nama default sesi, P2.5), V19 (kalibrasi jarak, P3.7). V2–V15, V20–V22 sudah hijau.

## D-016 P0.9: TrackSimulator, golden, dan coverage
- `TrackSimulator` deterministik (seeded `Random` hanya untuk `NOISY`); jadi sumber golden test yang stabil.
- Golden disimpan sebagai resource teks (`engine/src/test/resources/golden/`); diregenerasi dengan env
  `RC_UPDATE_GOLDEN=1`, dibandingkan byte demi baris pada run normal.
- Coverage memakai **plugin `jacoco` bawaan Gradle** (bukan dependency pihak ketiga), jadi tidak ada
  penambahan dependency di luar whitelist. Gate: LINE >= 85% pada task `jacocoTestCoverageVerification`;
  hasil 93.5%. `:engine:test` < 30 detik.

## D-017 P1.1: S01/S14, izin, dan navigasi
- `:app` memakai Navigation Compose; start = S01 onboarding; S14 panduan baterai; `home` placeholder (P1.7).
- Izin runtime: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `POST_NOTIFICATIONS` (API 33+).
  **Tidak** meminta `ACCESS_BACKGROUND_LOCATION` (sesuai bagian 4).
- Deep link baterai memakai `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` (tanpa izin khusus), bukan
  `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` yang menuntut izin `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
- Teks UI di `strings.xml` (values = ID, values-en = EN); tiap layar punya `@Preview` light & dark (aturan #12/#13).

## D-018 P1.2: LocationSource, dependency, dan wiring `:engine`
- `:app` menambahkan dependency `:engine` (project) dan `play-services-location` 21.4.0 (whitelist bagian 4).
- `LocationSource` interface; `FusedLocationSource` (Play Services) + `AndroidLocationSource` (fallback framework);
  `LocationSourceFactory` memilih berdasar ketersediaan Play Services.
- `LocationSample` juga membawa `speedAccuracyMps` (dipakai F8 untuk syarat Doppler); field ini sudah ditambahkan saat P0.6.
- Catatan: AC "sampel ~1 Hz di perangkat nyata" belum diverifikasi (butuh device).

## D-019 P1.3: RunForegroundService
- Service tipe `location` memegang `WorkoutRunner` + `LocationSource`; partial wake lock; notifikasi ongoing
  diperbarui tiap detik dari `runner.state`; aksi Stop memanggil `onStop` lalu `stopSelf`. Tidak auto-restart.
- `Formatters` (aturan #14) ditambahkan sebagai satu-satunya tempat format angka tampilan/notifikasi.
- Tombol "Mulai/Stop sesi uji" pada layar placeholder `home` bersifat **sementara** (agar uji lapangan P1.3 bisa
  dilakukan); akan diganti S02 pada P2.3. Tidak ada fitur produk di luar plan yang ditambahkan.
- AC "layar mati 30 menit" belum diuji (butuh device).

## D-020 targetSdk diturunkan ke 30 (Android 11) untuk device uji
Plan bagian 4 menulis "Min SDK / Target = 26 / latest stable". Atas permintaan pemilik, `targetSdk` diturunkan
dari 37 ke **30 (Android 11)** agar perilaku app cocok dengan device uji (Realme, API 30). `minSdk` tetap **26
(Android 8)** — jadi app memang sudah bisa berjalan di Android 11 sejak awal; penurunan ini penyimpangan sengaja
dari plan dan dapat dikembalikan ke latest stable saat rilis. `compileSdk` tetap 37.

## D-021 Uji device P1.3 (Realme RMX1971 / Android 11)
- Diuji via adb: service foreground tipe `location` hidup, notifikasi update tiap detik, bertahan **5 menit layar
  mati** (durasi 00:20 → 05:27, jarak 0.10 km), Stop menghentikan service + menghapus notifikasi.
- Kendala adb di perangkat: `pm grant` izin ditolak shell (OEM Realme) dan `settings put secure location_mode`
  diblokir → izin & lokasi diselesaikan lewat UI (tap). `svc location enable` juga tidak tersedia; lokasi
  akhirnya aktif (=3) dan izin diberikan lewat dialog.
- Device ter-charge USB selama uji; uji tanpa charge (doze penuh) belum dilakukan.

## D-022 P1.4: komponen suara/getar + perubahan payload event
- `TtsSpeaker`, `BeepPlayer`, `Haptics`, `RunAnnouncer` di `:app` (`service/voice`). Audio focus
  `GAIN_TRANSIENT_MAY_DUCK` (ducking), queue TTS tunggal, Countdown memakai `QUEUE_FLUSH` (memotong ucapan).
- `AlertEvent.PreSegmentWarning` kini membawa `nextKind`; `AlertEvent.KmMarker` kini membawa
  `km, timeSec, paceSecPerKm` — agar `PhraseBuilder` (F20) bisa menyusun teks. Test terkait disesuaikan; golden
  test tidak terpengaruh (ringkasan dihitung per `simpleName`).
- `VIBRATE` ditambahkan ke manifest; sebelum itu getar melempar `SecurityException` dan mematikan proses saat uji
  device. Event announcer dibungkus `runCatching` agar satu kegagalan tidak mematikan service.
- Gaya countdown & bahasa masih default (BEEP, ID); akan disambungkan ke pengaturan S12 pada P2.11.
  `AlertEngine` (alert koreksi) dan teks `SegmentEnd` belum di-wire ke announcer.

## D-023 P1.5: Room+KSP dan pemulihan berbasis replay
- Menambah **Room 2.8.5** + **KSP 2.3.12** (whitelist). `exportSchema = true`; skema diekspor ke `app/schemas`.
- Pemulihan memakai **replay** track point ke `WorkoutRunner` baru (deterministik), bukan menyimpan state internal
  runner. `RunSnapshot` menyimpan workout + waktu mulai (`elapsedRealtime`) + `zoneId`; kolom DB tetap `runnerStateJson`.
- **Batasan:** moving time saat resume ikut menghitung jeda ketika proses mati (jam dihitung dari start asli).
  Jarak & progres segmen tetap benar. Presisi waktu bisa diperbaiki nanti bila diperlukan.
- D04 sementara ditempatkan di layar Home (placeholder); akan dipindah ke alur "app dibuka" saat S02 ada (P2.3).

## D-024 P1.6: simulator in-app
- `SimulatedLocationSource` mengimplementasi `LocationSource` dari `TrackSimulator`; `SimulationController`
  memutar workout pada clock sintetis (`delay(1000/scale)`) sehingga 1x/10x/60x.
- S15 hanya bisa diakses di build debug (dicek via `ApplicationInfo.FLAG_DEBUGGABLE`); tombolnya di Home placeholder.
- Ringkasan S15 memakai format golden agar mudah dibandingkan; sudah diverifikasi identik di device (60x).

## D-025 P1.7: jembatan UI + layar S05
- `RunStateHolder` (singleton in-process) menyalurkan `RunUiState` dari service ke Compose via `StateFlow`.
- `AlertEngine` di-wire di service tiap tick; `CorrectionInput` dibangun dari `RunnerState` memakai `PaceCalculator`.
  **Penyederhanaan:** `elapsedScope` memakai waktu segmen penuh (bukan diinterpolasi per-km), dan sisa-ke-batas
  jarak memakai ETA `remainingM/1000 × paceLive`. Cukup untuk gate/status, bisa diperhalus nanti bila ada kebutuhan.
- `RunScreen` dibuat stateless (menerima `RunUiState`) agar setiap state bisa di-`@Preview` light & dark.
- `keepScreenOn` default `false`; digerakkan dari pengaturan pada P2.11.

## D-026 P1.8: sinyal countdown/cue di state + overlay S05
- `RunnerState` menambah `segmentCountdownSec`, `distanceCueActive`, `distanceCueNextKind`, dihitung di
  `updateState` (bukan field mutable baru) agar deterministik dan ikut pause.
- Overlay countdown awal memakai `phase == COUNTDOWN` + `countdownSec`; tombol Batal lewat aksi service
  `ACTION_CANCEL_COUNTDOWN`. Karena S04 belum ada (P2.4), Batal mengembalikan ke `WAITING_GPS` (tetap di S05).
- Catatan: uji device menutup countdown awal & cue; banner countdown segmen waktu diverifikasi lewat preview
  (engine sudah diuji di P0.6).

## D-027 P1.9: kontrol layar lari
- Kontrol lewat aksi service (`ACTION_PAUSE`, `ACTION_RESUME_RUN`, `ACTION_SKIP`, `ACTION_EXTEND`) sehingga
  UI tidak memegang engine. `RunUiState.steps` membawa hasil expand workout untuk D10.
- **Ditunda & tercatat:** baris `Session` saat Stop (butuh `Session`/S06, P2.5); chip hasil segmen & status
  hasil per baris D10 (butuh evaluasi segmen di-wire, P2.8). Ini bukan stub: fitur belum ditambahkan, bukan dummy.
- Back dialog memakai `BackHandler`; "Tetap lari" menutup dialog, "Stop" menghentikan sesi.

## D-028 P2.1: tabel template + migrasi
- `RunDatabase` dinaikkan ke v2 dengan `MIGRATION_1_2` menambah tabel `WorkoutTemplate` (SQL dibuat manual agar
  cocok dengan skema hasil Room). Tanpa `fallbackToDestructiveMigration` (aturan plan).
- Proteksi built-in dilakukan di lapisan repository (DAO `deleteIfNotBuiltIn`, `update` menolak jika `isBuiltIn`).
- Verifikasi CRUD/migrasi memakai **instrumented test** di device (bukan unit lokal) karena butuh SQLite Android.

## D-029 P2.2: editor + preset + info
- Estimasi (F24) dan preset (D02) ditaruh di **engine** (pure, teruji), bukan di UI; UI hanya memanggil.
- `Repeat` diedit inline di daftar (batas via dialog, anak via D01); `Open` hanya diizinkan untuk segmen terakhir
  di luar Repeat (konsisten dengan validator).
- **D11**: konten 8.3 disimpan di `strings.xml` (ID & EN) sebagai satu string per topik dengan label bagian inline
  (Kegunaan/Kapan/Contoh/Perilaku/Tips) — penyederhanaan dari tabel 8.3 agar praktis; isi tetap dari resource.
- Menyimpan template lewat `WorkoutTemplateRepository.create`; editor dipakai juga nanti untuk edit (initial Workout).

## D-030 P2.3: Beranda + update template
- **D04 digabung** menjadi banner di S02 (sesuai spec bagian 8) agar tidak ada dua prompt untuk sesi yang sama.
- `EditorRequestHolder` membawa Workout yang diedit ke route editor; Simpan memilih create/update.
- Kartu ringkasan minggu ini ditunda ke P2.10 sesuai plan.
- Catatan UX: tombol aksi editor bisa tertutup keyboard saat field nama fokus → kandidat `imePadding` (belum dikerjakan).

## D-031 P2.4: Pra-start & pengaturan cepat
- Beranda tidak lagi memulai sesi langsung; ia menuju S04 dan memilih workout via `StartRequestHolder`.
- Countdown awal & bahasa voice diteruskan sebagai intent extra ke service (belum dipersistensi; S12/P2.11).
- Mulai aktif hanya bila izin lokasi ada **dan** `GpsFixQuality.READY`; selain itu "Tunggu GPS" + "Mulai saja".

## D-032 P2.5: sesi tersimpan & hasil
- Hasil (SegmentResult/KmSplit/BestEffort) dihitung dari **replay track point** (`SampleFilter`+`DistanceTracker`
  → kurva), lalu `SessionResultsCalculator`. **Moving time ≈ elapsed** (durasi pause belum didistribusikan) —
  batas yang dicatat; jarak/segmentasi benar.
- Session disimpan **sebelum** S06 (aturan plan); snapshot aktif dihapus, track point disimpan untuk riwayat.
- Catatan >500 **dipotong** di repository; S06 menolak (tombol nonaktif) saat >500.
- Lencana rekor = effort sesi ini yang lebih cepat dari minimum sesi lain yang tidak dikecualikan.
- P2.4 & P2.5 di-commit bersama karena menyentuh file yang sama (service/AppNavHost/strings).

## D-033 P2.6-P2.8: Riwayat, chart, detail
- Bottom bar baru 2 tab (Latihan/Riwayat); tab Statistik & Pengaturan menyusul (P2.9/P2.11).
- Chart = Compose Canvas kustom (tanpa library), sesuai tech stack; tap memilih titik/index.
- D03 mengedit nama/catatan/RPE/kecualikan-rekor via `SessionRepository.updateForm`; kosong → nama default.
- S08 memuat hasil dari tabel (`SegmentResult`/`KmSplit`), bukan menghitung ulang.
- **Gap dicatat:** DILEWATI belum dipersistensi; lencana rekor di daftar S07 belum ada; tab Peta menunggu P3.1.
- P2.6-P2.8 di-commit bersama karena berbagi file (AppNavHost/Beranda/strings).

## D-034 P2.9: rekor dari tabel BestEffort
- Rekor dihitung dari tabel `BestEffort` (min per jarak, JOIN sesi non-excluded), jadi hapus sesi cukup menurunkan
  rekor ke baris berikutnya tanpa memindai ulang track.
- Progresi rekor dari `RecordProgression` (pure). Tab Statistik ditambahkan (sub-tab Mingguan placeholder → P2.10).
- Grafik S11 memakai `LineChart` yang sama; toggle waktu/pace.

## D-035 P2.10: statistik mingguan
- `WeeklyStatsAggregator` pure (F21); minggu mulai Senin; sesi < 100 m diabaikan; pace berbobot jarak (bukan rata-rata pace).
- S09 memakai data sesi dari `SessionRepository`; kartu Beranda memakai agregator yang sama.
- Hindari API 34 (`LocalDate.ofInstant`) demi API 30: pakai `Instant.atZone().toLocalDate()`.
- "Rekor minggu ini" di S09 belum ditampilkan.

## D-036 P2.11: pengaturan (DataStore) + wiring
- Setelan di DataStore (`SettingsRepository`); tema mengalir ke `RunCoachTheme` → berubah langsung tanpa restart dan persist.
- Wiring penuh baru untuk: tema, layar menyala (S05), bahasa voice + countdown awal (S04/service). Setelan lain dipersistensi
  dan akan disalurkan saat task terkait (auto-pause P3.3; cue/margin/vibrate/kmMarker/paceAlerts menyusul).
- S13 (Data) & S14 (baterai) direferensikan; S13 masih placeholder (P3.5).

## D-037 P3.1/P3.2: peta osmdroid
- `RouteMapView` (Compose `AndroidView` + osmdroid MapView); User-Agent via `RunCoachApp`; tanpa API key.
- Tab Peta S08 aktif bila ada track point; batas segmen dihitung dari jarak kumulatif (perkiraan terdekat).
- Peta live S05 default mati (setelan `liveMap`); pause/render mengikuti lifecycle; display-only.
- Gap: uji offline & dark manual; track live kosong saat resume.

## D-038 P3.3: auto-pause
- Deteksi di runner (engine, pure) memakai `speedMps` sample atau kecepatan tersirat; hold 3 dtk / resume 2 dtk; tidak retroaktif.
- Default OFF; diaktifkan lewat S12 (`autoPause`) dan diteruskan ke `EngineConfig` saat start.
- Announcer + label S05 (DIJEDA OTOMATIS). Voice text "Dijeda."/"Dilanjutkan." sesuai P3.3.
- Uji device tertunda (device terputus); AC inti (waktu berjalan tanpa durasi berhenti) diuji di engine.

## D-039 P3.4: export GPX
- `GpxBuilder` pure di engine (mudah diuji dengan parser XML); `GpxExporter` di app menulis file + share via FileProvider.
- File disimpan di `cacheDir/gpx`; provider authority `${applicationId}.fileprovider`.
- Uji validitas file GPX dilakukan di device (`GpxExportTest`).

## D-040 P3.5: cadangan & data
- Format cadangan: satu file zip berisi `runcoach.db` (mentah) + `settings.json`; tidak ada format kustom agar DB utuh.
- Restore menyimpan ulang file DB dan menghapus `-wal`/`-shm`; file rusak/tanpa DB → `BackupResult.Failure` dengan pesan.
- `RunDatabase.close()` mereset singleton (bukan `RoomDatabase.close()` biasa) supaya DB bisa dibuka ulang setelah backup.
- DAO di repository dibuat *lazy* (diambil tiap panggilan) agar tidak memakai instance DB yang sudah ditutup saat backup.
- Hapus semua data = `clearAllTables()` + `reset()` setelan; konfirmasi dua langkah (ketik HAPUS).

## D-041 P3.6: ketahanan GPS
- Deteksi hilang/pulih sudah ada di runner (berbasis `GPS_LOST_TIMEOUT_SEC`); P3.6 menambah test eksplisit, bukan logika baru.
- `droppedSampleCount` diekspos lewat `RunnerState` (dari `SampleFilter`) lalu disimpan per sesi; tidak dihitung ulang saat replay.
- Suara GPS memakai `PhraseBuilder` (ID/EN) dan diumumkan seperti alert lain (queue, bukan flush).

## D-042 P3.7: kalibrasi jarak
- Rumus F25 di engine (`CalibrationCalculator`) supaya mudah diuji dengan vektor V19; app hanya membaca/menulis DataStore.
- Faktor tidak menumpuk: jarak mentah dipulihkan dengan membagi faktor lama sebelum menghitung faktor baru.
- Nilai faktor yang dipakai saat lari disalin ke sesi (`distanceCalibrationFactorUsed`), jadi sesi lama tidak berubah.
- Reset lewat `setCalibrationFactor(1.0)` (S12) dan `reset()` menghapus semua setelan → faktor kembali 1,0.

## D-043 P3.8: pengingat cadangan
- Aturan "jatuh tempo" di engine (`BackupReminder`) supaya UI dan test memakai definisi yang sama.
- Interval disimpan sebagai hari (`backupReminderDays`): 0 = mati, 14 = 2 minggu, 30 = sebulan (sesuai S13).
- Belum pernah mencadangkan dianggap jatuh tempo saat pengingat aktif; default mati agar tidak mengganggu.
- Ditampilkan sebagai banner di S13 (bukan notifikasi sistem) supaya tidak perlu izin notifikasi tambahan.

## D-044 Adopsi DESIGN.md sebagai spec visual yang mengikat
- `DESIGN_updated.md` di-rename menjadi `DESIGN.md` dan berlaku sebagai spec tampilan (menang untuk tampilan;
  `RUNCOACH_PLAN.md` tetap menang untuk data/alur/rumus).
- Konsekuensi: dikerjakan **U0** (fondasi tema: `RcColors`/`RcTypography`/`RcSpacing`/`RcShapes`/`RcMotion`,
  Inter dibundel, uji kontras) dan **U1** (katalog komponen `ui/components`) **sebelum** FASE P4, lalu re-skin
  halaman S02–S13 yang sudah dibangun dengan widget Material mentah.
- Tema lama (`Theme.kt` dengan `darkColorScheme()`/`lightColorScheme()` bawaan, ungu Material) diganti `RcTheme`.
- Penyimpangan yang diketahui & akan dibereskan saat re-skin: S05 masih bisa di-scroll (spec: tidak),
  D08 dibuat dialog (spec: Sheet), S08 memakai tombol Export/Kalibrasi (spec: menu `more_vert`), S13 belum
  punya "Zona bahaya"/SegmentedControl pengingat.

## D-045 U0: fondasi tema
- Inter hanya tersedia sebagai font variabel di sumber (tidak ada static); dibundel sebagai `inter_variable.ttf`
  dan dipakai lewat `Font(..., variationSettings = FontVariation.Settings(FontVariation.weight(w)))`.
- Ikon: 45 VectorDrawable dari Material Symbols Rounded (Apache 2.0), viewBox `0 -960 960 960` dipetakan dengan
  `<group android:translateY="960">`; tanpa menambah dependency ikon (aturan DESIGN 5.4).
- `MaterialTheme.colorScheme`/`typography` dipetakan dari token agar widget Material sisa tidak ungu; komponen baru
  membaca `LocalRc*` langsung.

## D-046 U0: penyesuaian kontras
- `textTertiary` light diubah `#6A737C` → `#66707A`: pasangan dengan `bg` hanya 4,40:1, di bawah gerbang 4,5:1.
- Warna `kind*` (tipe segmen) **tidak** masuk uji kontras numerik karena bersifat dekoratif dan selalu disertai
  ikon + label (DESIGN 3.3, 3.4 "status tidak hanya warna").
- Tombol destruktif terisi memakai token baru `onFail` (dark: teks gelap di atas merah terang; light: putih)
  karena putih di atas `#FF5D5D` (dark) hanya 3,01:1.

## D-047 U1: komponen
- Semua komponen membaca `LocalRc*`; tidak ada hex di komponen. Nilai dp di dalam komponen adalah definisi ukuran
  dari DESIGN bagian 6 (bukan "kode layar"), jadi boleh literal di sana; layar tetap dilarang literal.
- `RcTextField` memakai label bertumpuk (label kecil di atas input) sebagai bentuk sederhana "label mengambang";
  fokus/error tetap border aksen/`fail`.
- Grafik belum dirombak di U1; akan dikerjakan bersama S08/S09/S11 di U2 sesuai 6.1/6.2.

## D-048 Mock data hanya lewat flag build
- `MockDataSeeder` ada di source set main (agar bisa dipanggil `MainActivity`), tetapi hanya berjalan saat
  `BuildConfig.MOCK_DATA` true, yang hanya di-set dengan `-PmockData=true`. Release default false, dan R8 membuang
  cabang tersebut, sehingga APK rilis tidak menyertakan/menjalankan mock data.
- Saat `MOCK_DATA`, app mulai di Beranda (lewati onboarding) agar mudah dieksplorasi.
- Mock data membuat sesi selesai (hasil per segmen, split km, best effort, track) — tidak menyentuh pelacakan live
  (jarak saat lari tetap dari GPS nyata).

## D-049 Release signing & lint
- Ditambah `release.keystore` (gitignored) + `signingConfigs.release` agar `assembleRelease` menghasilkan APK
  ter-sign yang bisa dipasang di ponsel lain.
- `lint.checkReleaseBuilds = false`/`abortOnError = false`: `targetSdk 30` disengaja (dipilih untuk device uji),
  dan lint Play "target API 33" tidak boleh memblokir build rilis lokal.
