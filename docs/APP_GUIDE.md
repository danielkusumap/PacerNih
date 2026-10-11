# PacerNih — Panduan Aplikasi (Flow, Use Case, Detail Halaman & UI)

Dokumen ini menjelaskan **cara kerja aplikasi PacerNih** secara menyeluruh: alur navigasi, use case, detail tiap
halaman (data, tombol, grafik, state, navigasi), **dan detail UI** (token warna, tipografi, jarak, komponen,
ukuran, serta gaya visual tiap halaman).

Deskriptif (menggambarkan implementasi saat ini). Sumber kebenaran: `RUNCOACH_PLAN.md` (data/alur/rumus) dan
`DESIGN.md` (tampilan).

Versi: label **PacerNih**, `applicationId com.daniel.runcoach`, `minSdk 26`, `targetSdk 34`.

---

## 1. Ringkasan

PacerNih adalah aplikasi lari **offline, tanpa biaya**:
- melacak jarak, pace, dan waktu dari **GPS**;
- menjalankan **workout terstruktur** dengan target pace + suara/beep;
- memberi **koreksi real-time** dan auto-pause;
- menyimpan **riwayat, statistik mingguan, rekor pribadi**;
- **export GPX** dan **cadangan/restore**; 100% data lokal.

---

## 2. Use Case Utama

| Use case | Ringkasan | Layar |
|---|---|---|
| **Lari bebas** | Mulai cepat tanpa target | S02→S04→S05→S06 |
| **Workout terstruktur** | Template dengan target per segmen | S02→S04→S05→S06 |
| **Interval** | Repeat 6x400 m + pemulihan | S03→S04→S05 |
| **Buat/ubah workout** | Susun segmen & repeat | S03 + D01/D02 |
| **Koreksi pace** | "Terlalu lambat/cepat" saat lari | S05 |
| **Auto-pause** | Jeda otomatis saat berhenti | S05/S12 |
| **Riwayat** | Telusuri/edit/hapus sesi | S07 + D03/D09 |
| **Analisis sesi** | Segmen, split km, grafik, peta, GPX | S08 + D08 |
| **Rekor** | 1k/5k/10k/HM/FM + progres | S10→S11 |
| **Statistik mingguan** | Jarak/waktu/sesi + tren 12 minggu | S09 |
| **Kalibrasi jarak** | Sesuaikan faktor jarak | S08(D08)/S12 |
| **Cadangan & restore** | Backup DB + setelan satu file | S13 |
| **Hemat baterai** | Panduan optimasi baterai | S14 |
| **Simulator** (debug) | Uji engine dengan track sintetis | S15 |

---

## 3. Alur Navigasi

```
   pertama kali ─▶ S01 Onboarding ─(Mulai, set onboarding_done)─▶ S02
        ┌───────────── Tab utama (BottomBar) ─────────────┐
        │  S02 Latihan   S07 Riwayat   S09/S10 Statistik   S12 Pengaturan │
        └───┬────────────┬───────────────┬────────────────────┬──────────┘
     pilih  │     buka    │       buka rekor│            Data  │
   workout  ▼      sesi   ▼              ▼                    ▼
          S04 ─▶ S05 ─(Stop tahan)─▶ S06            S11 Detail   S13/S14
                 │                    │ (app mati → auto-recovery)
                 └ snapshot → banner "Lanjutkan" di S02

   S02 ─▶ S03 Editor ─▶ D01 Segmen / D02 Preset / D11 Info
```

Penting:
- **Onboarding hanya sekali** (`onboarding_done`).
- **Bisa mulai kapan saja** (S04 tak menunggu GPS; akurasi ditampilkan).
- **Sesi baru tersimpan saat menekan Simpan** (sebelum itu berupa **draft**).
- **Auto-recovery**: app mati sebelum Simpan → buka app langsung ke S06.

---

## 4. Istilah & Konsep

| Istilah | Arti |
|---|---|
| **Segmen** | Bagian workout: tipe, batas (jarak/durasi/bebas), target pace opsional. |
| **Repeat** | Mengulang sekelompok segmen (sejumlah kali / sampai total jarak). |
| **Target pace maks (`≤`)** | Batas **paling lambat** (rata-rata ≤ target). Lari/tempo/interval. |
| **Target pace min (`≥`)** | Batas **paling cepat** (rata-rata ≥ target). Pemanasan/pendinginan/pemulihan. |
| **Margin "terlalu cepat"** | Detik lebih cepat dari target yang memicu alert. |
| **Per-km check** | Untuk segmen ≥ 1 km bertarget: tiap km penuh juga harus memenuhi target. |
| **Auto-pause** | Jeda otomatis saat `speed < 0.6 m/s` ≥ 3 dtk; lanjut saat bergerak. |
| **Moving time** | Waktu berjalan (tanpa pause). |
| **Kalibrasi jarak** | Faktor koreksi 0,95–1,05 untuk sesi berikutnya. |
| **Rekor pribadi** | Waktu terbaik per jarak dari bagian tercepat dalam sesi. |
| **Draft** | Sesi selesai belum disimpan (`pending`), tersembunyi dari Riwayat/Statistik/Rekor. |
| **Snapshot** | Data sesi berjalan, agar bisa dilanjutkan bila app mati. |

---

## 5. Sistem UI (Token & Komponen)

### 5.1 Warna (token; dynamic color dimatikan)

**Inti (dark / light):**

| Token | Dark | Light | Dipakai untuk |
|---|---|---|---|
| `bg` | `#0B0D0F` | `#F4F5F1` | Latar layar |
| `surface` | `#14181C` | `#FFFFFF` | Kartu, sheet, bottom bar |
| `surfaceRaised` | `#1C2127` | `#FFFFFF` | Kartu di atas kartu, dialog, input |
| `outline` | `#2A3139` | `#E1E4DC` | Garis pemisah, border kartu |
| `outlineStrong` | `#3A444E` | `#C5CABF` | Border input, tombol sekunder |
| `textPrimary` | `#F2F5F7` | `#0E1114` | Teks & angka utama |
| `textSecondary` | `#A3ADB7` | `#4A545D` | Label, keterangan |
| `textTertiary` | `#7C8791` | `#66707A` | Keterangan sangat kecil, placeholder |
| `accent` | `#C6F432` | `#B7E41C` | Tombol utama, indikator aktif, aksen grafik |
| `onAccent` | `#0B0D0F` | `#0E1114` | Teks/ikon di atas aksen |
| `accentText` | `#C6F432` | `#4F6F00` | Teks aksen di atas bg/surface |
| `accentContainer` | `#2D371F` | `#F1FAD2` | Latar lembut berisi aksen |
| `scrim` | hitam 60% | hitam 40% | Latar sheet/dialog |

**Status** (hanya untuk status, **tidak** untuk tombol): `pass` `#34D399`/`#0B7A55`, `fail` `#FF5D5D`/`#C42B2B`,
`warn` `#FFB547`/`#9A5B00`, `fast` `#5BB5FF`/`#135FA8`, `neutral` (textSecondary) — masing-masing dengan container
lembut (mis. `failContainer` `#372222`/`#FBE2E2`).

**Tipe segmen** (untuk strip/penanda): `kindRun` lime, `kindWalk` biru-kelabu, `kindRecovery` teal, `kindWarmCool` ungu.

Aturan: teks normal ≥ 4,5:1, teks besar/ikon ≥ 3:1 (diuji otomatis). Pace hero selalu `textPrimary` (bukan warna status).

### 5.2 Tipografi (Inter, angka tabular)

| Token | Ukuran/berat | Dipakai |
|---|---|---|
| `displayHero` | 96sp ExtraBold | Pace live (autosize → 72sp bila sempit) |
| `displayL` | 56sp Bold | Jarak di S06, rekor di S11 |
| `numberXL` | 40sp Bold | Jarak S08, rekor S10 |
| `numberL` | 32sp Bold | Metrik segmen, target |
| `numberM` | 24sp SemiBold | Angka baris statistik |
| `titleL` | 28sp Bold | Judul halaman tab |
| `titleM` | 20sp SemiBold | Judul kartu/dialog/sheet |
| `titleS` | 16sp SemiBold | Nama item daftar |
| `body` / `bodySmall` | 16/14sp Regular | Teks isi / keterangan |
| `label` | 12sp SemiBold, KAPITAL, +6% | Label metrik |
| `caption` | 12sp Regular | Sumbu grafik, catatan kecil |

Aturan: tidak ada teks < 12sp; judul maks 2 baris (elipsis); angka tabular (`tnum`).

### 5.3 Jarak, Bentuk, Gerak
- Grid 4dp: `s1`4 · `s2`8 · `s3`12 · `s4`16 · `s5`24 · `s6`32 · `s7`48. Padding layar 20dp (16dp bila < 360dp).
- Radius: `8` (chip/input kecil), `14` (tombol/input/status band), `20` (kartu), `28` (sudut sheet/dialog).
- **Tanpa bayangan**; kedalaman lewat lapisan warna + border `outline`.
- Gerak: 180ms `easeOut`; sheet 240ms; dihormati "hapus animasi" sistem.
- Area sentuh minimal **48×48dp**.

### 5.4 Komponen (spesifikasi)

| Komponen | Spesifikasi ringkas |
|---|---|
| **RcButton** | Tinggi 56dp (kecil 44), radius 14. Varian: primary (isi aksen/teks onAccent), secondary (border outlineStrong 1,5dp), tertiary (teks aksen), destructive (teks fail), destructive-filled (isi fail). State: pressed/disabled 38%/loading (spinner 20dp). Ikon opsional 20dp. |
| **RcIconButton** | Area 48dp, ikon 24dp; varian plain/filled (lingkaran surfaceRaised 56dp)/accent. |
| **RcCard** | Isi surface, radius 20, padding 16, border outline 1dp; `interactive` memberi efek tekan. |
| **StatusBadge** | Tinggi 44dp, radius 14, container status + ikon 24dp + teks titleS KAPITAL + selisih di kanan. |
| **ResultChip** | Tinggi 28dp, radius 8, container status + ikon 16dp + teks label. |
| **MetricBlock** | Label (label, textSecondary) di atas; nilai (numberM/numberL) + satuan kecil di samping. |
| **HeroPace** | displayHero rata tengah, "/km" (titleS) di bawah; autosize 96→72sp; `--:--` saat tak ada. |
| **SegmentProgressBar** | Tinggi 10dp, radius 5, trek outline, isi warna tipe segmen. |
| **SegmentStrip / ResultStrip** | Blok tipis tinggi 8dp, radius 3, jarak 2dp; warna tipe (SegmentStrip) / status (ResultStrip). |
| **SectionHeader** | Label KAPITAL textSecondary, jarak atas 24dp bawah 8dp; aksi teks opsional di kanan. |
| **ListRow** | Tinggi min 56dp; kiri konten, kanan kontrol; pemisah outline inset. |
| **TopBar** | Tinggi 56dp (64dp di halaman tab); judul kiri; ikon aksi kanan 48dp; panah kembali. |
| **RcBottomBar** | Tinggi 64dp + inset; ikon 24dp + label 12sp; aktif = aksen + **batang aksen 3×24dp** di atas ikon. |
| **RcSheet** | Bottom sheet surface, radius atas 28, pegangan 4×36dp, padding 20, scrim. |
| **RcDialog** | surfaceRaised, radius 28, lebar maks 320dp, padding 24, tombol rata kanan. |
| **RcTextField** | Tinggi 56dp (multiline min 96), surfaceRaised, border outlineStrong 1,5dp; fokus aksen 2dp; error fail + teks. |
| **NumberStepper** | Tombol −/+ 48dp, nilai numberM di tengah, label langkah. |
| **SegmentedControl** | Tinggi 44dp, radius 14, trek surfaceRaised, pilihan aktif isi aksen/teks onAccent. |
| **RcSwitchRow** | ListRow + switch (trek 52×32; aktif aksen/thumb onAccent). |
| **Banner** | Tinggi min 48dp, radius 14, ikon 24dp + teks bodySmall bold + aksi teks opsional. Varian info/warning/success/accent. |
| **EmptyState / ErrorState** | Lingkaran 72dp + ikon 32dp, judul titleM, satu kalimat body. |
| **Skeleton** | Blok surfaceRaised berdenyut, radius sesuai elemen. |
| **ProgressRing** | Cincin 3dp mengelilingi tombol Stop, terisi `fail` selama **2 dtk** tekan-tahan; lepas sebelum penuh = batal; **penuh = langsung konfirmasi**. |

### 5.5 Gaya Grafik (CH1–CH6)
- Latar transparan, tanpa kotak; garis bantu horizontal 1dp outline (maks 4); tanpa garis vertikal (kecuali batas segmen).
- Label sumbu `caption` `textTertiary`; sumbu Y kiri (3–4 label).
- **Batang**: lebar 60% slot, sudut atas radius 4, isi aksen (terpilih/terbaru) atau aksen 35% (lainnya); nilai 0 = garis 2dp outline.
- **Garis**: tebal 3dp aksen, ujung bulat; area gradien aksen 18%→0%; nilai null = garis putus.
- **Garis target** (tangga): 2dp putus-putus `textSecondary`. **Batas segmen**: 1dp putus-putus outlineStrong.
- **Tooltip**: kartu surfaceRaised radius 12, padding 8×12, angka + keterangan; muncul saat sentuh/seret.
- Tinggi: CH1/CH6 200dp; CH2–CH5 160dp. Animasi masuk sekali (garis 300ms / batang 250ms), hormati "hapus animasi".

---

## 6. Detail Halaman

Legenda: **Tujuan · Data · Kontrol · Grafik · UI · State · Navigasi**.

### S01 — Onboarding & Izin
- **Tujuan:** memandu izin, satu keputusan per langkah.
- **Data:** 4 langkah (indikator 4 titik di atas; aktif = batang aksen 24×6dp, lain = titik 6dp). Isi: intro; izin lokasi; izin notifikasi; baterai. Status izin + banner (GPS mati / izin belum).
- **Kontrol:** primer (Berikan izin/Lanjut/Mulai), tersier (Lewati), sekunder (Buka pengaturan baterai).
- **UI:** tanpa TopBar; di tengah lingkaran **112dp** `accentContainer` berisi ikon **56dp** `accentText`; judul `titleL`; penjelasan `body` `textSecondary`; tombol bawah lebar penuh; status izin = ikon `check_circle` pass / `cancel` fail + teks.
- **State:** izin ditolak tidak memblokir (banner di S04).
- **Navigasi:** Mulai → set `onboarding_done` → S02; Buka pengaturan baterai → S14.

### S02 — Beranda (tab Latihan)
- **Data:** banner "Lanjutkan sesi" (bila snapshot); kartu Lari Bebas; kartu ringkasan minggu ini (Jarak, Sesi, Selisih vs minggu lalu); daftar template (nama, strip segmen, chip ringkasan, tombol Mulai, "terakhir dipakai").
- **Kontrol:** kartu Lari Bebas → S04; Mulai → S04; menu ⋮ (Edit/Duplikat/Hapus); tombol mengambang **Buat** → S03; BottomBar.
- **UI:** TopBar `titleL` "Latihan". Kartu Lari Bebas **aksen penuh**, tinggi **96dp**, radius 20, teks `onAccent`, lingkaran 56dp play di kanan. Kartu template: nama `titleS`, strip segmen, chip `bodySmall`, tombol **Mulai** kecil (44dp) di kanan. Tombol **Buat**: pil radius 18, isi surfaceRaised, border outlineStrong, ikon `add` + "Buat" (bukan FAB bulat). Banner lanjutkan = varian aksen.
- **State:** loading = skeleton kartu; kosong = EmptyState + "Buat workout pertama".
- **Navigasi:** ke S03/S04/S07/S09/S12; "Simulator" (debug) → S15.

### S03 — Editor Workout
- **Data:** nama + target jarak total; bar ringkasan (strip + "N segmen · ≥ X km · ≥ Y mnt"); kartu segmen (batang kiri warna tipe 4dp, ikon tipe, nama, detail, ⓘ, ⋮); blok Repeat (kurung aksen kiri 3dp, "Ulangi 3x", anak, "+ Segmen di sini"); pesan validasi.
- **Kontrol:** TopBar kembali + **Simpan** (tertiary, nonaktif bila invalid); baris bawah **+ Segmen / + Repeat / Preset**; dialog "Buang perubahan?".
- **UI:** kartu segmen tinggi min 72dp radius 20; batang tipe 4dp; ikon tipe 24dp; menu ⋮ berisi Naik/Turun/Duplikat/Hapus; baris bawah menempel (sticky) dengan tombol secondary.
- **Navigasi:** kembali/Simpan → S02; Preset → D02; Info → D11; kartu → D01.

### S04 — Pra-start
- **Data:** TopBar kembali + nama workout; strip segmen + ringkasan; kartu status GPS (batang sinyal, angka akurasi `numberL`, chip Siap/Lemah/Mencari sinyal); daftar segmen ringkas (ikon tipe + deskripsi); pengaturan cepat (bahasa voice, countdown awal).
- **Kontrol:** **MULAI** (tombol besar, aktif bila izin lokasi ada — **tanpa menunggu GPS**).
- **UI:** kartu GPS tinggi ~120dp; batang sinyal 5 batang (8–40dp) berwarna status; chip status memakai ResultChip; tombol MULAI tinggi **64dp**, `titleM` KAPITAL, ikon play; baris pengaturan cepat pakai SegmentedControl.
- **Navigasi:** MULAI → S05.

### S05 — Layar Lari
- **Data (A–H):** A header (nama; ikon GPS + akurasi; toggle peta; chip DIJEDA). B segmen (ikon, **nama segmen**, "SEGMEN x/y", progress bar, sisa + "dari …"). Banner (ganti segmen/cue/GPS hilang/rencana selesai/target tercapai). C hero (pace besar, "/km", TARGET `≤/≥`, StatusBadge). D metrik segmen (rata-rata; butuh sisa). E total (waktu, jarak, pace rata-rata). F berikutnya. G kontrol. H baris Perpanjang/Daftar segmen.
- **Kontrol:** Lewati (ikon), **Jeda/Lanjut** (lingkaran besar), **Stop tekan-tahan 2 dtk**, Perpanjang (+100/+500 m; +30/+60 s), Daftar segmen, toggle peta, kembali (dialog).
- **UI:** **tidak bisa di-scroll**; hero autosize 96→72sp; StatusBadge tinggi 44dp slot tetap; tombol Jeda **lingkaran 88dp** aksen; Stop dikelilingi ProgressRing; chip DIJEDA neutral; konten diredupkan 55% saat pause; target `≤ 6:30`/`≥ 8:25`.
- **State:** WAITING_GPS, COUNTDOWN overlay, RUNNING, PAUSED, GPS hilang (`--:--`), rencana selesai.
- **Navigasi:** Stop → S06.

### S06 — Simpan Sesi
- **Data:** "Sesi siap disimpan"; jarak besar; metrik Waktu/Pace(`/km`)/Segmen lulus; ResultStrip + ringkasan; banner rekor; Nama sesi; skala capek 1–10; Catatan + penghitung.
- **Kontrol:** **Simpan** (primer) → simpan; **Buang sesi** (destruktif) → dialog konfirmasi. Tanpa "Lewati".
- **UI:** jarak `displayL`; metrik `MetricBlock`; ResultStrip; field nama/catatan RcTextField; skala capek 10 lingkaran 28dp (terpilih aksen); footer menempel (sticky) dengan 2 tombol.
- **State:** "Menyimpan sesi…" saat menunggu draft; auto-recovery bila app mati.
- **Navigasi:** Simpan/Buang → S02.

### S07 — Riwayat
- **Data:** TopBar + pencarian; header bulan ("OKTOBER 2026" + "42,5 km · 9 sesi"); item sesi (nama; tanggal/jam; jarak besar + km; waktu; pace `/km`; chip hasil "N/M").
- **Kontrol:** kartu → S08; ⋮ (Edit D03 / Hapus D09).
- **UI:** kartu sesi radius 20; angka jarak `numberM` di kanan; chip hasil memakai ResultChip (warna status).
- **State:** skeleton; kosong ("Belum ada sesi" + "Mulai lari"); hasil pencarian kosong.

### S08 — Detail Sesi
- **Data:** TopBar (kembali; ikon **peta** toggle; ⋮: Edit/Export GPX/Kalibrasi/Hapus); judul + edit; tanggal/jam; nama workout; jarak besar; metrik Waktu/Pace(`/km`)/Hasil; jeda & status rencana; ResultStrip; kartu catatan; tab Segmen/Split km/Grafik/(Peta).
- **Tab Segmen:** "#3 Interval 1" + chip; "Batas 200 m"; Jarak & Waktu; **TARGET**/**AKTUAL**(`/km`, berwarna)/**SELISIH**; "Km di dalam segmen" (buka → daftar km).
- **Tab Split km:** tabel **KM | WAKTU | PACE (/km) | SELISIH**.
- **Tab Grafik:** **CH2** pace per km (batang), **CH1** pace sepanjang jarak (garis, sumbu dibalik).
- **Tab Peta:** polyline rute aksen, start/finish, batas segmen.
- **Kontrol:** menu aksi; ketuk "Km di dalam segmen"; sentuh/seret grafik (tooltip).
- **UI:** tab dengan garis bawah **3dp aksen**; chip hasil; tabel split tinggi baris 52dp.
- **Navigasi:** Edit/Kalibrasi (D08)/Hapus; Export GPX → share sheet.

### S09 — Ringkasan Mingguan (Statistik ▸ Mingguan)
- **Data:** navigator minggu (panah, label, "Minggu ini"); grid 2×3 kartu angka (Jarak + selisih, Waktu, Sesi, Pace, Sesi terpanjang, Segmen lulus %); daftar sesi minggu ini.
- **Grafik:** **CH3** jarak per hari (batang), **CH4** jarak 12 minggu (batang), **CH5** pace rata-rata/minggu (garis).
- **UI:** kartu angka tinggi 84dp; grafik tinggi 160dp; hari ini/minggu terpilih disorot aksen.
- **State:** minggu kosong → grafik tetap tampil + pesan.

### S10 — Rekor Pribadi (Statistik ▸ Rekor)
- **Data:** 5 kartu rekor (1k/5k/10k/HM/FM): waktu besar, "pace x/km", tanggal + nama sesi, ikon piala; kartu "Belum ada" = garis putus-putus. Daftar "Rekor terbaru". Catatan sumber rekor.
- **UI:** kartu tinggi 104dp; waktu `numberXL`; piala `accentText`.
- **Navigasi:** kartu → S11.

### S11 — Detail Rekor
- **Data:** TopBar; hero (waktu `displayL`, pace, tanggal + nama sesi → S08, "dari km x"); SegmentedControl Waktu/Pace; Top 10 (peringkat, waktu, pace + tanggal, nama).
- **Grafik:** **CH6** best effort per sesi (garis) + garis rekor (tangga). < 2 sesi → pesan.
- **UI:** grafik tinggi 200dp.

### S12 — Pengaturan
- **Kelompok:** TAMPILAN (Tema, Layar tetap menyala), VOICE (bahasa, Uji suara, status paket, km marker, alert pace), HITUNG MUNDUR (awal, antar segmen, jarak cue, gaya), ALERT (margin terlalu cepat, getar), TRACKING (auto-pause, kalibrasi + Reset), DATA → S13, BANTUAN → S14 + versi, PENGEMBANG (debug) → S15 + Katalog.
- **UI:** tiap kelompok = RcCard dengan SectionHeader KAPITAL; kontrol pakai SegmentedControl / RcSwitchRow / NumberStepper.

### S13 — Cadangan & Data
- **Data:** kartu ringkasan (Sesi/Template/Ukuran KB); "Cadangan terakhir"; banner pengingat; SegmentedControl pengingat (Mati/2 minggu/Bulanan); Zona bahaya.
- **Kontrol:** Cadangkan ke file (SAF); Pulihkan dari file (+ peringatan menimpa); Hapus semua data (dialog ketik "HAPUS").
- **UI:** Zona bahaya = kartu border `fail` 1dp, tombol destructive-filled.

### S14 — Panduan Baterai
- **Data:** penjelasan; status optimasi (pass/warn); chip merek (gulir horizontal); kartu langkah sesuai merek.
- **Kontrol:** Buka pengaturan baterai (primer); Buka pengaturan aplikasi (sekunder).
- **UI:** chip pil (aktif = aksen); kartu langkah body.

### S15 — Simulator (debug)
- **Data:** workout (Interval 200/400); chip skenario (STEADY…NOISY); SegmentedControl kecepatan 1x/10x/60x; panel log monospace (ringkasan event + jumlah sample dibuang).
- **Kontrol:** Jalankan (loading saat berjalan).
- **Katalog Komponen (debug):** menampilkan seluruh komponen UI untuk dibandingkan dengan spesifikasi.

---

## 7. Sheet & Dialog

| ID | Nama | Isi / Fungsi | Bentuk UI |
|---|---|---|---|
| **D01** | Editor Segmen | Label; grid tipe (ikon+nama); batas (Jarak/Durasi/Tanpa batas); input jarak/durasi; target pace; **Arah target (≤ Maks / ≥ Min)**; margin "terlalu cepat"; switch per-km; Selesai | Sheet tinggi penuh |
| **D02** | Preset | Form parameter + buat: Stable run, Progressive tempo, Interval, Run/walk | Sheet |
| **D03** | Edit Sesi | Nama (placeholder default), skala capek, catatan, "jangan hitung untuk rekor"; Simpan/Batal | Dialog (inline S07/S08) |
| **D04** | Lanjutkan Sesi | Nama workout + waktu tersimpan; Lanjutkan/Buang | **Banner** di S02 |
| **D05** | Konfirmasi Stop | Tetap lari / Stop | Dialog di S05 |
| **D06** | Konfirmasi Skip | — | Tidak ada dialog (Lewati langsung) |
| **D07** | Perpanjang Segmen | +100/+500 m; +30 s/+1 mnt | Dialog di S05 |
| **D08** | Kalibrasi Jarak | Jarak terukur; input jarak sebenarnya (km); faktor baru (0,95–1,05); "Terapkan untuk sesi berikutnya" | Sheet di S08 |
| **D09** | Konfirmasi Hapus | Hapus sesi (konsekuensi); "Hapus semua data" = dua langkah (ketik HAPUS) | Dialog |
| **D10** | Daftar Segmen (live) | Daftar **nama segmen** + detail, status berjalan | Sheet di S05 |
| **D11** | Info Segmen & Istilah | Penjelasan tipe/batas/target/margin/per-km/repeat/cue; "Mengerti" | Sheet |

---

## 8. Data yang Disimpan

### Database (Room) — `runcoach.db` (skema v5)
| Tabel | Isi |
|---|---|
| `Session` | Sesi: nama, catatan, RPE, waktu, elapsed/moving/paused, jarak, pace rata-rata, segmen lulus/gagal/borderline, status rencana, faktor kalibrasi dipakai, sample dibuang, **pending** (draft). |
| `SegmentResult` | Hasil segmen: label, tipe, batas, target, **arah target**, jarak, durasi, pace, selisih, status. |
| `KmSplit` | Split per km: jarak, durasi, pace, target, selisih, status. |
| `BestEffort` | Best effort per jarak (rekor). |
| `TrackPoint` | Titik GPS sesi. |
| `WorkoutTemplate` | Template workout (nama, JSON, bawaan, terakhir dipakai). |
| `ActiveRunSnapshot` | Snapshot sesi berjalan (resume). |

### Setelan (DataStore)
Tema; bahasa voice; countdown awal & antar segmen; jarak cue; gaya countdown; margin terlalu cepat; getar;
auto-pause; layar tetap menyala; km marker; alert pace; peta live; peta riwayat; faktor kalibrasi; pengingat
cadangan (hari); cadangan terakhir; **onboarding selesai**.

---

## 9. Perizinan

| Izin | Untuk | Wajib |
|---|---|---|
| `ACCESS_FINE/COARSE_LOCATION` | Rute & pace | Ya (untuk lari) |
| `POST_NOTIFICATIONS` | Notifikasi sesi (Android 13+) | Dianjurkan |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_LOCATION` | Sesi jalan saat layar mati | Ya |
| `WAKE_LOCK`, `VIBRATE`, `INTERNET` | Layar bangun, getar, tile peta | Otomatis |

Tidak meminta lokasi latar belakang.

---

## 10. Rumus & Aturan Penting

- **Jarak efektif**: `dEff = d × faktor kalibrasi` (0,95–1,05).
- **Kalibrasi (F25)**: `rawM = total / faktorLama`; `faktorBaru = clamp(sebenarnya / rawM, 0,95, 1,05)`. Tidak menumpuk.
- **Auto-pause (F26)**: `speed < 0.6 m/s` ≥ 3 dtk → jeda; lanjut saat bergerak. Tidak retroaktif.
- **Sample dibuang**: akurasi > 20 m, timestamp tidak naik, kecepatan tersirat > 9 m/s.
- **GPS hilang**: tanpa sample valid ≥ 10 dtk.
- **Segmentasi (F17)**: margin GPS `min(target × 6 / jarak, 15)` detik. Target **maks** lulus bila pace ≤ target; target **min** lulus bila pace ≥ target.
- **Rekor**: dari bagian tercepat dalam sesi (window per jarak).

---

## 11. Catatan & Penyimpangan

- **S06 tidak auto-save**: draft → final saat Simpan (auto-recovery saat app dibuka).
- **Mulai tanpa menunggu GPS**; akurasi ditampilkan.
- **Target pace dua arah**: `≤ Maks` (lari) & `≥ Min` (pemanasan/pendinginan/pemulihan).
- **D04/D06** tidak berbentuk dialog terpisah (lihat bagian 7).
- Keputusan lain: `docs/DECISIONS.md`; status tugas: `docs/PROGRESS.md`.
