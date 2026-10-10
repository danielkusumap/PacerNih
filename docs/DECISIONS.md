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
