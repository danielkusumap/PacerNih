# DESIGN-RESKIN.md: Spesifikasi Re-skin PacerNih

Re-skin **tampilan saja**. Isi, urutan, dan perilaku tiap halaman **tidak berubah** (sumber: `APP_GUIDE.html`, `RUNCOACH_PLAN.md`). Dokumen ini menggantikan bagian visual `DESIGN.md` (font, ukuran, gaya komponen). Token warna sama.

## 0. Aturan Utama (baca dulu)
1. **Sumber kebenaran visual = folder `mock/`.** Buka `mock/index.html` untuk melihat semua layar. Jika kata-kata di dokumen ini berbeda dengan HTML/CSS, **HTML/CSS yang menang**. `mock/reskin.css` adalah daftar style induk; nilai di bawah diambil darinya.
2. **Jangan mengubah isi halaman.** Semua teks, angka, dan nama sesi di mock adalah **contoh**. Data nyata tetap dari app. Jangan menambah atau menghapus elemen; hanya ganti tampilannya.
3. **Mock hanya tema gelap.** Tema terang tidak digambar. Turunkan dari token terang di `DESIGN.md` bagian 3 (struktur, ukuran, dan font sama; hanya warna yang berganti).
4. **Satuan:** angka `px` di mock = `dp`/`sp` dengan lebar acuan **390**. Di layar lebih sempit, skala layout secara proporsional; angka hero dan judul boleh mengecil otomatis (autosize).
5. Semua nilai warna, font, dan ukuran lewat token (jangan literal di layar). Jangan menambah dependency.
6. Elemen bertanda **[placeholder]** di mock (mis. langkah baterai) berisi teks contoh; pakai teks asli app.

## 1. Token

### 1.1 Warna (gelap)
| Token | Hex | Pakai |
|---|---|---|
| bg | #0B0D0F | Latar layar |
| surface | #14181C | Kartu, sheet, bottom bar |
| surfaceRaised | #1C2127 | Input, ikon-tombol, kontrol segmen, dialog |
| outline | #2A3139 | Garis, border kartu |
| outlineStrong | #3A444E | Border input/tombol sekunder, pegangan sheet, trek switch mati |
| textPrimary | #F2F5F7 | Teks/angka utama |
| textSecondary | #A3ADB7 | Label, keterangan |
| textTertiary | #7C8791 | Keterangan sangat kecil |
| accent | #C6F432 | Tombol utama, indikator aktif, aksen |
| onAccent | #0B0D0F | Teks/ikon di atas accent |
| accentContainer | #2D371F | Banner/lencana aksen, kotak ikon |
| pass / container | #34D399 / #19342F | LULUS, STABIL, GPS siap |
| fail / container | #FF5D5D / #372222 | GAGAL, TERLALU LAMBAT, hapus |
| warn / container | #FFB547 / #373022 | BORDERLINE, peringatan |
| fast / container | #5BB5FF / #1F303E | TERLALU CEPAT (tidak digambar di mock, pakai token ini) |
| neutral container | #232A31 (teks textSecondary) | TANPA TARGET, chip biasa |
| scrim | rgba(0,0,0,.62) | Latar di belakang sheet/dialog |

Warna tipe segmen (strip, penanda kiri kartu, ikon): **Lari/Lari bebas #C6F432**, **Jalan #8FA3B8**, **Pemulihan #6FB7C4**, **Pemanasan & Pendinginan #B8A6E8**. Selalu disertai ikon/teks. Warna aksen **tidak pernah** dipakai untuk status; warna status tidak dipakai untuk tombol.

### 1.2 Font
Dibundel di app (gratis, OFL), tanpa unduh runtime:
- **Barlow Condensed** (500/700/800): semua angka besar, judul, label, tombol, tab.
- **Manrope** (400/600/700): teks biasa (nama, keterangan, isi kartu).
Angka selalu **tabular** (`tnum`).

| Gaya | Spesifikasi |
|---|---|
| Angka (`.n`) | Barlow Condensed 800, letter-spacing -0.01em, line-height 1, tabular |
| Label (`.l`) | Barlow Condensed 700, 12px, letter-spacing .14em, **KAPITAL**, warna textSecondary |
| Tombol | Barlow Condensed 800, 17px, letter-spacing .12em, KAPITAL |
| Chip | Barlow Condensed 700, 12px, letter-spacing .1em, KAPITAL |
| Tab bawah | Barlow Condensed 700, 11px, letter-spacing .1em, KAPITAL |
| Isi | Manrope 14px / 1.45 (nama item 16px bold; keterangan 13px/12px textSecondary) |

Ukuran angka yang dipakai: pace hero S05 **150**; jarak S06 **96**; hero S11 **96**; jarak S08 **72**; GPS S04 **56**; judul besar S01/S04 **44**; judul halaman tab **34** (LATIHAN, RIWAYAT, STATISTIK, PENGATURAN, nama sesi S08, "LARI BEBAS"); judul sheet **28**; judul dialog **26-28**; metrik **22-34**; rekor S10 **40**; angka stepper **32**.

### 1.3 Jarak, bentuk
- Padding layar 20; padding atas 20. Jarak antar kartu 10-12; antar grup 16-20.
- Radius: kartu **20**; tombol/input/status band/banner **14**; chip **8**; strip blok **3**; sheet atas **28**; dialog **28**; switch/lingkaran penuh.
- **Tanpa bayangan.** Kedalaman = lapisan warna + border 1px `outline`.
- Ikon: stroke 2px, ujung bulat, 24px (boleh Material Symbols Rounded weight 400-500 sebagai setara). Ikon bernilai warna teks pasangannya.

## 2. Komponen (spesifikasi persis)

| Komponen | Spesifikasi |
|---|---|
| **Kartu** `.c` | surface, border 1px outline, radius 20, padding 14x16 |
| **Tombol utama** `.b.p` | tinggi 56, radius 14, isi accent, teks onAccent, penuh lebar; S04 MULAI tinggi **64**, 22px |
| **Tombol sekunder** `.b.q` | transparan, border 1.5px outlineStrong, teks textPrimary |
| **Tombol destruktif** `.b.d` | transparan, teks fail (varian zona bahaya: border 1.5px #372222) |
| **Teks-tombol** | Barlow 18-20px bold; aksen (`Lanjutkan`, `Simpan`, `Lewati`) atau textSecondary (`Batal`) atau fail (`Hapus`, `Stop`) |
| **Chip hasil** `.ch` | tinggi 26, radius 8, container status + teks status KAPITAL (LULUS `ok`, GAGAL `fl`, BORDERLINE `wr`, TANPA TARGET `nt`) |
| **Status band** | tinggi 44, radius 14, container status; kiri teks KAPITAL Barlow 20 letter-spacing .1em, kanan selisih Barlow 20 |
| **Strip segmen** `.st` | blok tinggi 8 (S04: **14**), jarak 2, radius 3, lebar `flex` proporsional, warna tipe segmen; Result strip = warna status |
| **Progress bar segmen** | tinggi 10, radius 5, trek #2A3139, isi warna tipe |
| **Kontrol segmen** `.sg` | tinggi 40, radius 14, latar surfaceRaised, padding 3; pilihan: Barlow 14 bold KAPITAL, radius 11; **aktif: isi accent, teks onAccent** |
| **Field** `.cc` | tinggi 48, border 1.5px outlineStrong, radius 14, latar surfaceRaised, padding 14 |
| **Switch** `.sw` | 52x32, radius 16; aktif: trek accent + bulatan 24 onAccent di kanan; mati: trek outlineStrong + bulatan textSecondary di kiri |
| **Ikon-tombol** `.ib` | 48x48, bulat, latar surfaceRaised |
| **Pilihan tipe** `.tp` | tinggi 40, radius 12, border 1.5px outlineStrong, latar surfaceRaised, 13px bold; **terpilih: border accent + latar accentContainer + teks accent** |
| **Bottom bar** `.bar` | tinggi 68 (+inset), surface, garis atas outline; 4 tab ikon 24 + label 11px; **aktif: warna accent + batang 24x3 di tepi atas (radius bawah 3)** |
| **Sheet** `.sh` | surface, radius atas 28, border atas outline, padding 12 20 24, pegangan 36x4 outlineStrong di tengah atas, scrim di belakang |
| **Dialog** `.dg` | surfaceRaised, radius 28, padding 24, margin kiri-kanan 35, tengah vertikal, border outline |
| **Banner** | radius 14, padding 10-14, container (aksen/warn), teks 13px bold + ikon 20 |
| **Stepper** | tombol -/+ 48 bulat (surfaceRaised), nilai Barlow 32 di tengah |
| **Grafik** | batang: lebar 60% slot, sudut atas 4, terpilih accent, lainnya accent@35%, nol = stub 2px #2A3139; garis: 3px accent ujung bulat; titik rekor 6-7px accent; label sumbu 11px textSecondary |

Kontrol "tombol utama" tunggal per layar. Aksi destruktif tidak pernah berisi accent.

## 3. Spesifikasi per Layar
Urutan atas ke bawah. File: `mock/<ID>.html` (S02 = `mock/S02.html`).

**S01 Onboarding & Izin (langkah 2 digambar)**: indikator 4 titik (aktif batang 24x6 accent, lain 6x6 outlineStrong); lingkaran 112 accentContainer + ikon 56 accent; judul 44 Barlow tengah; penjelasan 16px tengah; status izin dalam kartu (ikon + teks, warna pass/fail); tombol utama bawah. Tanpa TopBar.

**S02 Beranda**: judul "LATIHAN" 34; banner Lanjutkan (accentContainer, border accent: label "LANJUTKAN SESI" + `Nama workout · durasi berjalan`, kanan teks-tombol Lanjutkan/Buang); kartu **Lari Bebas** isi accent tinggi 96 radius 20 (judul 34 onAccent, lingkaran 56 onAccent berisi ikon accent); kartu **minggu ini** (label + 3 metrik: Jarak, Sesi, Selisih; selisih aksen); header "WORKOUT SAYA"; kartu template (nama bold 16 + ⋮, strip segmen, chip + "terakhir dipakai" kiri, chip "Mulai" 44 accent kanan); tombol **Buat** (pil radius 18, surfaceRaised, border outlineStrong, kanan bawah di atas bottom bar); bottom bar tab 1 aktif.

**S03 Editor Workout**: TopBar (kembali, judul, "Simpan" accent kanan); field Nama; field Target jarak total; bar ringkasan (strip kiri, teks kanan); kartu segmen (tinggi min 72, **batang kiri 4 warna tipe**, kotak ikon 32 bulat berwarna tipe@20%, nama bold, detail 13, kanan ⓘ dan ⋮); blok **Repeat** = garis kiri 3px accent + label "ULANGI n×" accent, anak menjorok 10; baris bawah menempel (tinggi 80, surface): tiga tombol sekunder + Segmen / + Repeat / Preset.

**S04 Pra-start**: TopBar kecil (kembali, label "PRA-START"); nama workout 44; ringkasan 14; strip tebal 14; **kartu GPS** (tinggi 112, border #19342F): 5 batang sinyal (lebar 9, tinggi 10/16/22/28/34, terisi pass, kosong outline), angka akurasi 56 + "m", label "AKURASI GPS", chip "Siap"; header SEGMEN; kartu daftar segmen (baris 42: kotak ikon 32 berwarna tipe + nama + detail kanan; blok Repeat dengan garis kiri accent); "PENGATURAN CEPAT": kartu dengan baris Voice (ID|EN) dan Hitung mundur (0|3|5|10), kontrol segmen; tombol **MULAI** 64 menempel bawah.

**S05 Layar Lari**: header (kembali, nama workout 13, kanan ikon GPS pass + akurasi 12 + ikon peta); banner (accentContainer, teks accent bold 13); blok segmen (ikon + nama Barlow 22 + "SEGMEN x/y" label kanan; progress bar 10; "Sisa" Barlow 22); **hero pace 150** tengah + "/km"; baris target ("TARGET" label + "≤ 6:30" Barlow 30); **status band** (container status); kartu metrik 2 kolom (tinggi 76: "RATA-RATA SEGMEN" | "BUTUH UNTUK SISA", kolom kanan **kosong bila tidak relevan**); baris total tiga kolom di bawah garis outline (Waktu/Jarak/Pace rata-rata, 24); baris Berikutnya (label + teks 13); kontrol: Lewati (ikon-tombol 48), **Jeda 88 bulat accent** ikon 40 onAccent, **Stop** (ikon-tombol, ikon fail, cincin 3px #372222); di bawah teks-tombol accent "Perpanjang" dan "Daftar segmen". Tidak bisa digulir.

**S06 Simpan Sesi**: label "SESI SIAP DISIMPAN"; jarak 96 + "km"; 3 metrik (Waktu, Pace, Segmen lulus aksen); result strip; banner rekor (accentContainer + ikon piala); field Nama sesi; "SKALA CAPEK (1-10)" 10 lingkaran 28 (terpilih isi accent); field Catatan tinggi 84 + penghitung "0/500"; footer menempel: **Simpan** utama + **Buang sesi** destruktif. Tanpa "Lewati".

**S07 Riwayat**: judul 34 + ikon cari; field pencarian; header bulan (label); kartu sesi (nama bold 16, tanggal/jam 13, baris waktu · pace /km + chip hasil; kanan jarak 34 + "km"); bottom bar tab 2.

**S08 Detail Sesi**: TopBar (kembali; kanan ikon peta, ⋮); nama sesi 34 + ikon edit accent; tanggal/jam 13; jarak 72 + "km"; 3 metrik (Waktu, Pace /km, Hasil aksen); result strip; kartu Catatan (label + "Capek n/10"); **tab teks** Barlow 18 KAPITAL, aktif textPrimary + garis bawah 3px accent, lain textSecondary (SEGMEN, SPLIT KM, GRAFIK, PETA); kartu segmen: nama bold + chip hasil, "Batas ...", "jarak · waktu", tiga kolom TARGET/AKTUAL/SELISIH (angka 24; selisih pass bila negatif, fail bila positif), baris "Km di dalam segmen" accent + chevron.

**S09 Statistik ▸ Mingguan** (kanvas tinggi 1160): judul "STATISTIK"; kontrol segmen MINGGUAN|REKOR; navigator minggu (kembali, "5-11 Okt 2026", maju); grid 2x3 kartu angka (tinggi 84: label + angka 30 + satuan); "JARAK PER HARI" batang (hari terpilih accent penuh, lain accent@35%); "JARAK 12 MINGGU" batang; "PACE PER MINGGU" garis 3px accent + titik terakhir 6; "SESI MINGGU INI" kartu seperti S07; bottom bar tab 3.

**S10 Statistik ▸ Rekor**: judul + kontrol segmen (REKOR aktif); 5 kartu rekor (tinggi 104: label jarak, "tanggal · sesi" 13, pace; kanan waktu 40 + ikon piala accent); kartu belum ada: **border putus-putus, tanpa isi**, waktu "--:--" textTertiary, teks "Belum ada"; header "REKOR TERBARU" + daftar baris 42 dalam satu kartu; catatan 12px "Dihitung dari bagian tercepat dalam sesi."

**S11 Detail Rekor**: TopBar (kembali + jarak); waktu 96; "pace x /km · dari km y" 14; link accent "tanggal · sesi" + chevron; kontrol segmen Waktu|Pace; grafik garis (titik rekor 7 accent, lain 4 textSecondary, garis putus accent 2px); "TOP 10" kartu daftar baris 48 (peringkat Barlow 24, #1 accent, lain textSecondary; waktu bold + sesi 12; kanan pace · tanggal 13).

**S12 Pengaturan** (kanvas 1380): judul 34; tiap kelompok = label KAPITAL + kartu; baris min tinggi 52, pemisah outline; kontrol kanan: kontrol segmen / switch / stepper / chevron. Kelompok: TAMPILAN, VOICE, HITUNG MUNDUR, ALERT, TRACKING, DATA, BANTUAN, PENGEMBANG. *Pengelompokan peta live/peta riwayat di TRACKING = asumsi; sesuaikan dengan app asli.* Bottom bar tab 4.

**S13 Cadangan & Data**: TopBar; kartu 3 metrik (Sesi, Template, Ukuran); kartu "Cadangan terakhir"; banner peringatan (warn); tombol utama "Cadangkan ke file"; sekunder "Pulihkan dari file"; "PENGINGAT CADANGAN" kontrol segmen (Mati|14 hari|30 hari, *asumsi*); **zona bahaya**: kartu border fail, label fail, tombol "Hapus semua data", catatan 'Ketik "HAPUS"'.

**S14 Panduan Baterai**: TopBar; penjelasan 16px; kartu status (warn); chip merek gulir horizontal (tinggi 36, aktif accent, lain neutral); kartu langkah (lingkaran 30 accentContainer berisi nomor accent + teks **[placeholder]**); dua tombol menempel bawah (utama, sekunder).

**S15 Simulator**: TopBar; field Workout; chip skenario (aktif accent); kontrol segmen 1x|10x|60x; tombol utama Jalankan; panel log monospace 12px dalam kartu tinggi 200 (baris peringatan warn, pulih pass, lain textSecondary). *Katalog Komponen tidak digambar.*

## 4. Sheet dan Dialog
Semua digambar di atas latar redup (`scrim`) dengan label layar induknya.

| ID | Bentuk | Isi (urutan) |
|---|---|---|
| D01 Editor Segmen | Sheet tinggi (mulai y=60) | Judul 28 + tutup; Label (field); Tipe (grid `.tp` 3+2 + ⓘ); Batas (kontrol segmen Jarak/Durasi/Tanpa batas + stepper); Target pace (angka 32 + kontrol segmen ≤ Maks | ≥ Min); Margin terlalu cepat (angka 28 + switch "Mati"); Per-km check (switch); tombol Simpan |
| D02 Preset | Sheet | Judul; 4 kartu **hanya nama** (Stable run, Progressive tempo, Interval, Run/walk) + ikon kanan. **Tanpa deskripsi.** |
| D03 Edit Sesi | Dialog | Judul; Nama; Skala capek (10 lingkaran 26); Catatan; switch "Jangan hitung untuk rekor"; Batal / Simpan |
| D04 Lanjutkan Sesi | **Banner di S02**, bukan dialog | Nama workout + durasi berjalan; Lanjutkan / Buang |
| D05 Konfirmasi Stop | Dialog | "HENTIKAN SESI?"; ringkas jarak · waktu; teks-tombol "Tetap lari" (accent) dan "Stop" (fail) |
| D06 Konfirmasi Skip | **Dari plan, belum ada di app** | Segmen dilewati vs berikutnya; catatan DILEWATI; Batal / Lewati. Abaikan bila app tetap langsung melewati |
| D07 Perpanjang | Dialog | Judul; baris pilihan `.tp`: +100 m, +500 m (jarak); +30 dtk, +1 mnt (durasi) |
| D08 Kalibrasi Jarak | Sheet | Jarak terukur (36); Jarak sebenarnya (stepper); kartu Faktor (0,95-1,05) angka aksen; tombol Terapkan |
| D09 Hapus | Dialog | "HAPUS SESI?" + nama/tanggal; Batal / Hapus (fail) |
| D09b Hapus semua data | Dialog | "HAPUS SEMUA DATA?"; field border fail berisi `HAPUS`; Batal / Hapus semua (fail) |
| D10 Daftar Segmen | Sheet (mulai y=200) | Baris tinggi min 56: bulatan status 24 (selesai: ikon centang warna status; berjalan: cincin 2px accent + baris accentContainer; belum: cincin kosong), nama bold + detail 12, kanan hasil/"sisa x" |
| D11 Info Segmen | Sheet (mulai y=90) | Lingkaran 56 (tipe @20%) + judul 30; bagian: label KAPITAL accent + teks 14 (KEGUNAAN, KAPAN DIPAKAI, CONTOH PENGATURAN, PERILAKU DI APLIKASI, TIPS); tombol "Mengerti". Hanya topik Lari digambar; topik lain = kerangka sama |

## 5. Perbedaan terhadap `DESIGN.md` lama
| Hal | Lama | Re-skin |
|---|---|---|
| Font | Inter | **Barlow Condensed** (angka, judul, label, tombol) + **Manrope** (teks) |
| Pace hero | 96 | **150** (autosize turun) |
| Label | 12 Inter | 12 Barlow 700, KAPITAL, spacing .14em |
| Tombol | Inter titleS | Barlow 800 KAPITAL spacing .12em |
| Judul halaman | 28 Inter | 34 Barlow 800 KAPITAL |
| Kontrol pilihan | Chip/segmented generik | Kontrol segmen satu pola (aktif = isi accent) |
| Status | Badge | Status band 44 dengan selisih di kanan |
| Strip | 8 | 8 (S04: 14), tipe berwarna |
| Warna | Token sama | Tidak berubah |

## 6. Catatan implementasi (Compose)
- Token ke `RcColors`/`RcTypography`/`RcShapes` yang sudah ada; ganti keluarga font dan ukuran saja, jangan membuat komponen baru bila yang lama bisa diubah.
- Angka tabular: `fontFeatureSettings = "tnum"`.
- Satu komponen untuk satu fungsi (kontrol segmen, switch, kartu, chip, status band, strip). Layar tidak boleh memakai widget Material mentah.
- Uji kontras otomatis tetap berlaku untuk pasangan warna baru.

## 7. Definition of Done per layar
1. Dibandingkan dengan `mock/<ID>.html` (susunan, ukuran, warna, font) pada lebar 390 dan 360.
2. Isi/teks layar **identik** dengan layar existing (hanya tampilan yang berubah).
3. State kosong/loading/error tetap ada dan memakai gaya yang sama.
4. Screenshot sebelum/sesudah, dan daftar selisih terhadap mock di `docs/PROGRESS.md`.
5. Tema terang diturunkan dari token terang `DESIGN.md` dan dicek terpisah.
