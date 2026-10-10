# OPEN QUESTIONS

Pertanyaan dan keputusan yang masih menunggu. Yang sudah beres dipindah ke bagian **Selesai**.
Setiap item mencatat: apa masalahnya, dampaknya ke task mana, dan apa yang dibutuhkan untuk menutupnya.

---

# Open

## Q-001 Bagian 6.14 (Katalog Rumus) belum ada di `RUNCOACH_PLAN.md`
- 6.14 dideklarasikan sebagai **sumber kebenaran**: rumus rinci, aturan pembulatan, kasus tepi, dan
  20 vektor uji **V1–V20** (6.14.2). Aturan anti-slop #16 mengikat ("wajib diimplementasikan persis"),
  dan bagian 6 menyatakan 6.14 mengalahkan 6.2–6.13 bila berbeda.
- Banyak perilaku hanya didefinisikan sebagai kode **F** yang tempatnya di 6.14:
  - F3 = aturan jitter saat diam (dirujuk 6.1)
  - F8 = syarat data pace live (6.1)
  - F13 = margin keluar dari kondisi alert / hysteresis (6.1)
  - F15 = pemicu presisi countdown & cue jarak (6.5, 6.6, 6.7)
  - F17 = batas atas margin BORDERLINE (6.1)
  - F20 = format angka untuk suara, dasar snapshot test (6.7, 6.8)
  - F24 = estimasi durasi workout (S03)
  - F26 = auto-pause (6.1, 6.5)
- **Dampak:** P0.5, P0.7, P0.9, dan P0.11 terblokir penuh; P0.6 dan P0.8 berisiko harus ditulis ulang.
- **Dibutuhkan:** isi lengkap 6.14 (rumus + pembulatan + V1–V20).

## Q-002 Bagian 8.3 (isi Info Segmen / D11) belum ada
- 8.3 berisi teks penjelasan statis (ID + EN) untuk tiap tipe segmen/istilah, ditampilkan lewat sheet
  D11 (ikon ⓘ) di S03/D01/D02.
- **Dampak:** hanya task P2.2 (Editor Workout). **Tidak memblokir P0 maupun P1.**
- **Dibutuhkan:** isi 8.3.

## Q-003 Sumber & finalisasi nilai `EngineConfig` (tabel 6.1)
- Tabel 6.1 sudah memuat nilai awal, dan sudah dipakai di P0.3/P0.4/P0.10 via `EngineConfig` (data class).
- **Perlu konfirmasi:** apakah 6.14 nanti mengubah nilai 6.1? Bila ya, `EngineConfig` direvisi dan test
  yang bergantung padanya disesuaikan.
- Sementara: memakai nilai 6.1 apa adanya.

## Q-005 Tugas P0 yang ditunda karena 6.14 belum ada
- Sudah selesai: P0.1, P0.2, P0.3, P0.4, P0.10.
- Belum: P0.5, P0.6, P0.7, P0.8, P0.9, P0.11.
- **Keputusan yang diminta:** tunggu 6.14, atau izinkan P0.6 + P0.8 dibangun sekarang dengan asumsi
  dari 6.5/6.8 (risiko ditulis ulang; hasil asumsi dicatat di `DECISIONS.md`).
- Catatan: gerbang P0 di plan melarang masuk P1 sebelum semua AC P0 hijau.

## Q-006 Normalisasi line ending (CRLF)
- Git memunculkan warning `LF will be replaced by CRLF` (efek `core.autocrlf` di Windows). Belum ada `.gitattributes`.
- **Keputusan yang diminta:** tambahkan `.gitattributes` (`* text=auto eol=lf`) atau biarkan seperti ini.

## Q-007 Remote repository
- Belum ada remote Git (GitHub/GitLab); semua commit masih lokal.
- **Keputusan yang diminta:** apakah/bagaimana repo akan di-push ke remote.

---

# Selesai

## Q-004 Inisialisasi Git — SELESAI (2026-10-10)
- `git init -b main` dijalankan. Commit: P0.1, P0.2, P0.3, P0.4, P0.10.
- `.gitignore` diperbaiki: `build/` (semua modul), `.kotlin/`, `local.properties`, `.gradle/` tidak ikut terlacak.
