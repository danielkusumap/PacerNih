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
