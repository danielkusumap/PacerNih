# OPEN QUESTIONS

Pertanyaan dan keputusan yang masih menunggu. Yang sudah beres dipindah ke bagian **Selesai**.
Sinkronisasi terakhir: `RUNCOACH_PLAN.md` **Lampiran C** (2026-10-10).

---

# Open

## Q-007 Remote repository — tetap Open (keputusan pemilik proyek)
- Belum ada remote Git (GitHub/GitLab); semua commit masih lokal.
- Rekomendasi plan: repo privat gratis sebagai cadangan. Jangan pernah commit `local.properties`,
  keystore, atau rahasia lain. Plan tidak mewajibkan remote.
- **Dibutuhkan:** keputusan pemilik proyek apakah/bagaimana repo di-push.

## Q-008 P0.11 (audit V1–V22) bergantung pada task P2/P3
- Sebagian vektor 6.14.2 bergantung pada task setelah P0: **V1** (format tampilan `5:33`) di `Formatters.kt`
  (:app, P1.7), **V16–V17** (statistik mingguan, P2.10), **V18** (nama default sesi, P2.5), **V19** (kalibrasi
  jarak, P3.7). Karena itu audit "semua V1–V22 punya test" belum bisa hijau di dalam FASE P0.
- Vektor yang sudah hijau: V2–V15, V20–V22 (FormulaVectorsTest, PhraseBuilderTest, WorkoutRunnerTest).
- **Keputusan yang diminta:** (a) audit P0.11 ditunda sampai task terkait selesai, atau (b) auditnya dibatasi
  ke vektor yang memang milik P0 (V2–V15, V20–V22) dan sisanya menyusul. Usulan: (b).

---

# Selesai

## Q-001 Bagian 6.14 (Katalog Rumus) — SELESAI (2026-10-10)
- 6.14 sudah ada: rumus rinci, aturan pembulatan, kasus tepi, dan vektor uji. **Koreksi:** jumlah vektor
  adalah **V1–V22** (bukan V1–V20 seperti tertulis sebelumnya).
- Semua kode **F** yang dulu terblokir sudah terdefinisi: F3, F8, F13, F15, F17, F20, F24, F26.
- P0.5, P0.7, P0.9, P0.11 tidak lagi terblokir; urutan kerja ada di `PROGRESS.md`.

## Q-002 Bagian 8.3 (Info Segmen / D11) — SELESAI (2026-10-10)
- 8.3 sudah ada: teks statis ID + EN, memakai lima bagian (Kegunaan, Kapan dipakai, Contoh, Perilaku, Tips).
- Hanya relevan untuk **P2.2** (Editor Workout); tidak memblokir P0/P1.

## Q-003 Nilai `EngineConfig` (tabel 6.1) — SELESAI (2026-10-10)
- Dijawab: **ya, ada perubahan** pada 6.1 (Lampiran C.2). Nilai yang sudah dipakai kode
  (akurasi 20 m, kecepatan maksimum 9.0 m/s, pace rekor minimum 150, jarak rekor) **tidak berubah**.
- **Dihapus:** `PRE_SEGMENT_WARNING_M`, `COUNTDOWN_ETA_ABORT_SEC` (belum pernah ditambahkan ke kode).
- **Diubah:** `AUTO_PAUSE_HOLD_SEC` 5 -> **3**; `PRE_SEGMENT_WARNING_SEC` hanya untuk segmen waktu >= 30 dtk;
  `MIN_SEGMENT_SEC_FOR_COUNTDOWN` hanya untuk segmen waktu.
- **Ditambah:** `CUE_DISTANCE_M`, `EARTH_RADIUS_M`, `STATIONARY_SPEED_MPS`, `STATIONARY_MAX_DISP_M`,
  `SPEED_REJECT_RESET_COUNT`, `LIVE_MIN_SAMPLES`, `LIVE_FALLBACK_MIN_SEC`, `MIN_PACE_DISTANCE_M`,
  `MAX_PACE_DISPLAY_SEC`, `HYSTERESIS_SEC_PER_KM`, `NO_CORRECTION_BELOW_SEGMENT_SEC`, `PACE_MARGIN_CAP_SEC`,
  `CALIBRATION_MIN`/`_MAX`, `MIN_STATS_SESSION_M`, `DISCARD_OFFER_SESSION_M`.
- **Tindak lanjut:** penerapan di `EngineConfig` adalah bagian dari task **P0.4b** (bukan lagi pertanyaan terbuka).

## Q-004 Inisialisasi Git — SELESAI (2026-10-10)
- `git init -b main` dijalankan. Commit: P0.1, P0.2, P0.3, P0.4, P0.10.
- `.gitignore` diperbaiki: `build/` (semua modul), `.kotlin/`, `local.properties`, `.gradle/` tidak ikut terlacak.

## Q-005 Tugas P0 yang ditunda — SELESAI (2026-10-10)
- Tidak perlu menunggu lagi. Urutan yang disarankan (Lampiran C):
  **P0.4b -> P0.5 -> P0.6 -> P0.8 -> P0.7 -> P0.9 -> P0.11.**
- P0.6 dan P0.8 dibangun **mengikuti 6.14**, bukan asumsi.
- P0.10 yang dikerjakan lebih dulu (D-009) diterima.

## Q-006 Normalisasi line ending (CRLF) — SELESAI (2026-10-10)
- Keputusan (Lampiran C): **ya**. `.gitattributes` berisi `* text=auto eol=lf` ditambahkan pada commit terpisah.
