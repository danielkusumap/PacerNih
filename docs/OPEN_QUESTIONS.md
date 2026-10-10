# OPEN QUESTIONS

## Q-001 Bagian 6.14 (Katalog Rumus) belum ada di `RUNCOACH_PLAN.md`
Dokumen menyebut 6.14 sebagai sumber kebenaran (rumus, pembulatan, kasus tepi, dan 20 vektor uji
V1-V20) dan aturan anti-slop #16 menyatakan 6.14 mengikat. Namun bagian ini tidak ada di file.
Dampak: P0.5, P0.7, P0.8, P0.9, dan P0.11 belum bisa dikerjakan dengan benar.
Asumsi sementara: kerjakan dulu P0.1-P0.4, P0.6, P0.10, lalu tunda sisanya sampai 6.14 masuk.

## Q-002 Bagian 8.3 (isi Info Segmen / D11) belum ada
D11 dirujuk oleh 8.1 dan D01/D02, serta task P2.2, tetapi isinya belum ada.

## Q-003 Sumber nilai `EngineConfig` (tabel 6.1)
Tabel 6.1 sudah memuat nilai awal. Perlu konfirmasi apakah 6.14 (bila ada) akan mengubah nilai ini
sebelum P0.3 (SampleFilter) dimulai.

## Q-004 Inisialisasi Git
Plan aturan #3 ("satu task = satu commit") mengasumsikan repo Git, tetapi folder proyek belum
merupakan repo Git. Perlu keputusan apakah `git init` dijalankan.
