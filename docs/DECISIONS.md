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
