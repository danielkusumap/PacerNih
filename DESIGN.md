# DESIGN.md: Panduan Visual dan UX RunCoach

Dokumen pendamping `RUNCOACH_PLAN.md`. Plan menentukan **data apa yang tampil di tiap halaman** (ID halaman S01-S15, sheet D01-D11). Dokumen ini menentukan **bagaimana tampilannya**. Dokumen ini **tidak mengubah** isi data, alur, atau rumus di plan. Bila ada beda soal data, **plan yang menang**. Bila soal tampilan, **dokumen ini yang menang**.

Tanpa gambar atau mockup: semuanya dijelaskan dengan kata dan angka. Satuan `dp` untuk ukuran, `sp` untuk teks. Layar acuan: **360 x 800 dp** (potret). Wajib dicek juga di 411 x 914 dp dan 320 x 640 dp, dengan font scale 1.0 dan 1.3.

## Daftar Isi
1. Cara memakai dokumen ini
2. Arah visual dan prinsip
3. Token warna
4. Tipografi
5. Jarak, bentuk, elevasi, ikon, gerak
6. Komponen reusable
   - 6.1 Gaya grafik
   - 6.2 Spesifikasi grafik (CH1–CH6)
7. Aturan layout global
8. Spesifikasi per halaman (S01-S15)
9. Spesifikasi sheet dan dialog (D01-D11)
10. Microcopy
11. Aksesibilitas dan kualitas
12. Task UI, hubungan dengan task plan, dan Definition of Done

---

## 1. Cara Memakai Dokumen Ini

1. Kerjakan **U0 dan U1** (bagian 12) **sebelum** membangun halaman pertama (P1.1). Jangan membuat layar memakai widget Material mentah.
2. Setiap task plan yang membangun halaman (tabel pemetaan di 12.2) wajib membaca bagian halaman terkait di dokumen ini.
3. Tidak boleh menambah elemen visual yang tidak tertulis di sini tanpa mencatatnya di `docs/DECISIONS.md`.
4. Semua nilai warna, ukuran teks, jarak, dan radius **hanya** boleh berasal dari token (bagian 3-5). Dilarang hex, `dp`, atau `sp` literal di kode layar.
5. Jika ada keraguan estetika: pilih yang **lebih sederhana, lebih besar, dan lebih kontras**.

---

## 2. Arah Visual dan Prinsip

**Karakter:** atletik, tegas, tenang. Dark-first, kontras tinggi, **satu aksen lime**, angka sebagai pahlawan visual. Bukan dekorasi, bukan gradien ramai, bukan ilustrasi.

**Prinsip:**
1. **Angka adalah bintang.** Pace, waktu, dan jarak selalu paling besar dan paling kontras di layar. Label di sekitarnya kecil dan tenang.
2. **Satu aksen, satu makna.** Lime = aksi utama dan identitas. Lime **tidak pernah** dipakai untuk status. Warna status (hijau, merah, amber, biru) **tidak pernah** dipakai untuk tombol.
3. **Status tidak hanya warna.** Selalu ikon + teks + warna.
4. **Tidak ada layout loncat.** Elemen yang bisa muncul/hilang (required pace, badge status, banner) mendapat slot tetap atau animasi non-geser.
5. **Layar lari tidak boleh di-scroll**, tidak boleh ada teks di bawah 14sp kecuali label, dan semua kontrol utama terjangkau satu ibu jari.
6. **Kepadatan sesuai konteks.** Layar lari: besar dan lapang. Riwayat, statistik, editor: lebih padat tetapi tetap rapi dan berkelompok.
7. **Anti-default.** Tidak boleh terlihat seperti template Material: tanpa ungu bawaan, tanpa dynamic color, tanpa tombol pil standar, tanpa AppBar tengah, tanpa FAB bulat default.
8. **Konsisten.** Satu komponen untuk satu fungsi. Jangan membuat varian baru per halaman.

---

## 3. Token Warna

Dynamic color **dimatikan**. Buat `RcColors` (data class) dengan dua instans (`dark`, `light`) lewat `CompositionLocal`. Seluruh komponen membaca `RcColors`; `MaterialTheme.colorScheme` hanya dipetakan dari token agar komponen Material yang terpaksa dipakai (mis. TextField dasar) tidak berwarna bawaan.

### 3.1 Token inti

| Token | Dark | Light | Dipakai untuk |
|---|---|---|---|
| `bg` | #0B0D0F | #F4F5F1 | Latar layar |
| `surface` | #14181C | #FFFFFF | Kartu, sheet, bottom bar |
| `surfaceRaised` | #1C2127 | #FFFFFF | Kartu di atas kartu, dialog, input |
| `outline` | #2A3139 | #E1E4DC | Garis pemisah, border kartu (light: border 1dp) |
| `outlineStrong` | #3A444E | #C5CABF | Border input, tombol sekunder |
| `textPrimary` | #F2F5F7 | #0E1114 | Teks dan angka utama |
| `textSecondary` | #A3ADB7 | #4A545D | Label, keterangan |
| `textTertiary` | #7C8791 | #6A737C | Keterangan sangat kecil, placeholder |
| `accent` | #C6F432 | #B7E41C | Isi tombol utama, indikator aktif, aksen grafik |
| `onAccent` | #0B0D0F | #0E1114 | Teks/ikon di atas `accent` |
| `accentText` | #C6F432 | #4F6F00 | Teks/ikon aksen di atas `bg`/`surface` (di light harus gelap agar terbaca) |
| `accentContainer` | #2D371F | #F1FAD2 | Latar lembut berisi aksen (banner Lanjutkan, lencana rekor) |
| `scrim` | #000000 @ 60% | #000000 @ 40% | Latar belakang sheet/dialog |

### 3.2 Token status (hanya untuk status, tidak untuk tombol)

| Token | Dark teks/ikon | Dark container | Light teks/ikon | Light container | Arti |
|---|---|---|---|---|---|
| `pass` | #34D399 | #19342F | #0B7A55 | #DDF4EA | STABIL, LULUS, GPS siap |
| `fail` | #FF5D5D | #372222 | #C42B2B | #FBE2E2 | TERLALU LAMBAT, GAGAL, error, hapus |
| `warn` | #FFB547 | #373022 | #9A5B00 | #FFEFD0 | BORDERLINE, GPS lemah, peringatan |
| `fast` | #5BB5FF | #1F303E | #135FA8 | #DCEBFA | TERLALU CEPAT |
| `neutral` | `textSecondary` | #232A31 | `textSecondary` | #ECEEE8 | TANPA TARGET, DATA KURANG, DILEWATI |

### 3.3 Token tipe segmen (hanya untuk strip dan penanda; selalu disertai ikon + label)

| Token | Dark | Light | Tipe |
|---|---|---|---|
| `kindRun` | #C6F432 | #A4D10F | Lari, Lari Bebas |
| `kindWalk` | #8FA3B8 | #6F8499 | Jalan |
| `kindRecovery` | #6FB7C4 | #3E93A3 | Pemulihan |
| `kindWarmCool` | #B8A6E8 | #8C77D4 | Pemanasan, Pendinginan |

### 3.4 Aturan pemakaian warna
- Teks biasa di atas `bg`/`surface`/`surfaceRaised` memakai `textPrimary`/`textSecondary`. Hanya teks aksen yang memakai `accentText`.
- Teks status di atas container status memakai warna solid status yang sesuai.
- **Kontras wajib**: teks normal >= 4.5:1, teks besar (>= 24sp) dan ikon >= 3:1. Buat **unit test kontras** (U0) yang menghitung rasio semua pasangan teks/latar di kedua tema; bila ada yang gagal, sesuaikan token dan catat di DECISIONS.
- Pace hero **tidak diwarnai**; selalu `textPrimary`. Warna status ada di badge status.
- Jangan memakai lebih dari satu warna status dalam satu komponen.

---

## 4. Tipografi

**Satu keluarga font: Inter** (gratis, lisensi OFL), dibundel di `res/font` (tanpa unduh runtime). Gaya angka **tabular** (`fontFeatureSettings = "tnum"`) untuk semua angka yang berubah (pace, waktu, jarak, selisih) supaya tidak bergeser. Jika Inter tidak tersedia, catat di DECISIONS dan pakai font sistem dengan tnum.

| Token | Ukuran / berat | Tinggi baris | Pemakaian |
|---|---|---|---|
| `displayHero` | 96sp / ExtraBold (800), letter-spacing -2% | 100sp | Pace live di S05 |
| `displayL` | 56sp / Bold | 60sp | Jarak utama di S06, rekor di S11 |
| `numberXL` | 40sp / Bold | 44sp | Angka besar kartu (jarak di S08, rekor di S10) |
| `numberL` | 32sp / Bold | 36sp | Metrik segmen S05, hitung mundur banner |
| `numberM` | 24sp / SemiBold | 28sp | Angka di baris statistik |
| `titleL` | 28sp / Bold | 34sp | Judul halaman tab (S02, S07, S09, S10, S12) |
| `titleM` | 20sp / SemiBold | 26sp | Judul kartu, dialog, sheet |
| `titleS` | 16sp / SemiBold | 22sp | Nama item di daftar |
| `body` | 16sp / Regular | 24sp | Teks isi |
| `bodySmall` | 14sp / Regular | 20sp | Keterangan |
| `label` | 12sp / SemiBold, huruf kapital, letter-spacing +6% | 16sp | Label metrik, header bagian |
| `caption` | 12sp / Regular | 16sp | Sumbu grafik, catatan kecil |

Aturan:
- Tidak ada teks di bawah **12sp**. Selain `label` dan `caption`, tidak ada teks di bawah 14sp.
- Satuan di samping angka (`/km`, `km`, `m`) memakai `titleS` atau `bodySmall`, `textSecondary`, rata baseline dengan angka.
- Pace hero **mengecil otomatis** (autosize) ke minimal 72sp bila lebar tidak cukup (mis. `10:05` di layar sempit atau font scale 1.3). Tidak boleh terpotong atau membungkus.
- Judul tidak boleh lebih dari 2 baris; potong dengan elipsis.
- Teks sejajar kiri. Hanya angka hero, hitung mundur, dan state kosong yang boleh di tengah.

---

## 5. Jarak, Bentuk, Elevasi, Ikon, Gerak

### 5.1 Jarak (grid 4dp)
Token: `s1=4`, `s2=8`, `s3=12`, `s4=16`, `s5=24`, `s6=32`, `s7=48`.
- Padding horizontal layar: **20dp** (16dp bila lebar < 360dp).
- Jarak antar kartu: 12dp. Jarak antar grup: 24dp. Padding dalam kartu: 16dp.
- Ukuran sentuh minimal: **48 x 48dp** untuk semua yang bisa diketuk, termasuk ikon kecil (area sentuh diperluas, tampilan tetap).

### 5.2 Bentuk
`radiusS=8` (chip kecil, input kecil), `radiusM=14` (tombol, input, status band), `radiusL=20` (kartu), `radiusXL=28` (sudut atas bottom sheet, dialog). Dilarang bentuk pil penuh untuk tombol; pil penuh hanya untuk chip dan switch.

### 5.3 Elevasi
Tanpa bayangan. Kedalaman dibuat lewat lapisan warna (`bg` < `surface` < `surfaceRaised`) dan border `outline` (di light, kartu wajib border 1dp). Satu-satunya "bayangan" adalah `scrim` di belakang sheet/dialog.

### 5.4 Ikon
**Material Symbols Rounded**, weight 400, grade 0, ukuran 24dp (28dp di kontrol S05, 20dp di dalam chip). Diambil sebagai file **VectorDrawable** (Apache 2.0) ke `res/drawable`, **tanpa menambah dependency ikon**. Warna ikon = warna teks pasangannya.

Daftar ikon: `home`/`play_arrow` (Latihan), `history` (Riwayat), `bar_chart` (Statistik), `settings` (Pengaturan), `pause`, `play_arrow`, `skip_next`, `stop`, `add`, `edit`, `delete`, `info` (ⓘ), `check_circle`, `cancel`, `error`, `warning`, `trending_up`, `trending_down`, `emoji_events` (rekor), `directions_run` (Lari), `directions_walk` (Jalan), `self_improvement` (Pemulihan), `whatshot` (Pemanasan), `ac_unit` (Pendinginan), `repeat`, `gps_fixed`/`gps_not_fixed`/`gps_off`, `battery_alert`, `map`, `ios_share`, `more_vert`, `drag_indicator`, `chevron_right`, `close`, `arrow_back`, `schedule`, `straighten`, `notes`, `mood`, `list`, `notifications`, `check`, `search`, `chevron_left`.

### 5.5 Gerak
- Durasi standar **180ms** `easeOut`; sheet 240ms. Tanpa animasi pantul.
- Di S05: angka pace **tidak dianimasikan** (tanpa count-up); badge status crossfade 150ms; hasil segmen muncul fade+geser 8dp. Hanya hitung mundur yang berdenyut: skala 1.12 ke 1.0 dalam 180ms tiap detik.
- Hormati pengaturan sistem "hapus animasi": ganti semua gerak menjadi perubahan instan.
- Shimmer loading maksimal satu siklus per 1.4 dtk, berhenti saat "hapus animasi".

---

## 6. Komponen Reusable (`ui/components`)

Semua layar hanya boleh memakai komponen ini. Setiap komponen punya `@Preview` untuk light dan dark dan untuk tiap state.

| Komponen | Spesifikasi |
|---|---|
| **RcButton** | Tinggi 56dp (kecil: 44dp), radius 14. Varian: `primary` (isi `accent`, teks `onAccent`, titleS bold), `secondary` (transparan, border `outlineStrong` 1.5dp, teks `textPrimary`), `tertiary` (teks `accentText`, tanpa border), `destructive` (teks `fail`, tanpa isi; konfirmasi akhir isi `fail` + teks putih). State: pressed (isi 12% lebih gelap atau overlay 10%), disabled (opacity 38%), loading (spinner 20dp). Ikon opsional di kiri 20dp. Lebar penuh pada aksi utama di bawah layar. |
| **RcIconButton** | Area 48dp; ikon 24dp; varian `plain` (ikon `textSecondary`), `filled` (lingkaran 56dp isi `surfaceRaised`), `accent` (lingkaran isi `accent`, ikon `onAccent`). |
| **RcCard** | Isi `surface`, radius 20, padding 16, border `outline` 1dp (selalu di light; di dark opsional). Varian `interactive` memberi efek tekan (overlay 6%). |
| **StatusBadge** (status live) | Tinggi 44dp, radius 14, container status + ikon 24dp + teks `titleS` bold huruf kapital + selisih angka tabular di kanan. Dipakai di S05 (STABIL, TERLALU LAMBAT, TERLALU CEPAT). |
| **ResultChip** (hasil segmen/km) | Tinggi 28dp, radius 8, container status, ikon 16dp + teks `label`. Varian: LULUS (`pass`, check_circle), GAGAL (`fail`, cancel), BORDERLINE (`warn`, warning), TANPA TARGET (`neutral`, tanpa ikon), DATA KURANG (`neutral` dengan border putus-putus), DILEWATI (`neutral`, skip_next). |
| **MetricBlock** | Label (`label`, `textSecondary`) di atas, nilai (`numberM`/`numberL`, `textPrimary`) di bawah, satuan kecil di samping nilai. Rata kiri. Nilai kosong ditulis `--:--` atau `-`. |
| **HeroPace** | `displayHero` rata tengah, satuan `/km` (`titleS`, `textSecondary`) di bawah angka. Autosize (bagian 4). Saat pace tidak ada: `--:--` dalam `textTertiary`. |
| **SegmentProgressBar** | Tinggi 10dp, radius 5, trek `outline`, isi sesuai warna tipe segmen (token `kind*`), tanpa animasi panjang (transisi lebar 180ms). Segmen `Open`: tidak ditampilkan. |
| **SegmentStrip** | Deretan blok horizontal tipis (tinggi 8dp, radius 3, jarak 2dp) mewakili segmen berurutan, warna token `kind*`. Lebar blok proporsional terhadap estimasi (F24 di plan); bila tidak bisa diestimasi, lebar sama. Lebar minimum 6dp. Dipakai di S02, S03 (ringkasan), S04, D10. |
| **ResultStrip** | Sama dengan SegmentStrip, tetapi warna blok = warna status hasil segmen (pass/fail/warn/neutral). Dipakai di S06 dan S08. |
| **SectionHeader** | `label` (huruf kapital, `textSecondary`) dengan jarak atas 24dp, bawah 8dp; opsional aksi teks di kanan (`tertiary`). |
| **ListRow** | Tinggi minimal 56dp; ikon/teks utama kiri, nilai/kontrol kanan; pemisah `outline` 1dp inset 20dp. |
| **TopBar** | Tinggi 56dp; judul `titleL` rata kiri pada halaman tab (tinggi 64dp), `titleM` pada halaman dalam; ikon aksi kanan 48dp; panah kembali kiri. Tidak ada AppBar tengah, tidak ada warna latar (menyatu dengan `bg`). |
| **BottomBar** | Tinggi 64dp + inset sistem, isi `surface`, garis atas `outline`. 4 tab: ikon 24dp + label `label` huruf kecil-sedang 12sp. Aktif: ikon dan label `accentText`, plus **batang aksen 3dp x 24dp** di atas ikon. Nonaktif: `textSecondary`. Tanpa indikator pil Material. |
| **RcSheet** | Bottom sheet: isi `surface`, sudut atas radius 28, pegangan 4x36dp `outlineStrong` di tengah atas, padding 20, `scrim` di belakang. Tombol aksi di bawah, terikat (sticky) di atas inset. |
| **RcDialog** | Isi `surfaceRaised`, radius 28, lebar maks 320dp, padding 24, judul `titleM`, isi `body`, tombol rata kanan (`tertiary` dan `primary`/`destructive`). |
| **RcTextField** | Tinggi 56dp (multiline: min 96dp), isi `surfaceRaised`, border `outlineStrong` 1.5dp, radius 14; fokus: border `accent` 2dp; error: border `fail` + teks error 14sp di bawah dengan ikon. Label mengambang `label`. |
| **NumberStepper** | Tombol -/+ 48dp kiri-kanan, nilai `numberM` di tengah, langkah tertulis (mis. 5 dtk, 100 m); tekan-tahan mengulang cepat. Dipakai untuk pace, jarak, durasi, margin. |
| **SegmentedControl** | Tinggi 44dp, radius 14, trek `surfaceRaised`, pilihan aktif isi `accent` teks `onAccent`, nonaktif teks `textSecondary`. 2-4 pilihan. |
| **RcSwitchRow** | ListRow dengan switch: trek 52 x 32dp, aktif isi `accent` + bulatan `onAccent`, nonaktif trek `outlineStrong` + bulatan `textSecondary`. |
| **InfoIcon (ⓘ)** | Ikon `info` 20dp `textSecondary` dengan area sentuh 48dp, deskripsi aksesibilitas "Info {topik}". Membuka D11. |
| **Banner** | Tinggi min 48dp, radius 14, container status/aksen + ikon 24dp + teks `bodySmall` bold + aksi teks opsional. Varian: info (`fast`), peringatan (`warn`), sukses (`pass`), aksen (`accentContainer` + teks `accentText`). Tidak menggeser layout (menempel di atas isi sebagai overlay di S05). |
| **EmptyState** | Di tengah area: lingkaran 72dp isi container `neutral` berisi ikon 32dp `textSecondary`, judul `titleM`, satu kalimat `body` `textSecondary` (maks 2 baris), tombol `primary`/`secondary` opsional. |
| **ErrorState** | Seperti EmptyState dengan ikon `error` berwarna `fail`, pesan spesifik, tombol "Coba lagi". |
| **Skeleton** | Blok `surfaceRaised` berradius sama dengan elemen asli, shimmer lembut, tanpa teks palsu. Dipakai saat loading daftar. |
| **ChartBar / ChartLine** | Lihat 6.1. |
| **ProgressRing (tahan-untuk-konfirmasi)** | Cincin 3dp mengelilingi tombol Stop yang terisi `fail` searah jarum jam selama 2 dtk tekan-tahan; lepas sebelum penuh = batal. |

### 6.1 Gaya grafik (`ChartBar`, `ChartLine`)
- Latar transparan; tanpa kotak/border grafik. Garis bantu horizontal 1dp `outline`, maks 4 garis, tanpa garis vertikal (kecuali penanda batas segmen).
- Label sumbu: `caption`, `textTertiary`. Sumbu Y di kiri, hanya 3-4 label.
- **Batang**: lebar batang 60% slot, sudut atas radius 4, isi `accent` pada nilai terpilih/terbaru dan `accent` @ 35% pada lainnya; nilai nol digambar garis tipis 2dp `outline`.
- **Garis**: tebal 3dp `accent`, ujung bulat; titik hanya pada titik yang disorot (radius 5dp, isi `accent`, cincin `bg` 2dp). Area di bawah garis: gradien `accent` 18% ke 0%. Nilai null = garis putus (tidak disambung).
- **Garis target** (tangga): 2dp putus-putus `textSecondary`. **Batas segmen**: garis vertikal 1dp putus-putus `outlineStrong`.
- **Tooltip**: kartu `surfaceRaised` radius 12, padding 8x12, angka `numberM`, keterangan `caption`; muncul saat sentuh, berhenti mengikuti jari saat dilepas dengan memudar 150ms.
- Satu grafik = satu tinggi tetap: 160dp (kecil), 200dp (utama).

---


### 6.2 Spesifikasi grafik (CH1–CH6)

Bagian ini memperinci implementasi grafik untuk S08, S09, dan S11. Jika aturan khusus CH1–CH6 berbeda dari gaya umum di 6.1, **aturan khusus grafik ini yang berlaku**. `SegmentStrip`, `ResultStrip`, dan progress bar bukan grafik; ketiganya tetap mengikuti spesifikasi komponen masing-masing.

#### Daftar grafik (6 buah)

| ID | Halaman | Jenis | Isi |
|---|---|---|---|
| CH1 | S08 tab Grafik | Garis | Pace sepanjang jarak, garis target tangga, batas segmen |
| CH2 | S08 tab Grafik | Batang | Pace per km |
| CH3 | S09 | Batang | Jarak per hari, Senin–Minggu |
| CH4 | S09 | Batang | Jarak per minggu, 12 minggu |
| CH5 | S09 | Garis | Pace rata-rata per minggu, 12 minggu |
| CH6 | S11 | Garis + titik | Best effort per sesi dan garis rekor |

#### Aturan umum (semua grafik)

- **Tinggi:** CH1 dan CH6 200dp. Grafik lainnya 160dp. Padding plot: kiri 36dp bila ada label Y (0 bila tidak ada), bawah 24dp, atas 8dp, kanan 8dp.
- **Legenda** hanya jika ada lebih dari satu seri: baris `caption` di atas grafik, berisi contoh bentuk garis atau titik beserta namanya.
- **Animasi masuk** sekali saat pertama tampil: garis terungkap dari kiri ke kanan 300ms, batang tumbuh dari dasar 250ms. Tidak ada animasi saat data berubah. Animasi mati total bila “hapus animasi” aktif.
- **Rendering:** hanya Compose Canvas, path di-cache, tidak rekomposisi per frame.
- **Format angka:** pace `m:ss`, jarak km dengan koma desimal, waktu `mm:ss` atau `h:mm:ss`.
- **Fungsi pengolah data murni (bisa dites):** `bucketByDistance`, `niceTicks`, `downsample`, dan `invertPaceScale`. Semuanya mendapat unit test.

#### Skala sumbu

- **Sumbu pace (garis):** dibalik, lebih cepat di atas, dengan label “lebih cepat” (`caption`) di ujung atas. Rentang: `min = floor(pace tercepat / 10) × 10 − 10 dtk`, `max = ceil(pace terlambat / 10) × 10 + 10 dtk`, dengan lebar minimal 60 dtk. Tick 3–4 buah pada kelipatan 15 atau 30 dtk (pilih langkah terkecil dari `{10, 15, 30, 60}` dtk yang menghasilkan 3–4 tick).
- **Sumbu jarak (X):** langkah dari `{0,5; 1; 2; 5; 10}` km, maksimal 6 label. Di bawah 3 km pakai 0,5 km.
- **Sumbu jarak (Y, batang):** maks = nilai terbesar dibulatkan ke atas ke langkah rapi, minimal 5 km. 3 tick, termasuk 0.
- **Nilai di luar jangkauan:** pace lebih lambat dari 20:00 atau lebih cepat dari 2:30 dipotong di batas dan tidak mengubah skala.

#### Aturan per grafik

**CH1 Pace sepanjang jarak (S08)**

- **Data:** bucket 100 m. `pace bucket = waktu berjalan bucket / 0,1 km`. Bucket yang berisi jeda atau sampel hilang menjadi celah (garis putus). Maksimal 200 titik; bila lebih, gabungkan bucket yang berdampingan.
- **Garis pace:** 3dp `accent` dengan gradien area 18%.
- **Garis target:** tangga, 2dp putus-putus `textSecondary`, satu anak tangga per segmen bertarget. Segmen tanpa target tidak digambar.
- **Batas segmen:** garis vertikal putus-putus 1dp `outlineStrong`, plus **strip tipis 6dp di tepi atas plot** berwarna `kind*` sepanjang tiap segmen (pengganti latar berwarna supaya grafik tetap bersih).
- **Legenda:** “Pace” (garis solid) dan “Target” (garis putus).
- **Sentuh dan geser:** garis indikator vertikal mengikuti jari. Tooltip berisi jarak, pace, dan nama segmen di titik itu.

**CH2 Pace per km (S08)**

- Ini **mengganti** aturan “sumbu pace dibalik” pada grafik batang S08: **tinggi batang sebanding kecepatan (`1/pace`), jadi lebih tinggi = lebih cepat**. Sumbu Y disembunyikan.
- Setiap batang diberi label pace `caption` di atasnya.
- **Warna batang:** `accent` secara default. Km berstatus GAGAL memakai `fail`, BORDERLINE memakai `warn`. Hanya km yang punya target yang diberi warna status. Km tercepat diberi penanda kecil di atas label.
- **Km parsial** (misalnya 0,10 km): lebar batang proporsional terhadap jarak dengan minimum 12dp, isi `accent` @ 35% dan garis tepi putus-putus, label “0,10”.
- **Garis target** horizontal putus-putus bila ada. Bila target berbeda antar km (tempo), gambar sebagai tangga seperti CH1.
- **Sumbu X:** nomor km.

**CH3 Jarak per hari (S09)**

- Label X: Sen, Sel, Rab, Kam, Jum, Sab, Min (`caption`). Hari ini disorot dengan isi `accent` penuh, hari lain `accent` @ 35%. Hari mendatang dibuat redup (isi `outline`, tanpa nilai).
- Hari tanpa sesi: stub 2dp `outline`, tanpa label nilai.
- Nilai ditulis di atas batang bila lebih dari 0 (`caption`).

**CH4 Jarak 12 minggu (S09)**

- 12 batang. Minggu terpilih `accent` penuh dengan label nilai; lainnya `accent` @ 35%.
- Label X tiap minggu ke-3 dalam format “12/10” (tanggal awal minggu).
- Ketuk batang memilih minggu (area sentuh minimal 48dp lebar meski batang lebih sempit).
- Minggu kosong: stub 2dp.

**CH5 Pace rata-rata mingguan (S09)**

- Sumbu pace dibalik. Titik radius 4dp pada setiap minggu yang punya data; titik minggu terpilih 6dp dengan cincin.
- Minggu kosong (nilai `null`): garis **tidak disambung**.
- Satu titik saja: tampilkan titik dan nilainya tanpa garis.

**CH6 Rekor per jarak (S11)**

- **Sumbu X:** sesi berurutan berjarak sama (bukan skala waktu). Label hanya sesi pertama, tengah, dan terakhir dalam format tanggal.
- **Sumbu Y:** waktu atau pace sesuai pilihan `SegmentedControl`, dibalik (lebih cepat di atas).
- Dua seri: **titik** “Per sesi” (radius 4dp, isi `textSecondary`) dan **titik PR** (radius 6dp, isi `accent`, cincin `bg` 2dp), serta **garis rekor** berbentuk tangga 2dp `accent` yang menghubungkan titik PR.
- **Legenda:** “Per sesi”, “Rekor”.
- **Tooltip:** tanggal, nama sesi, waktu, pace. Ketuk tooltip menuju S08.

#### Interaksi umum

- Tooltip muncul di atas titik atau batang yang disentuh. Posisinya dibalik ke bawah atau ke samping bila hampir menyentuh tepi; tooltip tidak pernah terpotong.
- Setelah jari diangkat, tooltip bertahan 1,5 dtk lalu memudar 150ms. Pada batang, ketukan memilih dan sorotan menetap.
- Gestur geser horizontal pada grafik tidak boleh bentrok dengan gulir halaman: gulir vertikal tetap menggulir halaman, geser horizontal di dalam area plot menggeser indikator.

#### Kondisi khusus

- **Loading:** persegi Skeleton setinggi grafik dengan radius yang sama.
- **Kosong:** `caption` rata tengah “Belum ada data” di area plot, sumbu tetap digambar tipis. Khusus S09 minggu kosong: sumbu dan stub nol tetap tampil.
- **Data sangat sedikit:** kurang dari 2 titik pada grafik garis: tampilkan titik tunggal beserta nilainya dan teks `caption` “Butuh lebih banyak data untuk grafik”. Khusus S11: teks “Butuh minimal 2 sesi untuk grafik”.
- **Error:** `caption` pesan singkat + tombol `tertiary` “Coba lagi”.

#### Aksesibilitas

- Setiap grafik punya `contentDescription` berupa ringkasan, misalnya “Grafik pace per km. Km 1 6:40, km 2 6:40, km 3 6:43, sisa 0,10 km 7:00. Tercepat km 1.”
- Sorotan tidak hanya mengandalkan warna: batang terpilih punya label nilai di atasnya, titik PR berbentuk lebih besar dengan cincin.
- Area sentuh batang dan titik minimal 48dp.
- Kontras garis dan label mengikuti aturan bagian 3.4.

#### Aturan tambahan implementasi (acceptance criteria)

- Fungsi transformasi data punya unit test dengan nilai persis: bucket 100 m dari track sintetis, `niceTicks` untuk rentang pace 5:12–7:48 menghasilkan tick yang rapi, pembalikan skala pace, serta kasus 0, 1, dan 2 titik.
- Preview Compose untuk setiap grafik mencakup state: normal, kosong, satu titik, data dengan celah, light, dan dark.
- Tidak ada library chart. Semua grafik digambar di Canvas.

---

## 7. Aturan Layout Global

- Hanya **potret**. Hormati inset sistem (status bar, navigasi gestur, notch).
- Isi halaman tab berada di `LazyColumn` dengan padding bawah = tinggi BottomBar + 16dp.
- Aksi utama halaman (Mulai, Simpan) **menempel di bawah** (sticky), 56dp, di atas inset, dengan latar `bg` + garis atas `outline` bila isi bisa bergulir di baliknya.
- Urutan kepentingan visual di tiap halaman: **(1)** angka/pesan utama, **(2)** aksi utama, **(3)** detail, **(4)** aksi sekunder.
- Daftar panjang memakai pemisah halus atau jarak antar kartu, tidak keduanya.
- Konten kosong tidak boleh meninggalkan layar hitam kosong: selalu EmptyState.
- Tinggi layar: kelas **Compact** (< 680dp), **Standard** (680-820dp), **Tall** (> 820dp). Hanya S05 dan S04 punya aturan khusus per kelas (dijelaskan di halamannya).

---

## 8. Spesifikasi per Halaman

Format tiap halaman: **Tujuan visual**, **Susunan** (atas ke bawah), **Aturan khusus**, **State** (loading / kosong / error). Nama blok mengacu ke plan (mis. blok A-H di S05).

### S01 Onboarding & Izin
**Tujuan visual:** ramah, ringkas, tidak menakutkan. Empat langkah, satu keputusan per layar.
**Susunan:** layar penuh tanpa TopBar. Indikator langkah 4 titik di atas (aktif: batang aksen 24 x 6dp; lain: titik 6dp `outlineStrong`). Di tengah: lingkaran 112dp container `accentContainer` berisi ikon 56dp `accentText` (lokasi: `gps_fixed`, notifikasi: `notifications`, baterai: `battery_alert`, intro: `directions_run`). Judul `titleL` (maks 2 baris), penjelasan `body` `textSecondary` (maks 4 baris). Di bawah: `RcButton primary` penuh ("Berikan izin" / "Lanjut"), di bawahnya `tertiary` ("Lewati", hanya untuk notifikasi dan baterai).
**Aturan khusus:** status izin ditampilkan sebagai baris `ListRow` kecil di bawah penjelasan: ikon `check_circle` `pass` + "Diberikan" atau `cancel` `fail` + "Ditolak". Bila ditolak permanen: Banner peringatan + tombol "Buka pengaturan". Izin lokasi ditolak tidak boleh memblokir seluruh app; tampilkan Banner di S04 nanti.
**State:** tidak ada loading panjang. Error: GPS perangkat mati -> Banner `warn` "GPS perangkat mati" + tombol buka pengaturan.

### S02 Beranda (tab Latihan)
**Tujuan visual:** titik mulai yang jelas; satu tombol besar mengundang untuk berlari.
**Susunan (bergulir):**
1. TopBar: judul "Latihan" (`titleL`), kiri-rata, tanpa ikon.
2. **Banner Lanjutkan sesi** (hanya bila ada snapshot): Banner varian aksen (`accentContainer`, border `accent` 1dp) berisi nama workout, "4,2 km · 28:10", dan dua tombol teks "Lanjutkan" (`tertiary`) dan "Buang" (`destructive`).
3. **Kartu "Lari Bebas"**: kartu penuh lebar, isi `accent`, teks `onAccent`; kiri: judul "Lari Bebas" `titleM` bold + "Tanpa target, ukur pace dan jarak" `bodySmall`; kanan: lingkaran 56dp isi `onAccent` dengan ikon `play_arrow` berwarna `accent`. Tinggi 96dp, radius 20. Ini satu-satunya elemen berlatar aksen penuh di halaman.
4. **Kartu ringkasan minggu ini** (RcCard `interactive`): label "MINGGU INI"; baris 3 MetricBlock (Jarak, Sesi, Selisih vs minggu lalu dengan ikon `trending_up`/`trending_down` berwarna `pass`/`neutral`; "Baru" bila minggu lalu 0). Tap -> S09.
5. SectionHeader "WORKOUT SAYA".
6. **Kartu template** (RcCard `interactive`, satu per template): baris 1: nama `titleS` + menu `more_vert` (Edit, Duplikat, Hapus). Baris 2: SegmentStrip penuh lebar. Baris 3: chip kecil teks `bodySmall` (mis. "9 segmen", "5,6 km", "6:00-8:00") dan di kanan `RcButton primary` kecil 44dp "Mulai". Tampilkan "terakhir dipakai" `caption` `textTertiary` bila ada.
7. **FAB "Buat workout"**: tombol diperpanjang di kanan bawah di atas BottomBar: tinggi 56, radius 18, isi `surfaceRaised`, border `outlineStrong`, ikon `add` + teks "Buat" (`titleS`). Bukan FAB bulat default.
**State:** loading: Skeleton 3 kartu. Kosong (hanya Lari Bebas): di bawah kartu Lari Bebas tampil EmptyState kecil "Belum ada workout" + tombol `secondary` "Buat workout pertama". Error baca data: ErrorState.

### S03 Editor Workout
**Tujuan visual:** terasa seperti menyusun balok. Setiap segmen adalah kartu berwarna tipe; Repeat adalah kurung yang membungkus.
**Susunan:**
1. TopBar: panah kembali, judul "Buat workout"/"Edit workout", aksi kanan `tertiary` "Simpan" (nonaktif 38% bila tidak valid).
2. `RcTextField` nama workout; di bawahnya field "Target jarak total (opsional)".
3. **Bar ringkasan** (menempel di atas daftar saat menggulir, isi `surface`, tinggi 48): SegmentStrip kecil kiri, kanan teks `bodySmall`: "9 segmen · ≥ 5,6 km · ≥ 42 mnt" (awalan ≥ sesuai F24 plan).
4. **Daftar item**:
   - **Kartu segmen**: RcCard tinggi min 72, **batang kiri 4dp** warna `kind*`, ikon tipe 24dp (warna `kind*`), label `titleS`, baris kedua `bodySmall` `textSecondary`: batas dan target ("400 m · ≤ 6:00"). Kanan: InfoIcon (ⓘ), `drag_indicator`, `more_vert` (Duplikat, Hapus). Tap kartu membuka D01.
   - **Blok Repeat**: kontainer tanpa isi, **garis kurung kiri 3dp `accent`** sepanjang tinggi blok, header baris kecil: ikon `repeat` + chip "Ulangi 3x" (atau "Sampai 5 km") + `more_vert`. Anak-anak menjorok 12dp. Tombol kecil "+ Segmen di sini" `tertiary` di akhir blok.
   - **Pesan validasi**: di bawah item bermasalah, baris `bodySmall` `fail` dengan ikon `error` 16dp; kartu item bermasalah mendapat border `fail` 1.5dp.
5. **Baris aksi tambah** (menempel di bawah, tinggi 72, `surface`, garis atas `outline`): tiga tombol `secondary` sejajar: "+ Segmen", "+ Repeat", "Preset".
**Aturan khusus:** seret-untuk-urut lewat `drag_indicator`; saat diseret kartu naik (skala 1.02, border `accent`), tempat kosong ditandai garis putus. Daftar kosong: EmptyState "Belum ada segmen" + tombol "Tambah segmen". Dialog "Perubahan belum disimpan" memakai RcDialog.
**State:** tidak ada loading. Error simpan: Banner `fail` di atas.

### S04 Pra-start
**Tujuan visual:** tenang dan meyakinkan: "semua siap, tekan Mulai".
**Susunan:**
1. TopBar: panah kembali, judul nama workout.
2. SegmentStrip penuh lebar (tinggi 8dp) lalu `bodySmall` ringkas "9 segmen · ≥ 5,6 km".
3. **Kartu status GPS** (RcCard, tinggi ~120): kiri ikon sinyal 5 batang 40dp (batang terisi sesuai akurasi); tengah angka akurasi `numberL` ("8 m") + label "Akurasi GPS"; kanan ResultChip-gaya "Siap" (`pass`, <= 12 m), "Mencari sinyal" (`neutral`), atau "Lemah" (`warn`, > 12 m).
4. **Daftar segmen ringkas** (bergulir): baris 48dp: ikon tipe 20dp `kind*`, label `body`, di kanan `bodySmall` "400 m · ≤ 6:00". Pemisah halus. Blok Repeat ditulis sebagai satu baris "Ulangi 3x" lalu anak menjorok.
5. **Pengaturan cepat**: dua baris `ListRow` kecil dengan SegmentedControl: "Voice: ID | EN" dan "Hitung mundur: 0 | 3 | 5 | 10".
6. **Tombol bawah (sticky)**: `RcButton primary` tinggi **64dp**, penuh lebar, teks "MULAI" (`titleM`, huruf kapital), ikon `play_arrow`. Bila GPS belum siap: label "Tunggu GPS" dengan indikator kecil dan di bawahnya `tertiary` "Mulai saja".
**Aturan khusus:** izin belum ada -> tombol nonaktif + Banner `warn` "Izin lokasi diperlukan" dengan aksi "Berikan". Kelas Compact: sembunyikan blok 5 (pengaturan cepat dipindah ke ListRow ringkas).
**State:** loading: Skeleton untuk daftar segmen. Error: Banner.

### S05 Layar Lari (paling penting)
**Tujuan visual:** terbaca dalam sekilas, saat bergerak, di bawah matahari. Hierarki: **pace live >> target/status >> sisa segmen >> total >> kontrol**. Tidak bisa di-scroll. Satu layar tidak boleh memuat lebih dari satu warna status pada satu waktu.

**Susunan (layar acuan 360 x 800, Standard), berurutan dari atas:**

| Blok plan | Tinggi | Isi dan gaya |
|---|---|---|
| **A. Header** | 48dp | Kiri: nama workout `bodySmall` `textSecondary` (1 baris, elipsis). Kanan: ikon GPS 24dp (warna `pass`/`warn`/`fail`) + akurasi `caption` ("8 m"). Saat dijeda: chip "DIJEDA" (`neutral`) menggantikan akurasi. |
| **B. Segmen** | 96dp | Baris 1: ikon tipe 20dp (`kind*`) + label segmen `titleM`; kanan `label` "SEGMEN 3/9". Baris 2: SegmentProgressBar 10dp. Baris 3: kiri **sisa** `numberM` ("85 m" atau "02:41") + `bodySmall` "tersisa"; kanan `caption` "dari 200 m". Segmen Open: judul "Lari bebas", baris 3 menampilkan jarak dan waktu berjalan segmen, tanpa progress bar. |
| **C. Hero** | fleksibel (sisa ruang) | **HeroPace** rata tengah (96sp, `textPrimary`), satuan "/km" di bawah. Di bawahnya baris **target**: "TARGET" `label` + `≤ 6:30` `numberL` rata tengah (`textSecondary` untuk label, `textPrimary` untuk angka); tanpa target: "Tanpa target" `bodySmall`. Di bawahnya **StatusBadge** lebar penuh (44dp) dengan slot **tetap 52dp** (tidak ada geseran). Tanpa target: slot dibiarkan kosong. |
| **D. Metrik segmen** | 80dp | RcCard dua kolom MetricBlock: kiri "RATA-RATA SEGMEN" (`numberL`), kanan "BUTUH UNTUK SISA" (`numberL`; **hanya terisi saat TERLALU LAMBAT dan realistis**, selain itu kolom kanan dibiarkan kosong, kartu tetap 80dp). |
| **E. Total** | 64dp | Tiga MetricBlock sama lebar tanpa kartu, garis atas `outline`: "WAKTU" (`numberM`), "JARAK" (`numberM`, km dua desimal), "PACE RATA-RATA" (`numberM`, `textSecondary`). |
| **F. Berikutnya** | 64dp | RcCard rapat: ikon tipe `kind*` + `label` "BERIKUTNYA" + teks `body` "Jalan 400 m · ≤ 9:00". Segmen terakhir: "Lari bebas, tanpa target". |
| **G. Chip hasil** | menimpa F selama 8 dtk | Kartu setinggi F muncul fade + geser 8dp dari bawah di atas F: ResultChip besar + "Segmen 2: 6:28" lalu memudar kembali ke F. |
| **H. Kontrol** | 148dp | Baris 1 (rata tengah, jarak antar tombol 24dp): **Lewati** (RcIconButton `filled` 56dp, ikon `skip_next`), **Jeda/Lanjut** (lingkaran **88dp isi `accent`**, ikon 40dp `onAccent`), **Stop** (RcIconButton `filled` 56dp, ikon `stop` `fail`, dikelilingi **ProgressRing** untuk tekan-tahan 2 dtk). Baris 2: dua `tertiary` 44dp berdampingan: "Perpanjang" (ikon `add`) dan "Daftar segmen" (ikon `list`). |

**ASCII acuan susunan:**
```
 [ Nama workout            GPS 8 m ]  A
 [ Lari  Segmen 3/9               ]  B
 [ ▓▓▓▓▓▓▓░░░░░░░░                ]
 [ 85 m tersisa        dari 200 m ]
 [                                ]
 [             6:24               ]  C  (96sp)
 [               /km              ]
 [        TARGET  ≤ 6:30          ]
 [ [ ✓ STABIL                  ] ]  badge (slot tetap)
 [ ┌──────────────┬─────────────┐ ]  D
 [ │ RATA2 SEGMEN │ BUTUH SISA  │ ]
 [ │    6:27      │   (kosong)  │ ]
 [ └──────────────┴─────────────┘ ]
 [  WAKTU     JARAK    PACE RATA2 ]  E
 [  12:41     2,31     6:52       ]
 [ BERIKUTNYA  Jalan 400 m · ≤9:00]  F
 [  (skip)   ( JEDA 88 )   (stop) ]  H
 [  + Perpanjang   Daftar segmen  ]
```

**Aturan warna dan status di S05:**
- StatusBadge: STABIL = container `pass`; TERLALU LAMBAT = container `fail` + ikon `trending_down`; TERLALU CEPAT = container `fast` + ikon `trending_up`. Angka selisih (`+0:15`/`-0:22`) tabular di ujung kanan badge.
- Selain badge dan ikon GPS, tidak ada warna status lain di layar. Hero tetap `textPrimary`.
- Kontras S05 harus lolos **>= 7:1** untuk hero dan target (tinggi kontras dibutuhkan di bawah sinar matahari).

**Kelas tinggi layar:**
- **Compact** (< 680dp): hero 72sp; kartu F menjadi satu baris 40dp tanpa kartu; kartu D tinggi 64; tombol Jeda 72dp.
- **Standard**: sesuai tabel.
- **Tall** (> 820dp): ruang ekstra seluruhnya ke blok C (hero tetap 96sp, jarak antar elemen membesar), tidak menambah elemen.

**State S05:**
| State plan | Tampilan |
|---|---|
| WAITING_GPS | Hero diganti blok tengah: ikon sinyal 64dp, "Menunggu GPS" `titleM`, akurasi `numberL`; `secondary` "Mulai saja". Kontrol H tidak tampil. |
| COUNTDOWN awal | **Overlay penuh** isi `bg` @ 92%: "MULAI DALAM" `label` di atas, angka **160sp ExtraBold `accent`** di tengah (berdenyut tiap detik), nama workout `body` di bawah, `tertiary` "Batal". Pada 0: tampil "MULAI" 72sp `accent` selama 600ms. |
| RUNNING | Susunan A-H. |
| Countdown 3-2-1 antar segmen (waktu) | **Banner** menempel di bawah header: tinggi 64dp, container `accentContainer`, teks "Ganti ke JALAN dalam" `bodySmall` + angka `numberL` `accentText`. Tidak menutupi hero. |
| Cue jarak ("siap-siap") | Banner serupa: "Siap-siap: ganti ke JALAN" tanpa angka; hilang saat segmen berganti. |
| PAUSED manual / AUTO | Seluruh isi (B-F) opacity 55%; chip "DIJEDA"/"DIJEDA OTOMATIS" di header; tombol Jeda menjadi "Lanjut" (ikon `play_arrow`) dengan denyut pelan 1x. |
| GPS hilang | Banner `warn` "Sinyal GPS hilang" di bawah header; hero `--:--` `textTertiary`. |
| Rencana selesai | Banner `accentContainer` "Rencana selesai. Lanjut atau Stop"; segmen berlabel "Lari bebas". |
| Target jarak tercapai | Banner sukses `pass` selama 6 dtk "Target jarak tercapai". |
Banner lebih dari satu: yang lebih tinggi prioritasnya ditampilkan; tidak ditumpuk.

### S06 Selesai & Simpan
**Tujuan visual:** rasa selesai yang tenang, bukan perayaan berlebihan.
**Susunan (bergulir; tombol bawah sticky):**
1. Lingkaran 48dp `accentContainer` + ikon `check` `accentText`, di sebelahnya "Sesi tersimpan" `bodySmall` `textSecondary`.
2. Jarak **`displayL`** ("5,03") + "km" di samping.
3. Tiga MetricBlock sejajar: Waktu, Pace rata-rata, Segmen lulus ("7/9").
4. **ResultStrip** penuh lebar (warna per hasil segmen) + keterangan kecil `caption` jumlah LULUS/GAGAL/BORDERLINE.
5. **Banner rekor** bila ada: `accentContainer`, ikon `emoji_events` `accentText`, "Rekor baru: 5 km 28:40".
6. SectionHeader "CATATAN (OPSIONAL)". `RcTextField` "Nama workout" dengan placeholder berisi nama default (mis. "Lari Pagi"). Di bawahnya: "Seberapa capek? (1-10)" dengan baris 10 lingkaran 28dp bisa dipilih (terpilih: isi `accent`, angka `onAccent`; tidak terpilih: border `outlineStrong`); ketuk lagi untuk menghapus. Lalu `RcTextField` multiline "Catatan" dengan penghitung `caption` "0/500".
7. **Tombol bawah (sticky)**: `primary` "Simpan"; di bawahnya baris `tertiary` "Lewati" dan `destructive` "Buang sesi".
**Aturan khusus:** sesi < 10 m: "Buang sesi" dinaikkan menjadi `secondary` bersama Banner "Sesi sangat pendek". Back = Lewati.
**State:** tidak ada loading (data sudah tersimpan). Error simpan: Banner `fail`, data tetap aman.

### S07 Riwayat (tab Riwayat)
**Tujuan visual:** log rapi yang enak dipindai; angka jarak di kanan menjadi jangkar.
**Susunan:**
1. TopBar "Riwayat" (`titleL`) + ikon pencarian; pencarian membuka `RcTextField` penuh lebar di atas daftar.
2. **Header bulan** (menempel saat menggulir): "OKTOBER 2026" `label` kiri; kanan `bodySmall` "42,5 km · 9 sesi".
3. **Item sesi** (RcCard `interactive`, tinggi min 88): kiri atas nama `titleS` (+ ikon `emoji_events` 16dp `accentText` bila ada lencana rekor); kiri bawah `bodySmall` `textSecondary` "Sab, 10 Okt · 06:12"; kanan: jarak `numberM` + "km" kecil, di bawahnya `bodySmall` waktu. Baris bawah kartu: pace rata-rata `bodySmall` dan ResultChip kecil ("7/9 lulus" memakai `pass`/`warn` sesuai dominasi; "Tanpa target" `neutral`).
4. Tekan-tahan atau `more_vert`: menu kecil "Edit nama/catatan" (D03) dan "Hapus" (D09).
**State:** loading: Skeleton 5 item. Kosong: EmptyState `directions_run` "Belum ada sesi" + tombol "Mulai lari". Hasil pencarian kosong: "Tidak ada sesi dengan nama itu". Error: ErrorState.

### S08 Detail Sesi
**Tujuan visual:** laporan sesi yang bisa dibaca dalam 5 detik (header) dan digali dalam tab.
**Susunan:**
1. TopBar: panah kembali; kanan `more_vert` (Edit, Export GPX, Kalibrasi jarak, Hapus).
2. **Judul**: nama sesi `titleL` + ikon `edit` 20dp (membuka D03) di sebelahnya. Baris `bodySmall`: tanggal, jam mulai-selesai. Baris `caption`: nama workout snapshot.
3. **Jarak** `numberXL` + "km". Di bawahnya grid 3 MetricBlock: Waktu berjalan, Pace rata-rata, Hasil ("7 lulus"). Baris tambahan `bodySmall` bila ada waktu jeda ("Jeda 01:12") dan status rencana ("Rencana selesai" / "Dihentikan sebelum selesai").
4. **Lencana rekor**: satu chip `accentContainer` per rekor ("PR 5 km").
5. **ResultStrip** penuh lebar.
6. **Kartu Catatan** (RcCard): ikon `notes`; skala capek ditampilkan sebagai "Capek 6/10" `titleS`; teks catatan `body`; bila kosong: tombol teks "Tambah catatan" -> D03.
7. **Tab** (di bawah header, menempel saat menggulir): teks tab `titleS`, tab aktif `textPrimary` + garis bawah **3dp `accent`**, nonaktif `textSecondary`. Tab: Segmen, Split km, Grafik, (Peta setelah P3.1).
**Tab Segmen** (kartu per segmen, bukan tabel lebar): RcCard; baris 1: ikon tipe + "#3 Interval 1" `titleS` + ResultChip di kanan. Baris 2: `bodySmall` "Batas 200 m · Target ≤ 6:30". Baris 3: tiga nilai sejajar: Jarak, Waktu, Pace (`numberM`) dan selisih `bodySmall` berwarna (`pass` bila negatif/0, `fail` bila positif; BORDERLINE: `warn`). BORDERLINE menambah baris `caption` "Dalam batas error GPS". Segmen >= 1 km bertarget: chevron "Km di dalam segmen" yang membuka daftar kecil km + status. DILEWATI ditampilkan redup (opacity 60%).
**Tab Split km** (tabel dengan kolom tetap): header `label`: KM | WAKTU | PACE | SELISIH. Baris 52dp; nilai `numberM`; km tercepat disorot **batang kiri 3dp `accent`** + label "Tercepat" `caption`. Sisa (mis. 0,10 km) diberi label "0,10 km" dan pace `--:--` bila < 50 m. Kolom Selisih berisi `+0:05`/`-0:03` berwarna dan ikon status kecil bila relevan; selain itu kosong. Footer total di baris tebal.
**Tab Grafik:** dua ChartLine/ChartBar (6.1): (1) "Pace sepanjang jarak" garis 200dp dengan garis target tangga dan batas segmen; (2) "Pace per km" batang 160dp dengan garis target bila ada. Sumbu pace dibalik (lebih cepat di atas) dan diberi label "lebih cepat" `caption` di ujung atas.
**Tab Peta:** peta penuh lebar 360dp tinggi, polyline 4dp `accent`, marker start (lingkaran `pass`) dan finish (bendera `textPrimary`), penanda batas segmen titik putih kecil; tanpa tile saat offline: latar `surface` + Banner info.
**State:** loading: Skeleton header + tab. Error: ErrorState. Tab tanpa data: EmptyState lokal ("Tidak ada data km").

### S09 Ringkasan Mingguan (tab Statistik, sub-tab Mingguan)
**Tujuan visual:** dasbor tenang; satu minggu di depan, tren di belakang.
**Susunan:**
1. TopBar "Statistik" (`titleL`). Di bawahnya **SegmentedControl** dua pilihan: "Mingguan | Rekor".
2. **Navigator minggu**: baris tinggi 48: panah kiri, teks "12-18 Okt 2026" `titleS` di tengah, panah kanan (nonaktif di minggu ini); tombol `tertiary` kecil "Minggu ini" bila bukan minggu ini.
3. **Grid kartu angka** 2 kolom x 3 baris (RcCard, tinggi 84): Jarak, Waktu, Sesi, Pace rata-rata, Sesi terpanjang, Segmen lulus (%; BORDERLINE `caption` di bawahnya). Kartu Jarak memuat selisih vs minggu lalu dengan ikon + `bodySmall`.
4. **Grafik 1** (ChartBar 160dp) "Jarak per hari": 7 batang Sen-Min, hari ini disorot.
5. **Grafik 2** (ChartBar 160dp) "12 minggu terakhir": minggu terpilih disorot, tap memilih minggu.
6. **Grafik 3** (ChartLine 160dp) "Pace rata-rata per minggu": garis putus untuk minggu kosong.
7. **Rekor minggu ini**: daftar baris dengan ikon `emoji_events` `accentText`.
8. **Sesi minggu ini**: item seperti S07 (tanpa header bulan).
**State:** loading: Skeleton kartu dan grafik. Minggu kosong: grafik tetap tampil (nol); di bawah kartu angka tampil EmptyState kecil "Belum ada latihan minggu ini". Error: ErrorState.

### S10 Rekor Pribadi (tab Statistik, sub-tab Rekor)
**Tujuan visual:** deretan "piala" yang membuat ingin memecahkannya.
**Susunan:**
1. TopBar dan SegmentedControl seperti S09 (tab Rekor aktif).
2. **Lima kartu rekor** berurutan (1 km, 5 km, 10 km, Half Marathon, Marathon), RcCard `interactive` tinggi 104: kiri atas label jarak `titleS`; kiri bawah tanggal + nama sesi `bodySmall` `textSecondary`; kanan: waktu rekor **`numberXL`** `textPrimary`, di bawahnya "pace 5:46/km" `bodySmall`. Ikon `emoji_events` 24dp `accentText` di pojok kanan atas. Kartu "Belum ada": isi transparan, border putus-putus `outlineStrong`, ikon dan teks `textTertiary`, tulisan "Belum ada".
3. SectionHeader "REKOR TERBARU": daftar 5 baris (jarak, waktu, tanggal) `ListRow`.
4. Catatan `caption` `textTertiary`: "Dihitung dari bagian tercepat dalam sesi."
**State:** loading Skeleton; semua "Belum ada" tetap menampilkan lima kartu; error: ErrorState.

### S11 Detail Rekor
**Tujuan visual:** satu rekor sebagai pahlawan, perkembangannya di bawah.
**Susunan:** TopBar (panah kembali, judul "5 km"). **Hero**: waktu rekor `displayL`, di bawah "pace 5:46/km" `body` + tanggal + nama sesi (tap -> S08) + "dari km 2,3" `caption`. SegmentedControl "Waktu | Pace". **ChartLine 200dp**: best effort terbaik per sesi dari waktu ke waktu, titik rekor (PR saat itu) disorot (titik 6dp `accent` dengan cincin). **Top 10**: `ListRow`: nomor peringkat `numberM` (peringkat 1 `accentText`, lainnya `textSecondary`), waktu `titleS`, pace dan tanggal `bodySmall`, nama sesi `caption`.
**State:** loading Skeleton; sedikit data (< 2 sesi): grafik diganti teks "Butuh minimal 2 sesi untuk grafik".

### S12 Pengaturan
**Tujuan visual:** rapi, berkelompok, mudah dijangkau jempol.
**Susunan:** TopBar "Pengaturan". Kelompok dalam RcCard dengan SectionHeader di atas tiap kelompok, mengikuti tabel plan:
- **TAMPILAN**: SegmentedControl "Terang | Gelap | Sistem" (baris sendiri berlabel); `RcSwitchRow` "Layar tetap menyala".
- **VOICE**: SegmentedControl "Indonesia | English"; slider volume relatif (trek 4dp `outlineStrong`, bagian terisi `accent`, pegangan 24dp `accent`); tombol `secondary` "Uji suara"; baris status paket suara (ikon `check_circle` `pass` "Terpasang" atau `warning` `warn` "Belum terpasang" + aksi); `RcSwitchRow` km marker, alert terlalu cepat/lambat.
- **HITUNG MUNDUR**: SegmentedControl "Awal: 0 | 3 | 5 | 10 dtk"; "Antar segmen (waktu): Mati | 3 | 5"; "Jarak cue (segmen jarak): Mati | 10 | 20 | 30 | 50 m"; "Gaya: Beep | Suara".
- **ALERT**: NumberStepper margin "terlalu cepat" default; `RcSwitchRow` getar.
- **TRACKING**: `RcSwitchRow` auto-pause; baris kalibrasi ("Faktor 1,017" + `tertiary` "Reset").
- **DATA**: `ListRow` dengan chevron -> S13.
- **BANTUAN**: `ListRow` Panduan baterai -> S14; versi app; lisensi.
- **PENGEMBANG** (hanya debug): `ListRow` -> S15.
Pilihan 4-5 opsi yang tidak muat di SegmentedControl: gunakan baris chip bisa digulir horizontal (chip tinggi 40, terpilih isi `accent`).
**State:** tidak ada loading. Perubahan tema langsung terlihat.

### S13 Cadangan & Data
**Susunan:** TopBar. Kartu ringkasan (3 MetricBlock: Sesi, Template, Ukuran). Kartu cadangan: "Cadangan terakhir: 3 Okt 2026" (atau "Belum pernah" `warn`). `RcButton primary` "Cadangkan ke file", `secondary` "Pulihkan dari file". Pengingat: SegmentedControl "Mati | 2 minggu | Bulanan". **Zona bahaya** di paling bawah: RcCard border `fail` 1dp, judul "Zona bahaya", `destructive` "Hapus semua data".
**Aturan:** Pulihkan menampilkan RcDialog peringatan menimpa. Hapus semua = D09 dua langkah.
**State:** proses cadangan/pulih: tombol `loading` + Banner info; error: Banner `fail` spesifik (mis. "File rusak").

### S14 Panduan Baterai
**Susunan:** TopBar. Pilih merek lewat chip bisa digulir (Xiaomi, Oppo/Realme, Vivo, Samsung, Lainnya). Langkah bernomor dalam RcCard: lingkaran nomor 28dp `accentContainer` + teks `body`. Baris status "Optimasi baterai: dikecualikan" (`pass`) / "Belum dikecualikan" (`warn`). Tombol `primary` "Buka pengaturan baterai" sticky.
**State:** status tidak terdeteksi -> baris status disembunyikan.

### S15 Debug / Simulator (hanya build debug)
**Tujuan visual:** fungsional, tetap memakai token dan komponen yang sama. Dropdown workout dan skenario (RcTextField pilihan), SegmentedControl kecepatan "1x | 10x | 60x", `primary` "Jalankan", panel log monospaced (`caption`, font monospace sistem) dengan event alert dan jumlah sample dibuang. Boleh lebih padat; tidak perlu pemolesan ekstra.

---

## 9. Spesifikasi Sheet dan Dialog

Semua sheet memakai **RcSheet**, semua dialog **RcDialog**. Tombol utama di kanan/bawah, tombol batal di kiri/atas.

| ID | Bentuk | Susunan dan gaya |
|---|---|---|
| **D01 Editor Segmen** | Sheet tinggi penuh (bergulir), tombol "Simpan" sticky | 1. `RcTextField` label. 2. **Tipe**: grid 2 kolom kartu pilihan (tinggi 64: ikon `kind*` + nama `titleS`; terpilih: border `accent` 2dp + latar `accentContainer`); **InfoIcon (ⓘ)** di pojok kanan atas tiap kartu. 3. **Batas**: SegmentedControl "Jarak | Durasi | Tanpa batas" (Tanpa batas hanya aktif jika segmen terakhir); input angka besar `numberXL` dengan NumberStepper (jarak: 50 m / 0,1 km; durasi: 5 dtk) dan toggle satuan m/km. 4. **Target pace**: dua NumberStepper `mm` dan `ss` (langkah 5 dtk) dengan ":" di antara, plus `RcSwitchRow` "Tanpa target"; di samping label ada InfoIcon. 5. **Margin "terlalu cepat"**: NumberStepper (langkah 5 dtk) + switch "Mati"; InfoIcon. 6. **Per-km check**: `RcSwitchRow` (nonaktif dan redup bila syarat jarak/target tidak terpenuhi, dengan teks `caption` alasan); InfoIcon. Validasi inline di bawah tiap field. |
| **D02 Preset Generator** | Sheet setengah-penuh | Empat kartu pilihan 2x2 (Stable run, Progressive tempo, Interval, Run/walk) dengan ikon dan satu kalimat; memilih membuka form parameter (NumberStepper) dan **pratinjau SegmentStrip** hasilnya; tombol "Buat" `primary`. |
| **D03 Edit Sesi** | Sheet | `RcTextField` nama (placeholder = nama default; kosong = default), baris skala capek 1-10 (komponen sama dengan S06), `RcTextField` catatan, `RcSwitchRow` "Jangan hitung untuk rekor pribadi" dengan penjelasan `caption`. Tombol "Simpan" `primary`, "Batal" `tertiary`. |
| **D04 Lanjutkan Sesi** | RcDialog | Judul "Lanjutkan sesi?"; isi: nama workout, jarak, waktu, kapan disimpan (`body`). Tombol `tertiary` "Buang" (`destructive`) dan `primary` "Lanjutkan". |
| **D05 Konfirmasi Stop** | RcDialog | Judul "Hentikan sesi?"; isi menampilkan jarak dan waktu dalam `numberM`. Tombol `tertiary` "Lanjut lari" dan `destructive` terisi "Hentikan & simpan". |
| **D06 Konfirmasi Skip Segmen** | RcDialog | Menampilkan dua baris: segmen yang dilewati (dengan ikon tipe) dan segmen berikutnya; catatan `bodySmall` "Segmen yang dilewati ditandai DILEWATI". Tombol "Batal" dan `primary` "Lewati". |
| **D07 Perpanjang Segmen** | Sheet rendah | Deretan chip pilihan (+100 m, +500 m untuk jarak; +30 dtk, +1 mnt untuk durasi), chip "Lainnya" membuka NumberStepper. Tombol `primary` "Perpanjang". Tombol dan chip berukuran besar (>= 48dp) karena dipakai saat berlari. |
| **D08 Kalibrasi Jarak** | Sheet | Menampilkan "Jarak terukur" `numberM`; field "Jarak sebenarnya (km)" NumberStepper; hasil "Faktor koreksi 1,017" dengan batas 0,95-1,05 (peringatan `warn` bila terpotong). Tombol "Terapkan untuk sesi berikutnya". |
| **D09 Konfirmasi Hapus** | RcDialog | Judul menyebut objek ("Hapus sesi ini?"), isi menyebut konsekuensi. Tombol `destructive` terisi. **Hapus semua data**: dua langkah; langkah kedua meminta mengetik "HAPUS" di `RcTextField`, tombol baru aktif bila cocok. |
| **D10 Daftar Segmen (live)** | Sheet tinggi 60% | **Linimasa vertikal**: garis 2dp `outline` di kiri, titik di tiap segmen: selesai = lingkaran 20dp berisi ikon (`check_circle` `pass`, `cancel` `fail`, `warning` `warn`, `skip_next` `neutral`); berjalan = cincin `accent` 24dp dengan progres; belum = lingkaran kosong 16dp `outlineStrong`. Di kanan titik: label `titleS`, `bodySmall` batas + target, dan untuk yang selesai pace rata-rata + ResultChip. Segmen berjalan disorot latar `accentContainer`. Gulir otomatis ke segmen berjalan. |
| **D11 Info Segmen & Istilah** | Sheet 75%, bergulir | Isi dari 8.3 plan. Kepala: lingkaran 56dp berwarna `kind*` @ 20% dengan ikon tipe, judul `titleM` (atau judul istilah dengan ikon `info`). Bagian demi bagian: label `label` `accentText` (KEGUNAAN, KAPAN DIPAKAI, CONTOH PENGATURAN, PERILAKU DI APLIKASI, TIPS) lalu teks `body` (tinggi baris 24sp). **CONTOH** ditampilkan sebagai kartu `surfaceRaised` dengan SegmentStrip contoh dan teks pengaturan. Untuk istilah: bagian PENJELASAN dan CONTOH/TIPS. Tombol bawah `primary` "Mengerti". |

---

## 10. Microcopy

Nada: ringkas, hangat, tanpa tanda seru berlebihan, tanpa istilah teknis berat. Gunakan "kamu" bila perlu menyapa, selebihnya kalimat netral. Angka dan satuan sesuai 6.13 plan.

| Konteks | Indonesia | English |
|---|---|---|
| Tombol mulai | Mulai | Start |
| Jeda / lanjut | Jeda / Lanjut | Pause / Resume |
| Lewati segmen | Lewati | Skip |
| Hentikan | Hentikan | Stop |
| Perpanjang | Perpanjang | Extend |
| Status live | STABIL / TERLALU LAMBAT / TERLALU CEPAT | STEADY / TOO SLOW / TOO FAST |
| Hasil | LULUS / GAGAL / BORDERLINE / TANPA TARGET / DATA KURANG / DILEWATI | PASSED / FAILED / BORDERLINE / NO TARGET / NOT ENOUGH DATA / SKIPPED |
| GPS | Siap / Mencari sinyal / Lemah / Sinyal GPS hilang | Ready / Searching / Weak / GPS signal lost |
| Tidak ada nilai | -- | -- |
| Kosong riwayat | Belum ada sesi | No sessions yet |
| Banner rekor | Rekor baru: {jarak} {waktu} | New record: {distance} {time} |
| Konfirmasi hapus | Hapus sesi ini? Tindakan ini tidak bisa dibatalkan. | Delete this session? This can't be undone. |
| Keterangan borderline | Dalam batas error GPS | Within GPS error margin |
| Banner rencana selesai | Rencana selesai. Lanjut atau Stop | Plan complete. Keep going or Stop |

Aturan: tombol selalu berupa kata kerja; pesan error menyebut apa yang salah dan apa yang bisa dilakukan; tidak ada pesan "Terjadi kesalahan" tanpa penjelasan.

---

## 11. Aksesibilitas dan Kualitas

- Kontras sesuai bagian 3.4; unit test kontras wajib hijau.
- Ukuran sentuh >= 48dp. Jarak antar kontrol utama di S05 >= 24dp supaya tidak salah tekan.
- Setiap ikon fungsional punya `contentDescription`; ikon dekoratif `null`.
- Status (STABIL, GAGAL, dst.) tidak pernah hanya warna: ada ikon dan teks.
- TalkBack: tiap StatusBadge dan ResultChip dibacakan teksnya; perubahan status di S05 diumumkan sebagai live region **sopan** (tidak memotong).
- Font scale sampai 1.3: tidak ada teks terpotong atau bertumpuk di semua halaman; hero S05 autosize menjaga tetap satu baris.
- Fokus/keyboard: urutan fokus logis (atas ke bawah, kiri ke kanan); field mengambang tidak tertutup keyboard (`imePadding`).
- "Hapus animasi" sistem dihormati (bagian 5.5).
- Performa: S05 tidak boleh rekomposisi penuh tiap detik; hanya nilai yang berubah (pace, waktu, jarak) yang diperbarui.

---

## 12. Task UI, Hubungan dengan Task Plan, dan Definition of Done

### 12.1 Task UI (dikerjakan sebelum P1.1)

**U0 Fondasi tema**
- `RcColors` (dark + light) dari bagian 3, `RcTypography` (Inter dibundel, tnum) dari bagian 4, `RcSpacing`, `RcShapes`, `RcMotion`, `CompositionLocal`, `RcTheme` yang membungkus `MaterialTheme` (dynamic color mati, `ColorScheme` dan `Typography` dipetakan dari token). Ikon VectorDrawable dari daftar 5.4.
- AC: aplikasi kosong tampil dengan `bg`/`textPrimary` yang benar di kedua tema; tidak ada warna ungu bawaan; unit test kontras seluruh pasangan teks/latar hijau; font Inter termuat tanpa jaringan; `MainActivity` lama (template) diganti memakai `RcTheme`.

**U1 Katalog komponen**
- Seluruh komponen bagian 6, masing-masing dengan `@Preview` (light + dark, semua state). Satu layar internal "Katalog Komponen" (hanya build debug, boleh diakses dari S15) yang menampilkan semuanya.
- AC: setiap komponen sesuai spesifikasi ukuran dan token; tidak ada hex/dp literal di luar file token; katalog bisa dibuka dan dibandingkan dengan bagian 6.

**U2 Penerapan per halaman**: bukan task terpisah; dikerjakan bersama task plan yang membangun halaman (12.2).

### 12.2 Pemetaan task plan ke bagian dokumen ini

| Task plan | Halaman / sheet | Bagian DESIGN.md |
|---|---|---|
| P1.1 | S01, S14 | 8: S01, S14 |
| P1.5 | D04 | 9: D04 |
| P1.6 | S15 | 8: S15 |
| P1.7 | S05 (blok A-F, state dasar) | 8: S05 |
| P1.8 | S05 (countdown, banner cue) | 8: S05 (state COUNTDOWN, banner) |
| P1.9 | S05 (kontrol H, blok G), D05, D06, D07, D10 | 8: S05; 9: D05-D07, D10 |
| P2.2 | S03, D01, D02, D11 | 8: S03; 9: D01, D02, D11 |
| P2.3 | S02 | 8: S02 |
| P2.4 | S04 | 8: S04 |
| P2.5 | S06 | 8: S06 |
| P2.6 | S07, D03 | 8: S07; 9: D03 |
| P2.7 | Komponen chart | 6.1 |
| P2.8 | S08 | 8: S08 |
| P2.9 | S10, S11 | 8: S10, S11 |
| P2.10 | S09 (+ kartu di S02) | 8: S09, S02 |
| P2.11 | S12 | 8: S12 |
| P3.1-P3.2 | Tab Peta di S08, peta live S05 | 8: S08 (Tab Peta) |
| P3.5 | S13 | 8: S13 |
| P3.7 | D08 | 9: D08 |
| (lintas halaman) | D09 | 9: D09 |

### 12.3 Definition of Done untuk task UI
Task yang membangun halaman baru dianggap selesai hanya jika:
1. Susunan, ukuran, warna, dan teks sesuai bagian halaman di dokumen ini, memakai komponen dan token (tanpa nilai literal).
2. Semua state (normal, loading, kosong, error, dan state khusus S05) punya `@Preview` light dan dark.
3. Diuji di **360 x 800**, **411 x 914**, **320 x 640**, font scale 1.0 dan 1.3: tidak ada teks terpotong, layout loncat, atau elemen tumpang tindih.
4. **Tinjauan screenshot**: jalankan di emulator, ambil screenshot tiap state (`adb exec-out screencap -p > docs/ui-review/<ID>-<state>-<tema>.png`), bandingkan dengan teks spesifikasi, dan tulis daftar selisih yang ditemukan serta yang diperbaiki di `docs/PROGRESS.md`. Jika agent tidak bisa melihat gambar, ia mencatat nilai ukuran yang diukur dari layout inspector sebagai pengganti.
5. **Daftar anti-default** terpenuhi: tanpa ungu bawaan Material, tanpa dynamic color, tanpa tombol pil standar, tanpa AppBar rata tengah, tanpa FAB bulat default, tanpa bayangan, tanpa font sistem untuk angka.
6. Bila library screenshot-test (mis. Roborazzi) ingin dipakai, itu dependency baru: catat di `docs/DECISIONS.md` dulu. Tidak wajib.
7. Semua teks dari `strings.xml` (ID dan EN).
