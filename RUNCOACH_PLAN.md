# RunCoach (working name): Android Workout Runner, Zero Cost

**Versi 2.1.** Dokumen ini adalah spesifikasi dan rencana kerja untuk AI agent. Baca seluruhnya sebelum menulis kode. Kerjakan task **berurutan sesuai prioritas**. Jangan lompat fase. Spesifikasi halaman (bagian 8) bersifat mengikat: tidak boleh menambah atau mengurangi data di layar tanpa mencatatnya di `docs/DECISIONS.md`.

## Daftar Isi
1. Deskripsi Proyek
2. Aturan Anti-Slop untuk Agent
3. Arsitektur
4. Tech Stack
5. Domain Model
6. Spesifikasi Engine (termasuk 6.14 Katalog Rumus detail)
7. Skema Data (Room, gaya log, tanpa foreign key)
8. Peta Navigasi dan Spesifikasi Halaman (15 halaman, 11 sheet/dialog, termasuk 8.3 isi Info Segmen)
9. Rencana Task (urut prioritas)
10. Strategi Pengujian
11. Risiko dan Mitigasi
12. Asumsi

---

## 1. Deskripsi Proyek

Aplikasi Android pribadi (mirip Strava x Runna) untuk lari/jalan dengan **workout terstruktur**. Pengguna menyusun workout sebagai daftar segmen (warm up, interval, tempo, run/walk, cooldown, dst.), lalu menekan Start. Aplikasi menghitung mundur, menjalankan workout otomatis, melacak GPS, menghitung pace, memberi alert suara dan getar, lalu menilai tiap segmen lulus atau tidak. Setelah selesai, pengguna bisa memberi nama dan catatan. Riwayat, rekor pribadi, dan ringkasan mingguan tersedia.

### Tujuan utama
1. Mengukur jarak, waktu, dan pace seakurat mungkin dari GPS HP.
2. Workout fleksibel berbasis segmen (jarak atau waktu), dengan target pace per segmen.
3. Voice dan alert informatif: terlalu cepat / terlalu lambat / stabil, pace sekarang, pace target, plus hitung mundur sebelum start dan sebelum segmen berikutnya.
4. Riwayat dengan split per segmen (lulus/gagal) dan split per km.
5. Rekor pribadi (1 km, 5 km, 10 km, half marathon, marathon) dan ringkasan mingguan dengan grafik.
6. Nama workout dan catatan pasca-lari, bisa diedit di riwayat.

### Non-goals (JANGAN dikerjakan)
- Treadmill / indoor tanpa GPS.
- Akun, login, cloud sync, sosial, leaderboard.
- Heart rate, sensor eksternal, Wear OS.
- Elevasi/tanjakan (altitude GPS HP tidak akurat), cuaca.
- Rencana latihan otomatis / AI coach.
- Auto-stop atau auto-finish workout dalam kondisi apa pun.

### Batasan biaya (WAJIB)
- Tanpa API berbayar, tanpa API key, tanpa akun billing, tanpa backend, tanpa login.
- Dilarang: Google Maps SDK, Mapbox, Firebase, analytics, crash reporting pihak ketiga, iklan.
- Semua data disimpan lokal di SQLite (lewat Room). Distribusi hanya APK yang di-install manual.

---

## 2. Aturan Anti-Slop untuk Agent

1. **Jangan mengarang.** Jika spesifikasi ambigu, tulis pertanyaan di `docs/OPEN_QUESTIONS.md` dan lanjut dengan asumsi paling aman. Jangan diam-diam memilih.
2. **Engine = pure Kotlin (JVM), nol import `android.*`.** Engine tidak boleh memanggil `System.currentTimeMillis()`. Waktu selalu datang dari input (timestamp sample / tick), supaya deterministik dan bisa dites.
3. **Satu task = satu commit**, pesan `P0.4: implement boundary interpolation`. Jangan campur task.
4. **Setiap task punya Acceptance Criteria (AC).** Task selesai hanya jika semua AC terpenuhi dan test hijau. Jangan klaim selesai tanpa menjalankan test.
5. **Semua angka ambang hidup di satu file** `EngineConfig.kt`. Tanpa magic number tersebar.
6. **Dependency whitelist** (bagian 4). Menambah dependency di luar daftar harus dicatat dan dibenarkan di `docs/DECISIONS.md`.
7. **Tidak ada fitur di luar plan.** Tidak ada "sekalian" menambah fitur, animasi berlebihan, atau abstraksi yang tidak dipakai.
8. **Tidak ada TODO kosong, fungsi stub, atau test yang dikomentari.** Jika belum bisa, tulis di OPEN_QUESTIONS.
9. **Komentar kode seperlunya**: jelaskan *kenapa*, bukan *apa*. Rumus penting diberi komentar beserta satuannya.
10. **Satuan eksplisit** pada nama: `distanceM`, `durationMs`, `paceSecPerKm`, `speedMps`.
11. **Selesai satu fase = tulis ringkas di `docs/PROGRESS.md`**: apa yang jadi, apa yang belum, bug yang diketahui.
12. **Setiap halaman wajib punya state: loading, kosong (empty), dan error**, serta `@Preview` Compose untuk light dan dark. Tidak ada halaman yang hanya menampilkan "happy path".
13. **Semua teks UI di `strings.xml`** (id default, en tersedia). Tidak ada string hardcode di Compose.
14. **Semua format angka lewat satu `Formatters.kt`** (aturan di bagian 6.10), bukan `String.format` tersebar.
15. **Data log tidak bergantung ke tabel lain** (bagian 7). Dilarang menambah `@ForeignKey` atau `ON DELETE CASCADE`.
16. **Rumus di 6.14 wajib diimplementasikan persis** (termasuk pembulatan dan kasus tepi). Tiap vektor uji di 6.14.2 menjadi unit test dengan nilai persis. Dilarang mengganti metode (mis. merata-ratakan pace alih-alih membagi total waktu dengan total jarak) tanpa catatan di `docs/DECISIONS.md`.

---

## 3. Arsitektur

```
:engine   (Kotlin JVM library, pure, tanpa Android)
  model/      Workout, Block, Segment, Repeat, LocationSample, dst.
  gps/        SampleFilter, DistanceTracker (+ interpolasi)
  metrics/    PaceCalculator (live, avg, projected, required)
  runner/     WorkoutRunner (state machine segmen + countdown)
  alert/      AlertEngine, AlertEvent, hysteresis/cooldown
  result/     SegmentEvaluator, KmSplitCalculator, BestEffortCalculator
  stats/      WeeklyStatsAggregator, RecordProgression
  naming/     DefaultSessionNamer
  voice/      PhraseBuilder (ID, EN) -> teks, bukan audio
  sim/        TrackSimulator (skenario GPS palsu)

:app      (Android, Jetpack Compose)
  data/       Room, DataStore, repository
  service/    RunForegroundService, LocationSource, TtsSpeaker, BeepPlayer, Haptics
  ui/         Compose screens, charts, theme, Formatters
  map/        osmdroid wrapper
```

Alur data saat lari:
`LocationSource (Android) -> WorkoutRunner (engine) -> state + AlertEvent -> Service -> TtsSpeaker/BeepPlayer/Haptics/Notification + UI (StateFlow) + Persistence`

`LocationSource` adalah interface. Implementasi: `FusedLocationSource` (nyata), `SimulatedLocationSource` (replay simulator). Engine tidak tahu mana yang dipakai.

---

## 4. Tech Stack

| Area | Pilihan | Catatan |
|---|---|---|
| Bahasa | Kotlin | |
| UI | Jetpack Compose + Material 3 | |
| Navigasi | Navigation Compose | Bottom bar 4 tab |
| Min SDK / Target | 26 / latest stable | |
| Build | Gradle Kotlin DSL, version catalog | |
| Lokasi | `play-services-location` (FusedLocationProviderClient) | Gratis, tanpa API key. Fallback `LocationManager.GPS_PROVIDER` bila Play Services tidak ada |
| DB | Room (SQLite), `exportSchema = true`, migrasi eksplisit | Sesi, track point, hasil, template |
| Settings | DataStore Preferences | Tema, bahasa voice, dll. |
| Serialisasi | kotlinx.serialization (JSON) | Workout disimpan sebagai JSON |
| Async | Kotlin Coroutines + Flow | |
| Voice | `android.speech.tts.TextToSpeech` + `AudioManager` audio focus (ducking) | Bawaan, gratis |
| Beep countdown | `ToneGenerator` bawaan Android | Latensi lebih rendah daripada TTS |
| Getar | `Vibrator` / `VibrationEffect` | |
| Peta | osmdroid (tile OpenStreetMap) | Tanpa key. Set User-Agent, aktifkan tile cache |
| Grafik | Compose Canvas custom (`BarChart`, `LineChart`) | Tanpa library chart |
| DI | Manual (constructor injection) | Tanpa Hilt/Koin |
| Test | JUnit, kotlin-test, kotlinx-coroutines-test, Turbine, Compose UI test (seperlunya) | |

Izin Android: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `POST_NOTIFICATIONS` (API 33+), `VIBRATE`, `WAKE_LOCK`.
**Tidak perlu `ACCESS_BACKGROUND_LOCATION`** selama service dijalankan dari app yang terlihat (foreground service bertipe `location`).

---

## 5. Domain Model

```kotlin
data class Workout(
    val id: String,
    val name: String,
    val items: List<Block>,           // urutan eksekusi
    val goalDistanceM: Int? = null    // target total jarak (mis. run/walk 5 km)
)

sealed interface Block
data class Segment(
    val label: String,                // "Warm up", "Interval 1"
    val kind: SegmentKind,            // WARMUP, RUN, WALK, COOLDOWN, RECOVERY
    val limit: Limit,                 // Distance(m) | Duration(sec) | Open
    val targetPaceSecPerKm: Int?,     // BATAS PALING LAMBAT. null = bebas
    val fastMarginSec: Int? = 20,     // alert "terlalu cepat" jika lebih cepat dari target sebesar ini. null = nonaktif
    val perKmCheck: Boolean = true    // efektif hanya jika Distance >= 1000 m dan ada target pace
) : Block
data class Repeat(
    val times: Int?,                  // jumlah ulangan, atau
    val untilTotalDistanceM: Int?,    // ulang sampai total jarak workout tercapai (salah satu saja)
    val children: List<Segment>
) : Block
```

Aturan validasi (dites):
- `Repeat`: tepat satu dari `times` atau `untilTotalDistanceM`; `children` tidak kosong; children tidak boleh `Open`.
- `Limit.Open` (tanpa batas, hanya berakhir saat Stop) hanya boleh pada **segmen terakhir** workout dan tidak boleh di dalam `Repeat`.
- Distance > 0, Duration > 0, target pace dalam rentang wajar (2:30 sampai 20:00 per km).
- Workout minimal 1 segmen.

**Target pace = batas paling lambat.** Lebih cepat dari target = lulus, tetapi memicu alert "terlalu cepat" jika lebih cepat sebesar `fastMarginSec` (default 20 detik).

**Lari Bebas** adalah template bawaan (tidak bisa dihapus): 1 segmen `RUN`, `Limit.Open`, tanpa target. Gunanya untuk sekadar mengukur pace/jarak/waktu tanpa rencana.

Runtime: `Repeat` di-expand menjadi daftar langkah datar saat start (lazy untuk `untilTotalDistanceM`). Setelah rencana selesai: umumkan "rencana selesai", workout **tetap berjalan** sebagai segmen `Open` tanpa target sampai pengguna menekan Stop.

### Contoh workout (fixture test)
1. **Stable 5 km**: 1 Segment, Distance 5000 m, target 7:00.
2. **Interval**: Warm up 1500 m @8:00; Repeat 3x [200 m @6:30, 400 m @6:00]; Cooldown 1000 m @8:00.
3. **Tempo**: 1000 m @8:10; 1000 m @7:30; 1000 m @7:10; 1000 m @6:50; 1000 m cooldown.
4. **Run/walk**: Repeat sampai total 5000 m [Run 6 menit, Walk 2 menit], tanpa target pace.
5. **Lari Bebas**: 1 segmen Open.

---

## 6. Spesifikasi Engine (sumber kebenaran angka)

> Rumus rinci, pembulatan, kasus tepi, dan contoh angka ada di **6.14 (sumber kebenaran)**. Jika 6.2 sampai 6.13 berbeda dengan 6.14, **6.14 yang berlaku**.

### 6.1 `EngineConfig.kt` (nilai awal, semua bisa disetel)

| Konstanta | Nilai | Arti |
|---|---|---|
| `MAX_ACCURACY_M` | 20 | Sample dengan akurasi horizontal lebih buruk dibuang |
| `GOOD_FIX_ACCURACY_M` | 12 | Syarat indikator "GPS siap" |
| `MAX_SPEED_MPS` | 9.0 | Sample dengan kecepatan tersirat melebihi ini dibuang |
| `LIVE_WINDOW_SEC` | 15 | Jendela pace live |
| `MIN_SEGMENT_DIST_FOR_ALERT_M` | 100 | Jarak minimal dalam segmen sebelum alert koreksi |
| `MIN_SEGMENT_TIME_FOR_ALERT_SEC` | 20 | Waktu minimal dalam segmen sebelum alert koreksi |
| `NO_CORRECTION_BELOW_SEGMENT_M` | 400 | Segmen jarak lebih pendek: hanya cue awal dan hasil |
| `NO_ALERT_LAST_SEC` | 15 | Tanpa alert koreksi di 15 detik terakhir segmen (berbasis waktu: sisa waktu; berbasis jarak: ETA ke batas) |
| `ALERT_HOLD_SEC` | 5 | Kondisi harus bertahan sebelum alert (hysteresis) |
| `ALERT_REPEAT_COOLDOWN_SEC` | 25 | Ulang alert jika belum membaik |
| `PRE_SEGMENT_WARNING_SEC` | 10 | Peringatan sebelum ganti segmen, hanya segmen berbasis waktu dengan durasi >= 30 dtk (peringatan "100 m lagi" dihapus) |
| `START_COUNTDOWN_SEC` | 5 | Hitung mundur sebelum workout mulai (pilihan: 0, 3, 5, 10) |
| `SEGMENT_COUNTDOWN_SEC` | 3 | Hitung mundur sebelum segmen berikutnya (pilihan: 0 = mati, 3, 5) |
| `MIN_SEGMENT_SEC_FOR_COUNTDOWN` | 8 | Segmen berbasis waktu dengan durasi lebih pendek dari ini: tanpa countdown |
| `CUE_DISTANCE_M` | 20 | Cue "siap-siap" saat sisa jarak segmen berbasis jarak <= nilai ini. Pilihan: 0 (mati), 10, 20, 30, 50. Segmen lebih pendek dari 2 x nilai ini tanpa cue |
| `COUNTDOWN_TICK_MAX_LATE_MS` | 700 | Tick countdown yang terlambat lebih dari ini dibuang, bukan dibunyikan telat |
| `GPS_LOST_TIMEOUT_SEC` | 10 | Tanpa sample valid selama ini = "sinyal GPS hilang" |
| `PACE_ERROR_ASSUMED_M` | 6 | Estimasi error jarak GPS untuk status borderline |
| `AUTO_PAUSE_SPEED_MPS` / `_HOLD_SEC` | 0.6 / 3 | Auto-pause (tidak retroaktif, lihat F26) |
| `AUTO_RESUME_SPEED_MPS` / `_HOLD_SEC` | 1.0 / 2 | Auto-resume |
| `RECORD_MIN_VALID_PACE_SEC_PER_KM` | 150 | Best effort lebih cepat dari 2:30/km dianggap tidak valid (anomali GPS) |
| `RECORD_DISTANCES_M` | 1000, 5000, 10000, 21097.5, 42195 | Jarak rekor pribadi |
| `EARTH_RADIUS_M` | 6371008.8 | Jari-jari bumi untuk Haversine |
| `STATIONARY_SPEED_MPS` / `STATIONARY_MAX_DISP_M` | 0.3 / 5 | Aturan jitter saat diam (F3) |
| `SPEED_REJECT_RESET_COUNT` | 3 | Jumlah sample berturut ditolak karena kecepatan sebelum referensi di-reset (F3) |
| `LIVE_MIN_SAMPLES` / `LIVE_FALLBACK_MIN_SEC` | 5 / 8 | Syarat data pace live (F8) |
| `MIN_PACE_DISTANCE_M` | 50 | Jarak minimal agar pace rata-rata ditampilkan |
| `MAX_PACE_DISPLAY_SEC` | 1800 | Pace lebih lambat dari 30:00/km ditampilkan `--:--` |
| `HYSTERESIS_SEC_PER_KM` | 3 | Margin keluar dari kondisi alert (F13) |
| `NO_CORRECTION_BELOW_SEGMENT_SEC` | 60 | Segmen berbasis waktu lebih pendek dari ini: tanpa alert koreksi |
| `PACE_MARGIN_CAP_SEC` | 15 | Batas atas margin BORDERLINE (F17) |
| `CALIBRATION_MIN` / `_MAX` | 0.95 / 1.05 | Batas faktor kalibrasi jarak |
| `MIN_STATS_SESSION_M` | 100 | Sesi lebih pendek tidak dihitung di statistik dan rekor |
| `DISCARD_OFFER_SESSION_M` | 10 | Sesi lebih pendek ditawari "Buang" menonjol |

### 6.2 Filter sample (`SampleFilter`)
Sample dibuang jika: akurasi > `MAX_ACCURACY_M`; timestamp tidak naik; kecepatan tersirat dari sample valid terakhir > `MAX_SPEED_MPS`. Timestamp memakai `elapsedRealtimeNanos`. Setiap sample yang dibuang dihitung (statistik kualitas GPS).

### 6.3 Jarak dan interpolasi (`DistanceTracker`)
- Jarak antar dua sample valid = Haversine.
- Batas segmen/km/target jarak jatuh **di antara** dua sample. Waktu tepat di batas dihitung dengan interpolasi linear: `t = t1 + (boundary - d1) / (d2 - d1) * (t2 - t1)`. Sisa jarak setelah batas dilimpahkan ke segmen berikutnya.
- Satu sample bisa melewati lebih dari satu batas. Harus ditangani dalam satu loop.
- Gap GPS: jika sample kembali dan kecepatan tersirat masuk akal, jarak garis lurus dihitung (terowongan). Jika tidak, titik dibuang.

### 6.4 Metrik pace (`PaceCalculator`)
- **Pace live** = rata-rata kecepatan `location.speed` (Doppler) selama `LIVE_WINDOW_SEC`, dikonversi ke detik/km. Fallback ke jarak/waktu bila `speed` tidak tersedia. Kecepatan < 0.3 m/s dianggap berhenti (pace tidak ditampilkan).
- **Pace rata-rata segmen** = waktu berjalan segmen / jarak segmen (km).
- **Pace rata-rata workout** = total waktu berjalan / total jarak.
- **Projected pace** (lingkup evaluasi): `projected = (elapsedScope + remainingM/1000 * liveSecPerKm) / (scopeDistM/1000)`.
- **Required pace**: `required = (targetSecPerKm * scopeKm - elapsedScope) / remainingKm`. Jika lebih cepat dari 2:30/km atau remaining <= 0, tandai "tidak realistis" dan jangan tampilkan/bacakan angkanya.
- **Lingkup evaluasi alert**: jika `perKmCheck` aktif, lingkup = km yang sedang berjalan di dalam segmen (dari awal segmen). Jika tidak, seluruh segmen.
- Segmen berbasis waktu dengan target pace: lingkup = seluruh segmen, hanya pace live vs target (tanpa projected).
- Waktu berjalan **tidak** menghitung durasi pause atau countdown.

### 6.5 State machine (`WorkoutRunner`)
States: `IDLE -> WAITING_GPS -> COUNTDOWN -> RUNNING <-> PAUSED(manual|auto) -> (STOPPED oleh pengguna)`.

- `COUNTDOWN`: berlangsung `START_COUNTDOWN_SEC` detik (0 = langsung RUNNING). Bisa dibatalkan (kembali ke `WAITING_GPS`/Pra-start). Waktu dan jarak **belum** dihitung; sample tetap diproses untuk kesiapan GPS. Titik awal workout = sample valid pertama setelah countdown selesai.
- Tidak ada state `FINISHED` otomatis. Setelah segmen terakhir selesai: event `WorkoutPlanCompleted`, lalu runner masuk segmen `Open` tanpa target sampai Stop.
- Input: `onSample`, `onTick(timeMs)`, `onStart/onCancelCountdown`, `onPause/onResume`, `onSkipSegment`, `onExtendSegment(+jarak atau +waktu)`, `onStop`.
- Output: `StateFlow<RunnerState>` + `Flow<AlertEvent>`.

### 6.6 Countdown awal dan cue sebelum ganti segmen
Tiga mekanisme. Pemicu persisnya ada di 6.14 (F15).

1. **Countdown awal workout**: `START_COUNTDOWN_SEC` detik (0 = mati), state `COUNTDOWN`.
2. **Segmen berbasis waktu**: hitung mundur 3-2-1 (`SEGMENT_COUNTDOWN_SEC`, 0 = mati) pada detik terakhir segmen, lalu `SegmentStart`. Tepat karena batasnya waktu. Ada pre-warning "10 detik lagi ganti ke jalan" (`PRE_SEGMENT_WARNING_SEC`).
3. **Segmen berbasis jarak**: **tanpa** hitung mundur dan **tanpa** prediksi ETA. Satu cue "Siap-siap, ganti ke {tipe}" (suara + beep pendek + getar) saat sisa jarak segmen <= `CUE_DISTANCE_M` (default 20 m, pilihan Mati/10/20/30/50 di Pengaturan), lalu `SegmentStart` saat batas terlewati. Peringatan "100 m lagi" dihapus.

Aturan bersama:
- Tidak ada cue/countdown bila tidak ada segmen berikutnya.
- Mengikuti pause (waktu tidak berjalan saat pause).
- Tick yang terlambat lebih dari `COUNTDOWN_TICK_MAX_LATE_MS` dibuang, tidak dibunyikan telat.
- Bentuk suara countdown: **beep** (default) atau angka suara, pilihan di Pengaturan. Akhir hitungan: beep panjang/nada tinggi + getar. Cue jarak: beep pendek + getar + kalimat suara.
- Countdown awal workout memakai mekanisme yang sama (`Countdown(5..1)` lalu `WorkoutStarted`).

### 6.7 Alert (`AlertEngine`)
Jenis event: `Countdown(n, target)`, `DistanceCue(nextKind)`, `WorkoutStarted`, `SegmentStart`, `PreSegmentWarning`, `SegmentEnd(result)`, `TooSlow`, `TooFast`, `BackToStable`, `KmMarker`, `GoalDistanceReached`, `WorkoutPlanCompleted`, `GpsLost`, `GpsRecovered`, `AutoPaused`, `AutoResumed`.

Aturan:
- **TooSlow**: projected pace lingkup > target.
- **TooFast**: pace live lebih cepat dari (target - `fastMarginSec`). Nonaktif jika `fastMarginSec == null`.
- **BackToStable**: kembali dari TooSlow/TooFast ke rentang aman.
- Alert koreksi tunduk pada: gerbang awal segmen, gerbang segmen pendek, gerbang akhir segmen (`NO_ALERT_LAST_SEC`), `ALERT_HOLD_SEC`, cooldown ulang. Keluar dari kondisi alert butuh margin 3 detik/km (hysteresis).
- **Prioritas** (tinggi ke rendah): `Countdown` > `WorkoutStarted/SegmentStart/SegmentEnd`, `GoalDistanceReached`, `WorkoutPlanCompleted` > `PreSegmentWarning`, `DistanceCue` > `GpsLost/Recovered`, `AutoPaused` > `TooSlow/TooFast` > `BackToStable` > `KmMarker`. `Countdown` memotong ucapan yang sedang berjalan (flush). Alert rendah yang tertunda lebih dari 5 detik dibuang, bukan diantre.
- Tidak boleh ada dua suara tumpang tindih (kecuali Countdown yang memotong).

Teks yang dibacakan (`PhraseBuilder`, dites untuk ID dan EN):

| Event | Indonesia | English |
|---|---|---|
| Countdown (suara) | "Tiga. Dua. Satu." | "Three. Two. One." |
| WorkoutStarted | "Mulai." | "Go." |
| SegmentStart | "Segmen 2 dari 9. Lari 200 meter. Target pace 6 menit 30." | "Segment 2 of 9. Run 200 meters. Target pace 6 minutes 30." |
| TooSlow | "Terlalu lambat. Pace sekarang 7 menit 15, target 7 menit. Butuh 6 menit 50 untuk sisa jarak ini." | "Too slow. Current pace 7:15, target 7:00. You need 6:50 for the rest of this stretch." |
| TooFast | "Terlalu cepat. Pace sekarang 6 menit 30, target 7 menit." | "Too fast. Current pace 6:30, target 7:00." |
| BackToStable | "Pace stabil. Sekarang 7 menit 2, target 7 menit." | "Pace is steady. Now 7:02, target 7:00." |
| SegmentEnd | "Segmen selesai. Rata-rata 6 menit 28. Lulus." | "Segment done. Average 6:28. Passed." |
| KmMarker | "1 kilometer. Waktu 7 menit 1. Pace 7 menit 1." | "1 kilometer. Time 7:01. Pace 7:01." |
| GoalDistanceReached | "Target jarak tercapai. Lanjut atau berhenti kapan saja." | "Distance goal reached. Keep going or stop anytime." |
| WorkoutPlanCompleted | "Rencana workout selesai." | "Workout plan complete." |
| PreSegmentWarning | "10 detik lagi ganti ke jalan." | "10 seconds until walk." |
| DistanceCue | "Siap-siap. Sebentar lagi ganti ke jalan." | "Get ready. Switching to walk soon." |

Format ucapan pace/waktu/jarak diatur di 6.14 (F20). Contoh di tabel hanya ilustrasi; aturan F20 yang menang dan menjadi dasar snapshot test.

### 6.8 Evaluasi hasil (`SegmentEvaluator`)
Per segmen dengan target pace:
- `avgPace <= target` dan, jika `perKmCheck` aktif, semua km penuh di dalam segmen (dari awal segmen) `<= target` -> **PASS**.
- Gagal tetapi selisih <= `margin` -> **BORDERLINE**, `margin = pace * PACE_ERROR_ASSUMED_M / segmentDistanceM` (200 m: sekitar 10 detik/km; 1 km: sekitar 2-3 detik/km).
- Selain itu **FAIL**.
- Tanpa target: **NO_TARGET**.
- Hasil menyimpan: jarak aktual, waktu, pace rata-rata, selisih terhadap target, daftar km di dalam segmen beserta statusnya.

### 6.9 Split km historis (`KmSplitCalculator`)
Dihitung dari total jarak workout (bukan per segmen): km 1, 2, 3, ... lalu sisa (lari 3,1 km menghasilkan baris "0,1 km"). Tiap baris: jarak, waktu, pace. Jika km tersebut sepenuhnya berada di satu segmen bertarget, tampilkan selisih dan status terhadap target itu. Split km **hanya di riwayat**, tidak ditampilkan live.

### 6.10 Rekor pribadi (`BestEffortCalculator`)
- Rekor = **waktu tercepat untuk menempuh jarak D di mana pun dalam sesi** (sliding window pada jarak kumulatif), bukan sekadar split km yang sejajar. Contoh: 5K tercepat bisa dimulai di menit ke-3 sesi.
- Jarak D: 1 km, 5 km, 10 km, half marathon (21,0975 km), marathon (42,195 km).
- Algoritma (O(n), two-pointer): untuk tiap titik akhir `j` dengan `cumDist[j] >= D`, cari titik awal pada `cumDist = cumDist[j] - D` dengan interpolasi waktu; `effort = t[j] - tStart`; ambil minimum. Memakai **waktu berjalan** (tanpa durasi pause).
- Sesi dengan total jarak < D tidak menghasilkan rekor untuk D tersebut.
- Effort dengan pace lebih cepat dari `RECORD_MIN_VALID_PACE_SEC_PER_KM` dibuang. Sesi yang ditandai `excludeFromRecords` dilewati.
- Dihitung sekali saat sesi disimpan; hasilnya disimpan di tabel `BestEffort` (tanpa FK). Rekor saat ini = nilai minimum per jarak dari tabel itu.
- `RecordProgression` (pure): dari daftar sesi berurut waktu, hasilkan kapan tiap rekor baru tercipta (untuk grafik rekor dan lencana "PR saat itu").

### 6.11 Statistik mingguan (`WeeklyStatsAggregator`, pure)
- Minggu dimulai **Senin** sampai Minggu. Hari ditentukan dari `startedAtEpochMs` + `zoneId` yang tersimpan di sesi.
- Output per minggu: total jarak, total waktu berjalan, jumlah sesi, pace rata-rata (berbobot jarak = total waktu / total km), sesi terpanjang, persentase segmen lulus (segmen bertarget saja, BORDERLINE dihitung terpisah), jarak per hari (7 nilai), selisih jarak terhadap minggu sebelumnya.
- Seri 12 minggu terakhir **termasuk minggu kosong** (jarak 0, pace null sehingga garis putus).

### 6.12 Nama default sesi (`DefaultSessionNamer`, pure)
Berdasarkan jam mulai sesi (zona waktu tersimpan):

| Jam mulai | Indonesia | English |
|---|---|---|
| 04:00-10:59 | Lari Pagi | Morning Run |
| 11:00-14:59 | Lari Siang | Midday Run |
| 15:00-17:59 | Lari Sore | Afternoon Run |
| 18:00-20:59 | Lari Petang | Evening Run |
| 21:00-03:59 | Lari Malam | Night Run |

Bahasa mengikuti bahasa UI saat sesi disimpan. Nama yang tersimpan adalah **teks akhir** (bukan referensi), jadi mengganti bahasa kemudian tidak mengubah riwayat.

### 6.13 Aturan format tampilan (`Formatters.kt`)
- Pace: `m:ss` per km (mis. `6:30`), tanpa nol di depan menit; diberi satuan `/km` bila ruang cukup. Pace tidak valid/berhenti: `--:--`.
- Waktu: `mm:ss` bila < 1 jam, `h:mm:ss` bila >= 1 jam.
- Jarak: km dengan 2 desimal (`5,03 km`); pada segmen: meter bila < 1000 (`200 m`). Pemisah desimal mengikuti locale (koma di Indonesia).
- Selisih terhadap target: tanda eksplisit dan satuan detik (`+0:12`, `-0:05`).
- Tanggal: format lokal pendek; jam 24 jam.
- Angka bergaya tabular (monospaced digits) pada pace/waktu agar tidak bergeser.

---

## 7. Skema Data (Room / SQLite, gaya log)

**Prinsip: riwayat adalah log.** Tiap sesi menyimpan semua yang dibutuhkan untuk ditampilkan sendiri, termasuk salinan workout dan hasil segmen. Tabel hasil segmen, km split, track point, dan best effort **hanya menyimpan `sessionId` biasa (kolom berindeks), TANPA `@ForeignKey` dan TANPA cascade**. Template workout boleh diubah atau dihapus tanpa memengaruhi riwayat.

Konsekuensi yang harus ditangani di kode: penghapusan sesi dilakukan oleh `SessionRepository.deleteSession(id)` dalam **satu transaksi** yang menghapus baris di semua tabel terkait. Ada test "tidak ada baris yatim".

| Tabel | Kolom penting |
|---|---|
| `WorkoutTemplate` | `id`, `name`, `json`, `isBuiltIn`, `createdAt`, `updatedAt`, `lastUsedAt` |
| `Session` | `id`, `templateId?` (nilai biasa, tanpa FK), `templateNameSnapshot`, `workoutSnapshotJson`, `name` (teks akhir), `nameIsCustom`, `note?`, `rpe?` (1-10), `excludeFromRecords`, `startedAtEpochMs`, `zoneId`, `endedAtEpochMs`, `elapsedMs`, `movingMs`, `pausedMs`, `totalDistanceM`, `avgPaceSecPerKm`, `segPass`, `segFail`, `segBorderline`, `segNoTarget`, `planCompleted`, `goalReached`, `distanceCalibrationFactorUsed`, `droppedSampleCount`, `updatedAt` |
| `SegmentResult` | `sessionId` (idx), `idx`, `label`, `kind`, `limitType`, `limitValue`, `targetPace?`, `distanceM`, `durationMs`, `avgPace`, `deltaSec?`, `status`, `kmDetailJson` |
| `KmSplit` | `sessionId` (idx), `idx`, `distanceM`, `durationMs`, `paceSecPerKm`, `targetPace?`, `deltaSec?`, `status?` |
| `TrackPoint` | `sessionId` (idx), `idx`, `timeMs`, `lat`, `lon`, `accuracyM`, `speedMps?`, `segmentIdx` |
| `BestEffort` | `sessionId` (idx), `distanceKey` (`K1`,`K5`,`K10`,`HM`,`FM`), `durationMs`, `startOffsetM` |
| `ActiveRunSnapshot` | `id=1`, `sessionId`, `runnerStateJson`, `savedAt` |

Aturan lain:
- Sesi disimpan (baris `Session` + hasil) **segera saat Stop**, sebelum form nama/catatan muncul, sehingga tidak ada data hilang bila app dimatikan di tengah form. Form hanya meng-`UPDATE` kolom `name`, `nameIsCustom`, `note`, `rpe`.
- Nama kosong saat edit = kembali ke nama default hasil `DefaultSessionNamer` (`nameIsCustom = false`).
- Room: `exportSchema = true`, migrasi tertulis dan dites; dilarang `fallbackToDestructiveMigration`.
- Pengaturan (tema, bahasa, dll.) di DataStore, bukan Room.

---

## 8. Peta Navigasi dan Spesifikasi Halaman

### 8.1 Peta navigasi

```
Bottom bar (4 tab)
 [Latihan]  -> S02 Beranda -> S03 Editor Workout (D01, D02)
                           -> S04 Pra-start -> S05 Layar Lari -> S06 Selesai & Simpan -> S08 Detail Sesi
 [Riwayat]  -> S07 Riwayat -> S08 Detail Sesi (D03, D08, D09)
 [Statistik]-> S09 Ringkasan Mingguan | S10 Rekor Pribadi -> S11 Detail Rekor -> S08
 [Pengaturan]-> S12 Pengaturan -> S13 Cadangan & Data
                              -> S14 Panduan Baterai
                              -> S15 Debug/Simulator (hanya build debug)
Saat pertama kali: S01 Onboarding & Izin
Saat ada sesi aktif tersisa: D04 Lanjutkan sesi
```
Bottom bar disembunyikan di S03, S04, S05, S06. Tombol back di S05 tidak menutup sesi: menampilkan dialog "Sesi masih berjalan" (kembali ke layar lari atau Stop).

### 8.2 Ringkasan: 15 halaman + 11 sheet/dialog

| ID | Halaman | Tab / akses | Rilis |
|---|---|---|---|
| S01 | Onboarding & Izin | Pertama kali | Ya |
| S02 | Beranda (Latihan) | Tab 1 | Ya |
| S03 | Editor Workout | Dari S02 | Ya |
| S04 | Pra-start | Dari S02 | Ya |
| S05 | Layar Lari | Dari S04 | Ya |
| S06 | Selesai & Simpan | Setelah Stop | Ya |
| S07 | Riwayat | Tab 2 | Ya |
| S08 | Detail Sesi | Dari S06/S07/S11 | Ya |
| S09 | Ringkasan Mingguan | Tab 3 (sub-tab 1) | Ya |
| S10 | Rekor Pribadi | Tab 3 (sub-tab 2) | Ya |
| S11 | Detail Rekor | Dari S10 | Ya |
| S12 | Pengaturan | Tab 4 | Ya |
| S13 | Cadangan & Data | Dari S12 | Ya |
| S14 | Panduan Baterai | Dari S01/S12 | Ya |
| S15 | Debug / Simulator | Dari S12 | Hanya debug |

| ID | Sheet / Dialog | Dipakai di |
|---|---|---|
| D01 | Editor Segmen | S03 |
| D02 | Preset Generator | S03 |
| D03 | Edit Sesi (nama, catatan, RPE, kecualikan dari rekor) | S07, S08 |
| D04 | Lanjutkan Sesi | Saat app dibuka dengan snapshot aktif |
| D05 | Konfirmasi Stop | S05 |
| D06 | Konfirmasi Skip Segmen | S05 |
| D07 | Perpanjang Segmen | S05 |
| D08 | Kalibrasi Jarak | S08 |
| D09 | Konfirmasi Hapus (sesi / template / semua data) | S02, S07, S08, S13 |
| D10 | Daftar Segmen (live) | S05 |
| D11 | Info Segmen & Istilah (ikon ⓘ), isi di 8.3 | S03, D01, D02 |

Semua halaman: dukung light dan dark mode; ukuran sentuh minimal 48dp; kontras minimal WCAG AA; status tidak boleh hanya dibedakan lewat warna (selalu ada ikon dan teks).

---

### S01 Onboarding & Izin
Tujuan: menyiapkan izin agar tracking bisa berjalan.
Langkah: (1) intro singkat, (2) izin lokasi presisi, (3) izin notifikasi (API 33+), (4) tawaran panduan baterai (S14).

| Data yang tampil | Keterangan |
|---|---|
| Status izin lokasi, notifikasi | Diberikan / ditolak / belum ditanya |
| Status GPS perangkat | Aktif / mati (dengan tombol buka pengaturan) |
| Penjelasan singkat tiap izin | Satu kalimat: kenapa dibutuhkan |

Aksi: Berikan izin, Lewati langkah notifikasi, Buka pengaturan sistem. State: izin ditolak permanen menampilkan arahan ke pengaturan sistem. Tidak boleh meminta `ACCESS_BACKGROUND_LOCATION`.

### S02 Beranda (tab Latihan)
Tujuan: titik mulai; memilih dan memulai workout.

| Data yang tampil | Keterangan |
|---|---|
| Banner "Sesi belum selesai" (jika ada) | Nama workout, jarak, waktu, kapan disimpan; tombol Lanjutkan / Buang |
| Kartu ringkasan minggu ini | Total jarak, jumlah sesi, selisih vs minggu lalu; tap menuju S09 |
| Tombol besar "Lari Bebas" | Start cepat ke S04 dengan template bawaan |
| Daftar template | Nama, jumlah segmen (setelah expand), total jarak/durasi perkiraan, ringkasan target pace (mis. "6:00-8:00"), terakhir dipakai |
| Aksi per template | Mulai, Edit, Duplikat, Hapus (D09). Template bawaan tidak bisa dihapus/diedit |
| FAB "Buat workout" | Menuju S03 |

State: kosong (hanya Lari Bebas) menampilkan ajakan membuat workout pertama.

### S03 Editor Workout
Tujuan: membuat/mengedit template.

| Data yang tampil | Keterangan |
|---|---|
| Nama workout | Teks, wajib |
| Target jarak total (opsional) | Untuk run/walk |
| Daftar item berurutan | Segmen (ikon tipe, label, batas, target pace) dan blok Repeat (jumlah ulangan atau "sampai X km", dengan anak-anaknya menjorok) |
| Ringkasan | Jumlah segmen setelah expand, total jarak (jika semua berbasis jarak), perkiraan durasi (dari target pace) |
| Pesan validasi | Per item, dari validator engine, spesifik |

Aksi: tambah segmen (D01), tambah Repeat, preset (D02), ubah urutan (naik/turun), duplikat, hapus item, simpan, batal. **Setiap baris segmen dan setiap istilah memiliki ikon ⓘ** yang membuka D11 (isi di 8.3), sehingga pengguna bisa melihat kegunaan segmen tersebut sebelum memilihnya. Ringkasan estimasi memakai F24. Tidak bisa disimpan jika tidak valid. Perubahan belum tersimpan memicu dialog saat back.

**D01 Editor Segmen** (bottom sheet; pilihan tipe dan tiap istilah punya ikon ⓘ yang membuka D11): label; tipe (Warm up / Run / Walk / Recovery / Cooldown); batas (Jarak m/km, Durasi mm:ss, atau Tanpa batas bila terakhir); target pace m:ss (opsional); margin "terlalu cepat" (default 20 dtk, atau mati); toggle per-km check (hanya aktif bila jarak >= 1 km dan ada target); validasi inline.

**D02 Preset Generator** (bottom sheet): pilih salah satu dan isi parameter.
- Stable run: jarak, pace.
- Progressive tempo: jumlah km, pace awal, langkah per km (mis. -20 dtk), warm up/cooldown opsional.
- Interval: warm up, jarak/durasi kerja + target, recovery + target, jumlah ulangan, cooldown.
- Run/walk: durasi run, durasi walk, target jarak total.
Hasil preset bisa diedit lagi di S03.

### S04 Pra-start
Tujuan: memastikan siap sebelum mulai.

| Data yang tampil | Keterangan |
|---|---|
| Nama workout | |
| Daftar segmen (ringkas, bisa digulir) | Urutan setelah expand: label, batas, target |
| Status GPS | Akurasi dalam meter, label "Siap" (<= 12 m) / "Mencari sinyal" / "Lemah" |
| Status izin dan GPS perangkat | Bila bermasalah: tombol perbaikan |
| Pengaturan cepat | Bahasa voice, countdown awal (0/3/5/10 dtk) |

Aksi: **Mulai**. Jika GPS belum siap: tombol berubah menjadi "Tunggu GPS" dengan opsi "Mulai saja". Mulai menjalankan service dan masuk S05 (state COUNTDOWN). Tidak aktif jika izin belum ada.

### S05 Layar Lari
Tujuan: layar utama saat berlari. Hanya potret (portrait). Boleh menjaga layar tetap menyala (setting). Dirancang agar terbaca sekilas di bawah matahari.

**Susunan dari atas ke bawah:**

| Blok | Data yang tampil |
|---|---|
| A. Header | Nama workout (kecil), ikon status GPS (akurasi), indikator pause/auto-pause bila aktif |
| B. Segmen | Label segmen + tipe (Lari/Jalan/...), "Segmen 3/9", progress bar segmen, sisa segmen (sisa jarak **atau** sisa waktu; yang bukan batas ditampilkan kecil) |
| C. Hero | **Pace live** (angka sangat besar, `m:ss`, `--:--` saat berhenti); **target pace** di bawahnya (`<= 6:30`, atau "Bebas" jika tanpa target); **badge status**: STABIL / TERLALU LAMBAT +0:15 / TERLALU CEPAT -0:22 (ikon + teks + warna; selisih dihitung live pace vs target); tanpa target: badge tidak ada |
| D. Metrik segmen | Pace rata-rata segmen; **Required pace** (hanya tampil saat TERLALU LAMBAT dan realistis, label "Butuh X untuk sisa"); sisa-ke-batas jika relevan |
| E. Total | Waktu total (waktu berjalan), jarak total, pace rata-rata workout (kecil) |
| F. Berikutnya | Kartu preview segmen berikutnya: label, batas, target (mis. "Berikutnya: Jalan 400 m, <= 9:00"). Segmen terakhir: "Berikutnya: Lari bebas (tanpa target)" |
| G. Hasil segmen terakhir | Chip hasil segmen yang baru selesai (LULUS 6:28 / GAGAL 6:41 / BORDERLINE), tampil 8 detik lalu menyusut ke daftar segmen |
| H. Kontrol | Pause/Resume (tombol besar), Skip segmen (D06), Perpanjang segmen (D07), Daftar segmen (D10), Stop (tekan tahan 2 detik atau D05) |

Tidak ditampilkan di layar lari: split km (khusus riwayat), grafik, peta (kecuali opsi peta live P3.2).

**State layar:**

| State | Tampilan |
|---|---|
| WAITING_GPS | "Menunggu GPS" dengan akurasi; tombol Mulai saja |
| COUNTDOWN awal | Overlay penuh: angka besar hitung mundur ("5, 4, 3 ..."), nama workout, tombol Batal. Beep/suara + getar mengikuti pengaturan |
| RUNNING | Susunan A-H di atas |
| Countdown segmen berbasis waktu | Banner di atas (tidak menutupi pace): "Ganti ke JALAN dalam 3"; beep/suara + getar |
| Cue segmen berbasis jarak | Banner "Siap-siap: ganti ke JALAN" saat sisa jarak <= jarak cue; beep pendek + getar; hilang saat segmen berganti |
| PAUSED manual / AUTO | Layar dimmed ringan, label "DIJEDA" / "DIJEDA OTOMATIS", tombol Lanjut |
| GPS hilang | Banner kuning "Sinyal GPS hilang"; pace tampil `--:--` |
| Rencana selesai | Banner "Rencana selesai. Lanjut atau Stop"; segmen berlabel "Lari bebas"; tetap merekam |
| Target jarak tercapai | Banner singkat "Target jarak tercapai" (tidak menghentikan apa pun) |

**D05 Konfirmasi Stop**: ringkas "Hentikan dan simpan sesi?" dengan jarak dan waktu. Alternatif: tekan-tahan 2 detik di tombol Stop tanpa dialog (setting).
**D06 Konfirmasi Skip Segmen**: menampilkan segmen yang akan dilewati dan segmen berikutnya; segmen yang dilewati ditandai "DILEWATI" (bukan lulus/gagal).
**D07 Perpanjang Segmen**: pilihan +100 m / +500 m (segmen jarak) atau +30 dtk / +1 mnt (segmen waktu), atau input bebas; batas baru dicatat di hasil.
**D10 Daftar Segmen** (bottom sheet): semua segmen setelah expand dengan penanda: selesai (hasil LULUS/GAGAL/BORDERLINE/TANPA TARGET/DILEWATI + pace rata-rata), sedang berjalan (progress), belum dimulai (batas + target).

### S06 Selesai & Simpan
Tujuan: setelah Stop; ringkasan cepat dan form opsional. Data sudah tersimpan sebelum halaman ini muncul.

| Data yang tampil | Keterangan |
|---|---|
| Ringkasan | Jarak total, waktu berjalan, pace rata-rata |
| Hasil segmen | Jumlah LULUS / GAGAL / BORDERLINE (ringkas), daftar singkat per segmen |
| Lencana rekor | "Rekor baru: 5 km 28:40" bila sesi ini memecahkan rekor |
| Form: Nama workout (opsional) | Placeholder berisi nama default (mis. "Lari Pagi"); kosong = pakai nama default |
| Form: Skala capek 1-10 (opsional) | Slider/pilihan angka, bisa dikosongkan |
| Form: Catatan (opsional) | Teks multiline, maksimal 500 karakter |

Aksi: **Simpan** (update nama/RPE/catatan lalu ke S08), **Lewati** (pakai nama default tanpa catatan), **Buang sesi** (D09, hanya setelah konfirmasi). Tombol back = Lewati (tidak pernah kehilangan data). Sesi sangat pendek (< 10 m): tawarkan Buang sebagai opsi menonjol.

### S07 Riwayat (tab 2)
Tujuan: daftar semua sesi.

| Data yang tampil | Keterangan |
|---|---|
| Pencarian | Berdasarkan nama |
| Header bulan | Nama bulan + total km dan jumlah sesi bulan itu |
| Item sesi | Nama sesi, tanggal dan jam mulai, jarak, waktu, pace rata-rata, chip hasil ("7/9 lulus" atau "Tanpa target"), lencana rekor bila ada |

Aksi: tap = S08; tekan-tahan atau menu = **Edit nama/catatan (D03)**, Hapus (D09). Urut terbaru dulu. State kosong: "Belum ada sesi".

### S08 Detail Sesi
Tujuan: log lengkap satu sesi. Header tetap; konten dalam tab.

**Header (selalu tampil):**

| Data | Keterangan |
|---|---|
| Nama sesi + ikon edit | Tap membuka D03 |
| Tanggal, jam mulai-selesai | |
| Nama workout (snapshot) | Mis. "Interval 200/400" |
| Statistik utama | Jarak total, waktu berjalan, waktu total (elapsed), waktu jeda (jika ada), pace rata-rata |
| Hasil segmen | Jumlah LULUS / GAGAL / BORDERLINE / TANPA TARGET |
| Status rencana | "Rencana selesai" / "Dihentikan sebelum selesai"; "Target jarak tercapai" bila ada |
| Lencana rekor | Daftar rekor yang dipecahkan sesi ini (PR saat itu) |
| Kartu Catatan | Skala capek (jika ada) dan teks catatan; kosong menampilkan "Tambah catatan" |

**Tab 1: Segmen**

| Kolom | Isi |
|---|---|
| # dan label | Nomor + nama + tipe |
| Batas | `200 m` atau `6:00` (durasi) |
| Target | `<= 6:30` atau "Bebas" |
| Jarak aktual, waktu | Hasil ukur |
| Pace aktual | Rata-rata segmen |
| Selisih | `-0:02` / `+0:12` terhadap target |
| Status | **LULUS / GAGAL / BORDERLINE** / TANPA TARGET / DATA KURANG / DILEWATI (ikon + teks + warna). BORDERLINE memiliki keterangan "dalam batas error GPS" |
| Baris detail (expand) | Daftar km di dalam segmen (untuk segmen >= 1 km bertarget): pace tiap km dan statusnya |

**Tab 2: Split km** (hanya di riwayat)

| Kolom | Isi |
|---|---|
| Km | 1, 2, 3, ..., dan sisa (mis. "0,1 km" untuk lari 3,1 km) |
| Waktu | Waktu km tersebut |
| Pace | Pace km tersebut (sisa km: pace dari jarak parsial) |
| Selisih / status | Terhadap target bila km berada penuh dalam satu segmen bertarget; selain itu kosong |
| Penanda | Km tercepat disorot |
| Footer | Total jarak, total waktu, pace rata-rata |

**Tab 3: Grafik**
- Grafik garis: pace sepanjang jarak (dihaluskan per 100 m), garis target per segmen (bentuk tangga), garis putus vertikal di batas segmen. Tap menampilkan jarak dan pace di titik itu.
- Grafik batang: pace per km (km parsial diberi tanda), dengan garis target bila ada.

**Tab 4: Peta** (aktif setelah P3.1; sebelum itu tab disembunyikan)
- Rute (polyline), marker start/finish, penanda batas segmen, tanpa peta jika tidak ada tile (offline) namun rute tetap digambar.

**Menu (titik tiga):** Edit nama/catatan (D03), Export GPX (P3.4), Kalibrasi jarak (D08, P3.7), Hapus (D09).

**D03 Edit Sesi**: Nama (kosong = nama default), Skala capek 1-10 (bisa dihapus), Catatan (500 karakter), toggle "Jangan hitung untuk rekor pribadi". Simpan memperbarui `updatedAt`. Jika toggle berubah, rekor dihitung ulang dari tabel `BestEffort`.
**D08 Kalibrasi Jarak**: input jarak sebenarnya (km); menampilkan jarak terukur dan faktor koreksi yang dihasilkan (dibatasi 0,95-1,05); tombol "Terapkan untuk sesi berikutnya". Tidak mengubah sesi lama.
**D09 Konfirmasi Hapus**: menyebut apa yang dihapus; hapus semua data meminta konfirmasi dua langkah (ketik "HAPUS").

### S09 Ringkasan Mingguan (tab 3, sub-tab "Mingguan")
Tujuan: melihat tren latihan per minggu (Senin-Minggu).

| Data yang tampil | Keterangan |
|---|---|
| Navigator minggu | "12-18 Okt 2026"; panah minggu sebelumnya/berikutnya; tombol "Minggu ini" |
| Kartu angka | Total jarak, total waktu, jumlah sesi, pace rata-rata, sesi terpanjang, persentase segmen lulus |
| Perbandingan | Selisih jarak (dan persen) terhadap minggu sebelumnya |
| Grafik 1 (batang) | Jarak per hari Senin-Minggu pada minggu terpilih |
| Grafik 2 (batang) | Jarak per minggu, 12 minggu terakhir; minggu terpilih disorot; tap memilih minggu |
| Grafik 3 (garis) | Pace rata-rata per minggu, 12 minggu terakhir; minggu kosong = garis putus |
| Rekor minggu ini | Daftar rekor baru yang tercipta di minggu itu (jika ada) |
| Daftar sesi minggu itu | Item seperti S07, tap menuju S08 |

State kosong: "Belum ada latihan di minggu ini" (grafik tetap tampil dengan nol).

### S10 Rekor Pribadi (tab 3, sub-tab "Rekor")
Tujuan: rekor tercepat per jarak.

| Data yang tampil | Keterangan |
|---|---|
| 5 kartu rekor: 1 km, 5 km, 10 km, Half Marathon (21,0975 km), Marathon (42,195 km) | Tiap kartu: waktu rekor, pace rata-rata, tanggal, nama sesi; "Belum ada" bila belum pernah menempuh jarak itu |
| Rekor terbaru | Daftar 5 rekor terakhir yang tercipta (jarak, waktu, tanggal) |

Aksi: tap kartu = S11. Catatan kecil: "Dihitung dari bagian tercepat dalam sesi, bukan hanya dari awal."

### S11 Detail Rekor
Tujuan: riwayat satu jarak rekor.

| Data yang tampil | Keterangan |
|---|---|
| Rekor saat ini | Waktu, pace, tanggal, nama sesi (tap ke S08), posisi awal dalam sesi (mis. "dari km 2,3") |
| Grafik garis | Best effort terbaik per sesi dari waktu ke waktu; titik rekor (PR saat itu) disorot |
| Top 10 | Peringkat, waktu, pace, tanggal, nama sesi |
| Kolom opsi | Beralih tampilan waktu / pace |

### S12 Pengaturan (tab 4)

| Bagian | Pengaturan |
|---|---|
| Tampilan | Tema: Terang / Gelap / Ikuti sistem; Layar tetap menyala saat lari |
| Voice | Bahasa voice: Indonesia / English; volume relatif; uji suara; status paket suara terpasang atau tidak; km marker (on/off); alert terlalu cepat/lambat (on/off) |
| Countdown | Awal: 0/3/5/10 dtk; segmen berbasis waktu: mati/3/5 dtk; **jarak cue segmen berbasis jarak: mati/10/20/30/50 m**; gaya: beep / suara angka |
| Alert | Margin "terlalu cepat" default (detik); getar (on/off) |
| Tracking | Auto-pause (on/off); kalibrasi jarak saat ini (faktor, tombol reset) |
| Data | Menuju S13 Cadangan & Data |
| Bantuan | Panduan baterai (S14); versi app; lisensi OSS |
| Pengembang | Debug/Simulator (S15), hanya build debug |

Perubahan tema berlaku langsung tanpa restart. Perubahan bahasa voice berlaku di sesi berikutnya.

### S13 Cadangan & Data

| Data yang tampil | Keterangan |
|---|---|
| Ringkasan data | Jumlah sesi, jumlah template, ukuran database |
| Cadangan terakhir | Tanggal, atau "Belum pernah" |
| Pengingat cadangan | Pilihan: mati / tiap 2 minggu / tiap bulan |

Aksi: Cadangkan ke file (SAF), Pulihkan dari file (dengan peringatan menimpa), Hapus semua data (D09 dua langkah). Cadangan berisi database dan pengaturan, satu file.

### S14 Panduan Baterai
Teks langkah demi langkah dan tombol langsung ke pengaturan baterai/aplikasi, dengan instruksi khusus per merek (Xiaomi, Oppo/Realme, Vivo, Samsung, lainnya). Menampilkan status pengecualian optimasi baterai (jika bisa dideteksi).

### S15 Debug / Simulator (hanya build debug)
Pilih workout, pilih skenario simulasi (steady, slowDown, speedUp, intervals, gpsJump, stopAndGo, tunnelGap, noisy), kecepatan putar 1x/10x/60x, tombol jalankan; menampilkan jumlah sample dibuang dan log event alert.

---

## 9. Rencana Task (urut dari prioritas tertinggi)

Legenda: **AC** = Acceptance Criteria. Semua task engine wajib unit test.

### FASE P0: Fondasi dan Engine (PRIORITAS TERTINGGI)
Tujuan: engine benar dan terbukti lewat simulasi sebelum ada UI.

**P0.1 Setup proyek**
- Project Android multi-modul: `:app` dan `:engine` (Kotlin JVM). Version catalog, Compose, Material 3, Navigation Compose.
- AC: `./gradlew :engine:test` dan `./gradlew :app:assembleDebug` sukses; `:engine` tanpa dependency Android; ada `docs/PROGRESS.md`, `docs/DECISIONS.md`, `docs/OPEN_QUESTIONS.md`.

**P0.2 Domain model dan validasi workout**
- `Workout`, `Segment`, `Repeat`, `Limit` (termasuk `Open`), `SegmentKind`, serialisasi JSON, validator, `WorkoutExpander` (termasuk `untilTotalDistanceM` lazy).
- AC: 5 contoh workout (bagian 5) bisa dibuat, diserialisasi bolak-balik, di-expand benar; `Open` hanya valid di segmen terakhir di luar Repeat; input tidak valid menghasilkan error spesifik; minimal 18 kasus test.

**P0.3 Filter sample GPS**
- `SampleFilter` + `LocationSample` sesuai 6.2.
- AC: test akurasi buruk, timestamp mundur, loncatan > 9 m/s, sample normal; statistik sample dibuang tersedia.

**P0.4 DistanceTracker dan interpolasi batas**
- Haversine, akumulasi jarak, lintasan batas dengan interpolasi, multi-batas dalam satu sample, pelimpahan sisa jarak, gap GPS.
- AC: jalur lurus 1000 m kecepatan konstan: waktu di 200/400/1000 m error < 0,2 detik; satu sample 30 m melewati dua batas 10 m menghasilkan dua event benar; test terowongan.

**P0.5 PaceCalculator**
- Live pace (Doppler + fallback), avg segmen, avg workout, projected, required, penanganan "berhenti" dan "tidak realistis".
- AC: test numerik (target 5:30, 2 km sudah 5:40: required sekitar 5:23 untuk 3 km sisa pada lingkup 5 km); stabil terhadap noise (deviasi pace live < 5 dtk/km pada lari konstan 6:00 skenario noisy).

**P0.6 WorkoutRunner (state machine) + countdown**
- Sesuai 6.5 dan 6.6: state `COUNTDOWN`, transisi segmen berbasis jarak (interpolasi) dan waktu (tick), pause/resume, skip, extend, segmen `Open` setelah rencana selesai, tidak pernah auto-stop. Countdown awal, countdown 3-2-1 untuk segmen berbasis waktu, dan cue "siap-siap" pada jarak yang dikonfigurasi untuk segmen berbasis jarak (6.6, F15).
- AC: kelima workout contoh berjalan penuh pada simulasi; countdown awal tidak menghitung waktu/jarak; countdown segmen waktu menghasilkan tick tepat di detik N..1; cue jarak muncul tepat sekali saat sisa jarak <= `CUE_DISTANCE_M`, tidak muncul bila sample yang sama sudah melewati batas atau segmen < 2 x jarak cue; tanpa countdown/cue untuk segmen waktu < 8 dtk atau bila tidak ada segmen berikutnya; tidak pernah masuk state selesai tanpa Stop; run/walk `untilTotalDistanceM=5000` memicu `GoalDistanceReached` sekali dan lanjut.

**P0.7 AlertEngine dan PhraseBuilder**
- Aturan 6.7: gerbang, hysteresis, cooldown, prioritas, antrean tunggal, event Countdown dengan prioritas tertinggi dan pembuangan tick terlambat. `PhraseBuilder` ID dan EN.
- AC: skenario (a) stabil -> nol alert koreksi; (b) melambat bertahap -> satu TooSlow lalu ulang tiap 25 dtk; (c) kembali stabil -> satu BackToStable; (d) lebih cepat 20+ dtk -> TooFast; (e) segmen 200 m -> hanya SegmentStart dan SegmentEnd; (f) tanpa alert di 100 m/20 dtk awal; (g) tabrakan SegmentEnd vs TooSlow -> SegmentEnd menang; (h) tidak ada alert koreksi dalam 15 dtk terakhir (waktu atau ETA); (i) tick Countdown yang terlambat > 700 ms dibuang; (j) DistanceCue tepat satu kali per segmen dan konfigurabel (0/10/20/30/50 m); (k) snapshot test teks ID dan EN semua jenis event sesuai aturan F20.

**P0.8 SegmentEvaluator dan KmSplitCalculator**
- 6.8 dan 6.9, termasuk BORDERLINE, DILEWATI, dan baris sisa km.
- AC: stable 5 km @7:00: PASS bila semua km <= 7:00 dan avg <= 7:00, FAIL jika satu km 7:05; segmen 200 m tepat di batas -> BORDERLINE; lari 3,1 km menghasilkan 4 baris split dengan baris terakhir 0,1 km; segmen tanpa target -> NO_TARGET.

**P0.9 TrackSimulator dan golden test**
- Generator track sintetis deterministik (seed tetap): `steady`, `slowDown`, `speedUp`, `intervals`, `gpsJump`, `stopAndGo`, `tunnelGap`, `noisy`, 1 Hz.
- Golden test end-to-end: workout x skenario menghasilkan daftar event dan hasil segmen yang disimpan sebagai file golden.
- AC: minimal 14 golden test hijau; `./gradlew :engine:test` < 30 detik; coverage engine >= 85% (lines).

**P0.10 BestEffortCalculator**
- Sesuai 6.10 (sliding window dengan interpolasi, filter anomali, jarak 1/5/10/HM/FM).
- AC: pada track sintetis 10 km dengan segmen tercepat yang diketahui (disisipkan di tengah), best effort 1 km dan 5 km ditemukan dengan error < 1 detik dan `startOffsetM` benar; sesi 4,9 km tidak menghasilkan K5; waktu pause tidak ikut dihitung; pace < 2:30/km dibuang.

**P0.11 Vektor uji rumus (6.14.2)**
- Ubah setiap vektor uji V1-V20 di 6.14.2 menjadi unit test dengan nilai persis (toleransi hanya yang tertulis).
- AC: seluruh V1-V20 hijau; satu file test `FormulaVectorsTest.kt`; tiap test menyebut ID vektornya.

> **Gerbang P0**: jangan lanjut ke P1 sebelum semua AC P0 hijau dan `PROGRESS.md` diperbarui.

---

### FASE P1: Runtime Android (tracking nyata, voice, service, layar lari)

**P1.1 Izin, Onboarding (S01), dan Panduan Baterai (S14)**
- Alur izin, deteksi GPS mati, teks panduan baterai per merek dan deep link pengaturan.
- AC: tidak crash saat izin ditolak; ada state jelas "izin belum diberikan"; tidak meminta `ACCESS_BACKGROUND_LOCATION`; S01 dan S14 sesuai spesifikasi bagian 8.

**P1.2 LocationSource**
- Interface + `FusedLocationSource` (interval 1 detik, `PRIORITY_HIGH_ACCURACY`) + fallback `LocationManager`. Konversi `Location` ke `LocationSample` (`elapsedRealtimeNanos`, `speed`, `speedAccuracyMetersPerSecond`).
- AC: di perangkat nyata sample masuk ~1 Hz; indikator "GPS siap" berdasar `GOOD_FIX_ACCURACY_M`.

**P1.3 Foreground Service**
- `RunForegroundService` (tipe `location`) memegang `WorkoutRunner` + `LocationSource`, partial wake lock, notifikasi ongoing (waktu, jarak, pace, nama segmen) diperbarui tiap detik.
- AC: workout berjalan dengan layar mati 30 menit di perangkat nyata; notifikasi benar; service berhenti hanya saat Stop.

**P1.4 Voice, beep, dan haptic**
- `TtsSpeaker`: inisialisasi TTS, pilih bahasa dari settings, cek ketersediaan voice. Audio focus `GAIN_TRANSIENT_MAY_DUCK`. Antrean tunggal mengikuti prioritas, dengan **flush** untuk Countdown. `BeepPlayer` (ToneGenerator): beep tick dan beep akhir. `Haptics`: pola berbeda untuk countdown, ganti segmen, terlalu cepat, terlalu lambat.
- AC: alert terdengar dengan layar mati; musik di-duck lalu kembali; tidak ada suara tumpang tindih; countdown memotong ucapan yang sedang berjalan; ganti bahasa mengubah suara dan teks; gaya countdown beep/suara bekerja.

**P1.5 Persistensi sesi dan pemulihan**
- Tulis track point batch tiap ~5 dtk; `ActiveRunSnapshot` tiap 5 dtk; D04 "Lanjutkan sesi"; rekonstruksi runner dari snapshot.
- AC: kill proses saat lari lalu buka lagi: sesi bisa dilanjutkan tanpa kehilangan jarak/segmen; test instrumented.

**P1.6 Simulator di dalam app (S15)**
- `SimulatedLocationSource` memakai `TrackSimulator`; S15 dengan kecepatan putar 1x/10x/60x, hanya build debug.
- AC: interval dijalankan penuh pada 60x di perangkat, menghasilkan event dan hasil segmen yang sama dengan golden test.

**P1.7 Layar Lari inti (S05)**
- Implementasi blok A-F dan state WAITING_GPS, RUNNING, PAUSED, GPS hilang, rencana selesai, target jarak tercapai (bagian 8). Tema light/dark, angka tabular, `Formatters.kt`.
- AC: semua angka cocok dengan state engine; pace `--:--` saat berhenti; required pace hanya muncul saat TERLALU LAMBAT dan realistis; preview segmen berikutnya benar termasuk "Lari bebas" di akhir; layar tetap menyala sesuai setting; setelah rencana selesai app tetap merekam; screenshot test/preview untuk tiap state, light dan dark.

**P1.8 Countdown UI (S05)**
- Overlay countdown awal (dengan Batal), banner countdown 3-2-1 (segmen waktu) dan banner "Siap-siap" (cue jarak), sinkron dengan event engine, beep/suara/getar.
- AC: countdown awal 5 dtk lalu RUNNING; Batal kembali ke S04; banner tidak menutupi pace; cue jarak hilang saat segmen berganti; countdown berhenti saat pause; hasil dicatat di tes UI.

**P1.9 Kontrol dan sheet layar lari**
- Pause/Resume, Skip (D06, ditandai DILEWATI), Perpanjang (D07), Daftar Segmen (D10), Stop (D05 / tekan-tahan), chip hasil segmen (blok G), dialog back.
- AC: setiap aksi mengubah state engine sesuai spesifikasi; Stop menyimpan sesi (baris `Session` ada sebelum S06 tampil); tombol back tidak menutup sesi diam-diam.

---

### FASE P2: Workout, Riwayat, Rekor, Statistik, Pengaturan

**P2.1 Template workout (CRUD) + Lari Bebas bawaan**
- Room: tabel `WorkoutTemplate` (tanpa FK ke apa pun), CRUD, duplikat, `lastUsedAt`, template bawaan "Lari Bebas" (tidak bisa dihapus/diedit).
- AC: CRUD persisten; menghapus template tidak mengubah riwayat; template bawaan tidak bisa dihapus.

**P2.2 Editor Workout (S03, D01, D02)**
- Sesuai bagian 8: daftar item, Repeat, validasi live, ringkasan, preset (stable, progressive tempo, interval, run/walk), ubah urutan, duplikat, dialog perubahan belum tersimpan, **ikon ⓘ dan sheet D11 Info (8.3)**, serta ringkasan estimasi sesuai F24.
- AC: setiap tipe segmen dan istilah (Batas, Target pace, Margin, Per-km check, Repeat, Cue) punya ⓘ yang membuka D11 dengan isi ID dan EN dari `strings.xml` (tanpa teks hardcode); kelima contoh workout bisa dibuat penuh lewat UI; input tidak valid tidak bisa disimpan dan pesan error jelas; preset menghasilkan workout lolos validator; progressive tempo (mis. 5 km, 7:30 turun 20 dtk/km) menghasilkan segmen benar.

**P2.3 Beranda (S02)**
- Daftar template, Lari Bebas, banner sesi belum selesai, aksi per template. Kartu ringkasan minggu ini disambungkan di P2.10.
- AC: sesuai bagian 8; state kosong; banner muncul hanya jika ada snapshot aktif.

**P2.4 Pra-start (S04)**
- Ringkasan segmen, status GPS, pengaturan cepat, tombol Mulai/Tunggu GPS/Mulai saja.
- AC: Mulai tidak aktif tanpa izin; status GPS berubah real time; masuk S05 ke state COUNTDOWN.

**P2.5 Selesai & Simpan (S06) + nama default**
- Pure `DefaultSessionNamer` di engine (6.12). Saat Stop: hitung `SegmentResult`, `KmSplit`, `BestEffort`, simpan `Session` **dulu**, baru tampilkan S06. Form nama/RPE/catatan, tombol Simpan/Lewati/Buang, lencana rekor.
- AC: nama kosong menghasilkan nama default sesuai jam (uji batas 03:59, 04:00, 10:59, 11:00, 14:59, 15:00, 17:59, 18:00, 20:59, 21:00) dan bahasa; kill app di S06 tidak menghilangkan sesi; Lewati dan back sama; Buang menghapus semua baris terkait (tanpa baris yatim); catatan > 500 karakter ditolak.

**P2.6 Riwayat (S07) + Edit Sesi (D03)**
- Daftar dengan header bulan, pencarian nama, chip hasil, lencana rekor; edit nama/catatan/RPE/kecualikan rekor dari list dan detail; hapus lewat repository transaksional.
- AC: edit nama tersimpan dan tampil di S07/S08/S09; nama dikosongkan kembali ke default; mengubah toggle rekor menghitung ulang rekor; test "tidak ada baris yatim" setelah hapus; riwayat tetap utuh walau template dihapus.

**P2.7 Komponen chart (Canvas)**
- `BarChart` dan `LineChart` reusable: sumbu, label, garis target/tangga, garis vertikal putus, titik sorotan, tap tooltip, dukungan light/dark, data kosong, nilai null (garis putus).
- AC: preview semua varian; tidak ada dependency chart; tap menampilkan nilai yang benar.

**P2.8 Detail Sesi (S08)**
- Header, kartu catatan, tab Segmen, Split km, Grafik (garis pace + batang per km). Tab Peta menyusul di P3.1.
- AC: data sesuai golden/hasil simpan; lari 3,1 km menampilkan 4 baris split dengan baris "0,1 km"; segmen 200 m tipis tampil BORDERLINE dengan keterangan; segmen dilewati tampil DILEWATI; km tercepat disorot; grafik menampilkan garis target per segmen.

**P2.9 Rekor Pribadi (S10, S11)**
- Simpan `BestEffort` saat sesi disimpan; query minimum per jarak; `RecordProgression` (pure); halaman kartu 5 jarak, rekor terbaru, detail dengan grafik dan top 10; lencana "PR saat itu" di S06/S07/S08.
- AC: rekor sesuai test P0.10; menghapus sesi pemegang rekor menurunkan rekor ke yang berikutnya tanpa scan ulang track; sesi yang dikecualikan tidak dihitung; jarak yang belum ditempuh menampilkan "Belum ada".

**P2.10 Ringkasan Mingguan (S09) + kartu di Beranda**
- Pure `WeeklyStatsAggregator` di engine (6.11) lalu UI: navigator minggu, kartu angka, 3 grafik, rekor minggu ini, daftar sesi. Sambungkan kartu mini di S02.
- AC: minggu dimulai Senin (uji sesi Minggu 23:30 dan Senin 00:30); minggu kosong tampil nol dan garis pace putus; pace rata-rata berbobot jarak; selisih vs minggu lalu benar; test dengan zona waktu berbeda.

**P2.11 Pengaturan (S12)**
- Semua pengaturan bagian 8: tema (Terang/Gelap/Sistem), bahasa voice (ID/EN), countdown (awal, antar segmen, gaya), jarak cue segmen berbasis jarak (mati/10/20/30/50 m), alert, getar, auto-pause toggle (fungsi di P3.3), layar menyala, uji suara.
- AC: tema berubah langsung tanpa restart dan bertahan setelah app ditutup; semua halaman terbaca di dark dan light; bahasa voice berubah di sesi berikutnya; status paket suara ditampilkan.

---

### FASE P3: Peta, Export, dan Penyempurnaan

**P3.1 Peta rute di Detail Sesi**
- osmdroid: polyline, marker start/finish, penanda batas segmen. Set User-Agent, tile cache, tangani offline.
- AC: rute tampil benar; tanpa API key; tidak crash saat offline; mode gelap tetap terbaca.

**P3.2 Peta live (opsional di S05)**
- Toggle peta di layar lari; default mati.
- AC: tidak memengaruhi akurasi tracking; peta berhenti render saat layar mati.

**P3.3 Auto-pause**
- Sesuai 6.1; voice "dijeda"/"dilanjutkan"; label "DIJEDA OTOMATIS".
- AC: simulasi `stopAndGo` menghasilkan waktu berjalan tanpa durasi berhenti; segmen waktu tidak berjalan saat auto-pause.

**P3.4 Export GPX**
- GPX 1.1 lewat share sheet (`FileProvider`).
- AC: file valid (parser XML di unit test); bisa diimpor ke aplikasi lain.

**P3.5 Cadangan & Data (S13)**
- Backup/restore database + pengaturan ke satu file (SAF), ringkasan data, hapus semua data dua langkah.
- AC: backup lalu restore di instalasi bersih mengembalikan seluruh riwayat, template, dan pengaturan; restore file rusak ditolak dengan pesan jelas.

**P3.6 Ketahanan GPS**
- Alert "Sinyal GPS hilang"/"pulih"; `droppedSampleCount` tersimpan per sesi.
- AC: simulasi `tunnelGap` memicu GpsLost dan GpsRecovered tepat sekali.

**P3.7 Kalibrasi jarak (D08)**
- Hitung faktor koreksi dari jarak sebenarnya, batas 0,95-1,05, simpan di DataStore, diterapkan di `DistanceTracker` untuk sesi berikutnya, nilai yang dipakai disimpan di sesi.
- AC: faktor benar; tidak mengubah sesi lama; reset mengembalikan 1,0.

**P3.8 Pengingat cadangan**
- Notifikasi/ banner sesuai interval di S13 jika belum ada cadangan melewati interval.
- AC: tidak muncul saat pengingat dimatikan; muncul tepat setelah interval terlewati.

---

### FASE P4: Prioritas Terendah

**P4.1 Aksi di notifikasi** — Pause/Resume dan Skip segmen di notifikasi foreground service. AC: bekerja dengan layar terkunci tanpa membuka app.

**P4.2 Kontrol layar kunci / media session** — Opsional: `MediaSession` agar tombol headset bisa pause/resume. AC: tombol headset berfungsi; tidak mengganggu pemutar musik.

**P4.3 Polesan** — Ikon app, splash sederhana, haptic tuning, ukuran font besar. AC: tidak ada layar terpotong pada font scale 1,3x.

---

## 10. Strategi Pengujian

**Unit (engine)**: semua modul P0, P2.5 (namer), P2.9 (progression), P2.10 (weekly), deterministik, tanpa Android.

**Skenario simulasi wajib**

| Skenario | Yang divalidasi |
|---|---|
| steady 7:00 selama 5 km | PASS, nol alert koreksi, 5 split km |
| steady 7:10 | FAIL, TooSlow terpicu setelah gerbang awal |
| slowDown di km 4 | TooSlow muncul, required pace dibacakan |
| speedUp 20+ detik lebih cepat | TooFast terpicu |
| interval 200/400 m | Hanya cue awal dan hasil; BORDERLINE bila tipis; countdown 3-2-1 sebelum segmen berikutnya |
| gpsJump (loncat 80 m) | Sample dibuang, jarak tidak melonjak |
| stopAndGo | Auto-pause, waktu berjalan benar |
| tunnelGap 15 detik | GpsLost/Recovered sekali, jarak lanjut wajar |
| run/walk 6+2 menit sampai 5 km | Transisi waktu tepat, countdown tepat; GoalDistanceReached sekali; tidak berhenti sendiri |
| noisy | Pace live tidak berkedip; alert tidak spam |
| sesi 10 km dengan segmen tercepat di tengah | Best effort 1 km dan 5 km benar dengan offset benar |
| 12 minggu data sintetis | Agregat mingguan, minggu kosong, batas Minggu/Senin |

**Instrumented (perangkat)**: pemulihan proses (P1.5), service dengan layar mati (P1.3), migrasi Room, hapus sesi tanpa baris yatim.

**UI**: Compose preview/screenshot untuk tiap state tiap halaman (loading, kosong, error, normal) di light dan dark.

**Uji lapangan manual (dijalankan pengguna)**
1. Lari 1 km di jalur yang jaraknya diketahui: selisih jarak < 1%.
2. Stable run 3 km @ target realistis: bandingkan split dengan jam tangan/app lain bila ada.
3. Interval pendek (200 m): apakah BORDERLINE masuk akal; apakah countdown 3-2-1 terdengar sebelum ganti segmen.
4. Layar mati 20 menit dengan musik: voice dan beep terdengar, musik kembali.
5. Battery saver agresif: sesi tidak mati.
6. Selesai lari: nama kosong menghasilkan nama sesuai jam; edit nama dan catatan dari riwayat tersimpan.

---

## 11. Risiko dan Mitigasi

| Risiko | Mitigasi |
|---|---|
| Akurasi GPS di segmen pendek | Interpolasi, filter, status BORDERLINE, tanpa alert koreksi di segmen < 400 m |
| Countdown segmen berbasis jarak bersifat prediksi | Dipotong/dibatalkan sesuai 6.6; segmen waktu tepat; dapat dimatikan |
| Latensi TTS untuk "3, 2, 1" | Default beep (ToneGenerator), TTS sebagai pilihan; tick terlambat dibuang |
| Android mematikan service (OEM agresif) | Foreground service, wake lock, snapshot + pemulihan, S14 |
| TTS Indonesia belum terpasang | Deteksi dan arahkan pemasangan voice data; fallback dengan peringatan |
| Alert bawel | Gerbang, hysteresis, cooldown, antrean tunggal, golden test |
| Data hanya lokal | Backup/restore (S13) dan pengingat cadangan |
| Tanpa FK: data yatim | Satu repository transaksional untuk hapus + test "tidak ada baris yatim" |
| Rekor palsu akibat anomali GPS | Filter sample, batas pace 2:30/km, toggle "kecualikan dari rekor" |
| Tile OSM dibatasi | Pemakaian pribadi ringan, cache, User-Agent; rute tetap tampil tanpa tile |
| Perangkat tanpa Google Play Services | Fallback `LocationManager` |

---

## 12. Asumsi (koreksi jika salah)

1. Target pace = **batas paling lambat**; lulus jika rata-rata <= target.
2. "Terlalu cepat" = lebih cepat dari target >= 20 dtk/km (default; per segmen bisa diubah atau dimatikan); tidak dibunyikan untuk segmen < 400 m.
3. Segmen >= 1 km dengan target: tiap km di dalam segmen (dihitung dari awal segmen) juga harus <= target. Bisa dimatikan per segmen.
4. Split km (dari awal workout) hanya di riwayat; split per segmen tampil live (chip dan daftar) dan di riwayat.
5. Voice default Indonesia, bisa English. Km marker lewat voice default aktif.
6. Hasil yang selisihnya dalam error GPS = BORDERLINE, bukan langsung GAGAL.
7. Workout tidak pernah berhenti otomatis. Setelah rencana selesai, app lanjut sebagai segmen `Open` sampai Stop manual.
8. **Ganti segmen tidak menghentikan waktu.** Segmen berbasis waktu: hitung mundur 3-2-1 sebelum batas. Segmen berbasis jarak: satu cue "siap-siap" saat sisa jarak <= 20 m (bisa Mati/10/20/30/50 m). Countdown awal workout 5 detik (bisa 0/3/5/10).
9. Rekor dihitung sebagai **bagian tercepat di dalam sesi** untuk jarak 1 km, 5 km, 10 km, 21,0975 km, dan 42,195 km, memakai waktu berjalan. Sesi yang lebih pendek dari jarak itu tidak masuk.
10. Minggu dimulai Senin. Zona waktu mengikuti yang tersimpan di sesi.
11. Nama default berdasarkan jam mulai (6.12); nama tersimpan sebagai teks akhir. Bahasa nama mengikuti bahasa UI saat disimpan.
12. Form pasca-lari berisi nama, skala capek (RPE 1-10), dan catatan, semuanya opsional. Sesi sudah tersimpan sebelum form muncul; back = Lewati.
13. "Lari Bebas" (segmen tanpa batas dan tanpa target) ditambahkan sebagai template bawaan untuk sekadar mengukur pace/jarak/waktu.
14. Kalibrasi jarak (P3.7) dan pengingat cadangan (P3.8) adalah prioritas rendah dan boleh dihapus dari plan bila tidak diinginkan.
15. Pace rata-rata workout ditampilkan kecil di layar lari (baris Total), karena itu bagian dari kebutuhan awal.

---

## Lampiran: Ringkasan Perubahan dari Versi 1
- Ditambah: countdown awal dan antar segmen (6.6, S05, P0.6, P0.7, P1.4, P1.8); `Limit.Open` dan template Lari Bebas; rekor pribadi (6.10, S10, S11, P0.10, P2.9); statistik mingguan dengan grafik (6.11, S09, P2.10); nama default sesi dan form catatan (6.12, S06, P2.5); edit nama/catatan di riwayat (D03, P2.6); komponen chart (P2.7); kalibrasi jarak dan pengingat cadangan (P3.7, P3.8).
- Diubah: skema data menjadi gaya log tanpa foreign key (bagian 7); spesifikasi lengkap semua halaman dan data per halaman (bagian 8); urutan task P2 disusun ulang sesuai halaman.
16. Pass/fail dan status segmen memakai pace yang **dibulatkan ke detik terdekat (half-up)**, sama dengan yang tampil, supaya layar tidak menampilkan "7:00" tetapi berstatus GAGAL.
17. Auto-pause **tidak retroaktif**: waktu diam sebelum terdeteksi (3 detik) ikut terhitung. Kompromi demi kesederhanaan dan keakuratan transisi segmen waktu.
18. `Repeat` berbasis `untilTotalDistanceM`: setelah jarak tercapai, segmen yang sedang berjalan diselesaikan dulu, lalu repeat berakhir dan app masuk segmen bebas. Tidak berhenti otomatis.
19. Sesi < 100 m tidak dihitung di statistik mingguan dan rekor (tetap muncul di riwayat). Sesi tanpa data cukup untuk sebuah segmen berstatus DATA KURANG.
20. Info segmen (D11) bersifat statis, tersedia ID dan EN, tanpa internet.

---

## Lampiran B: Perubahan Versi 2.1
- Ditambah: **6.14 Katalog Rumus** (semua perhitungan, pembulatan, kasus tepi, dan 20 vektor uji), **8.3 isi Info Segmen (D11)**, task P0.11 (vektor uji), aturan anti-slop no. 16.
- Diubah: segmen berbasis jarak memakai satu cue "siap-siap" pada jarak yang bisa dikonfigurasi (default 20 m), bukan hitung mundur dan bukan peringatan 100 m; auto-pause hold 3 detik; D11 menambah jumlah sheet menjadi 11.
