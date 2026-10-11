# UPDATE-BUGS-AND-FEATURES.md: Daftar Bug, Perubahan Perilaku, dan Fitur Baru

Dokumen kerja untuk agent. Semua keputusan sudah **final** (lihat bagian 2). Agent **tidak perlu bertanya**; bila ada celah, ambil opsi yang tertulis sebagai "default" dan catat di `docs/DECISIONS.md`.

## 0. Cara memakai
1. Kerjakan berurutan sesuai bagian 9. Satu item = satu commit, pesan `B1: ...`, `F3: ...`.
2. **Dokumen yang sudah ada tetap berlaku**: `RUNCOACH_PLAN.md` (data/rumus), `DESIGN.md` + `DESIGN-RESKIN.md` (visual), `APP_GUIDE.html` (isi halaman existing). Dokumen ini hanya **menambah/mengubah** yang tertulis di sini. Bila bentrok, dokumen ini menang untuk item yang dibahas.
3. **Mock visual** ada di folder `mock/` (buka file HTML di browser). Bila kata dan mock berbeda, **mock yang menang untuk tampilan**; angka dan teks di mock adalah **contoh**.
4. Aturan anti-slop plan tetap: engine pure Kotlin, tanpa angka ajaib di luar `EngineConfig`, semua teks di `strings.xml` (ID dan EN), tanpa dependency baru (kecuali dicatat di DECISIONS), tidak ada `@ForeignKey`.
5. Setiap item punya **Acceptance Criteria (AC)**. Item selesai hanya jika semua AC terpenuhi dan test hijau.

## 1. Ringkasan

| ID | Jenis | Judul | Prioritas | Mock |
|---|---|---|---|---|
| B1 | Bug | Mock data muncul lagi; harus hanya debug | Tinggi | (tanpa) |
| B2 | Bug | Voice alert tidak lengkap (run/walk berbasis menit: tidak ada sama sekali) | Tinggi | (tanpa) |
| B3 | Bug | Segmen tanpa target tidak menampilkan pace aktual di riwayat | Sedang | `M4` |
| B4 | Bug | Peta hanya punya titik start; titik end belum ada | Sedang | `M3` |
| I1 | Investigasi | Rekor 1 km 6:20 tidak ada di split km riwayat | Tinggi | `M4` |
| P1 | Perilaku | Stop di tengah segmen: segmen berjalan dan sesudahnya GAGAL | Sedang | `M4` |
| P2 | Perilaku | Warna polyline menjadi oranye | Rendah | `M3` |
| F1 | Fitur | Salin struktur workout sebagai teks | Sedang | `M4` |
| F2 | Fitur | Blok Repeat dan isinya bisa dinaik-turunkan di editor | Sedang | `M5` |
| F3 | Fitur | Share PNG (pilih data, pilih template) | Sedang | `M1`, `M2` |
| F4 | Fitur | Simpan sebagai gambar (PNG transparan) | Sedang | `M1`, `M2` |
| F5 | Fitur | Bar chart segmen yang lulus (di dalam F3/F4) | Sedang | `M2` |
| F6 | Fitur | Peta digambar per segmen, tampil sebagai satu rute | Sedang | `M3` |
| F7 | Fitur | (Opsional) Ketuk segmen di peta: tooltip jarak dan waktu | Rendah | `M3` |

Mock: `mock/M1-share-picker.html`, `mock/M2-share-output.html`, `mock/M3-map-segments.html`, `mock/M4-history-segments.html`, `mock/M5-editor-reorder.html`. Semuanya memakai `mock/reskin.css`.

## 2. Keputusan final (jangan ditanyakan lagi)
1. **F1:** salin **struktur saja** (urutan segmen, batas, target). Tanpa pace aktual, tanpa status lulus/gagal. Tujuannya "ini plan saya" di deskripsi Strava.
2. **F2:** blok Repeat dan elemen di dalam blok sama-sama bisa dipindah (aturan di F2).
3. **F3:** pilih **data** dulu, lalu pilih **template**. Data yang bisa dipilih: nama sesi + tanggal/jam, jarak, waktu, pace rata-rata, split km, hasil segmen (bar chart, hanya yang lulus), polyline rute. **Tidak ada** rekor, skala capek, catatan, atau jumlah segmen lulus. Template **Lengkap** menampilkan **hanya yang dicentang**.
4. **F4/F3:** semua gambar PNG berlatar **transparan**. Warna teks bisa dipilih: Putih atau Hitam.
5. **F5:** bar chart hanya memuat segmen berstatus **LULUS** (BORDERLINE tidak ikut).
6. **F6:** rute digambar per segmen; di tampilan peta tetap terlihat satu rute utuh.
7. **P2:** satu warna oranye; segmen dibedakan dengan **titik kecil di batas segmen**.
8. **B2:** alert "terlambat/terlalu cepat/stabil" hanya untuk **segmen yang punya target pace**. Alert lain berlaku untuk semua segmen.
9. **P1:** segmen **tanpa target** sesudah stop tetap **Tanpa target** (tidak ikut gagal).
10. **I1:** sudah dicek oleh pemilik: sesi pemilik rekor ada, dan tidak ada split km 6:20.
11. **Dibatalkan:** perubahan "pace live diganti pace rata-rata sejak mulai" (N1). Layar live tetap seperti sekarang.

---

## 3. Bug

### B1. Mock data muncul lagi (hanya boleh ada di debug)
**Gejala:** semua riwayat dihapus, app ditutup lalu dibuka lagi: mock riwayat dan mock workout muncul kembali.
**Perilaku yang benar:**
- Build **release/APK bersih**: **nol** mock data, **nol** kode mock di dalam APK. S07 kosong, S02 hanya berisi template bawaan **Lari Bebas**, S09 kosong, S10 semua kartu "Belum ada".
- Build **debug**: mock data diisi **sekali** pada peluncuran pertama; menghapusnya tidak membuatnya muncul lagi.

**Rincian implementasi (wajib):**
1. Pindahkan semua kode dan data mock ke source set **`app/src/debug/`**. Di `main/` hanya ada interface `MockDataSeeder` dengan implementasi **no-op** (di `src/release/` atau default). Release tidak boleh berisi kelas, aset, atau string mock.
2. Seeder debug berjalan **otomatis hanya sekali**, dijaga flag `debug_mock_seeded` yang disimpan di **DataStore terpisah** (`debug_flags`). Flag ini **tidak pernah** dihapus oleh: "Hapus semua data" (S13), hapus sesi/template, atau restore cadangan. Seeder **tidak boleh** memakai kondisi "database kosong" untuk memutuskan seeding.
3. Setiap baris mock memakai id berawalan **`mock_`** (sesi, template, dan seluruh baris turunannya) agar bisa dikenali. Tanpa perubahan skema.
4. Template bawaan **Lari Bebas** bukan mock. Ia tetap ada di release, tidak bisa dihapus/diedit, dan dibuat oleh migrasi/inisialisasi biasa.
5. Di **S15** (hanya debug) tambahkan dua tombol sekunder: **"Isi mock data"** dan **"Hapus mock data"** (hanya baris berawalan `mock_`). Tombol ini satu-satunya cara seeding ulang.
6. Penghapusan sesi (satu atau semua) wajib menghapus seluruh baris turunan dalam **satu transaksi**: `SegmentResult`, `KmSplit`, `TrackPoint`, **`BestEffort`**. Setelah itu rekor dihitung ulang dari `BestEffort` yang tersisa (plan bagian 7).

**AC:**
- [ ] `assembleRelease` lalu `apkanalyzer dex packages app-release.apk | grep -i mock` tidak mengeluarkan apa pun; `unzip -l` tidak menemukan aset mock.
- [ ] Instal release di emulator bersih: S07 kosong, S02 hanya Lari Bebas, S09 kosong, S10 lima kartu "Belum ada", S13 menunjukkan Sesi 0 dan Template 1.
- [ ] Debug, instal bersih: mock muncul sekali. Hapus semua data lewat S13, matikan app (swipe dari recents), buka lagi: **tidak ada mock**, S10 semua "Belum ada".
- [ ] S15 "Isi mock data" mengisi ulang; "Hapus mock data" hanya menghapus baris `mock_` (data pengguna sungguhan tetap).
- [ ] Test instrumented: hapus sesi pemilik rekor menghapus `BestEffort`-nya dan rekor turun ke yang berikutnya atau "Belum ada"; tidak ada baris yatim di semua tabel.

---

### B2. Voice alert tidak lengkap
**Gejala:** (1) hanya ada ucapan saat segmen mulai dan segmen hampir selesai. (2) Pada **run/walk berbasis menit** tidak ada alert sama sekali. **Harapan:** setiap alert pada matriks di bawah berbunyi pada kondisinya.

**Langkah wajib (urut):**
1. **Reproduksi** di S15 dengan workout run/walk (Run 6 mnt, Walk 2 mnt, sampai 5 km) dan skenario `steady`. Catat log event dan daftar ucapan yang dikirim ke TTS.
2. **Cari akar masalah.** Periksa dan catat hasilnya di `docs/DECISIONS.md`:
   - (a) Apakah engine mengeluarkan event? (unit test per event, lihat AC).
   - (b) Apakah service meneruskan semua event ke `TtsSpeaker`? (bukan hanya SegmentStart/PreWarning).
   - (c) Apakah event berbasis waktu dipicu oleh `onTick` yang **berjalan sendiri** (coroutine ~250 ms), tidak hanya saat sample GPS masuk? Segmen waktu tanpa GPS tick tidak boleh macet.
   - (d) Apakah gerbang (F14) salah dipakai untuk **semua** event? Gerbang hanya untuk alert koreksi (TooSlow/TooFast/BackToStable), bukan untuk event lain, dan **tidak berlaku** untuk segmen tanpa target.
   - (e) Apakah aturan pembuangan antrean rendah (5 detik) atau prioritas membuang event penting? Event bertabrakan harus digabung (lihat aturan gabung).
   - (f) Apakah TTS gagal init, bahasa belum terpasang, audio focus dilepas terlalu cepat, atau pengaturan bawaan mematikan alert?
3. **Perbaiki**, lalu verifikasi dengan matriks dan test di bawah.

**Matriks alert (semua wajib berbunyi bila syarat terpenuhi):**

| ID | Kondisi | Pemicu | Ucapan ID | Ucapan EN | Berlaku untuk | Dimatikan oleh |
|---|---|---|---|---|---|---|
| A01 | Hitung mundur awal | Tiap detik N..1 | beep atau "Lima. Empat..." | beep atau "Five. Four..." | Semua | Countdown awal = 0 |
| A02 | Workout mulai | Countdown selesai (atau langsung bila 0) | "Mulai." | "Go." | Semua | (tidak ada) |
| A03 | Segmen mulai | Awal tiap segmen | "Segmen {i} dari {n}. {tipe} {batas}. Target pace {pace}." | "Segment {i} of {n}. {type} {limit}. Target pace {pace}." | Semua | (tidak ada) |
| A04 | Peringatan sebelum ganti | Sisa 10 dtk | "10 detik lagi ganti ke {tipe}." | "10 seconds until {type}." | Segmen waktu >= 30 dtk, ada segmen berikutnya | (tidak ada) |
| A05 | Hitung mundur 3-2-1 | Sisa 3, 2, 1 dtk | beep atau "Tiga. Dua. Satu." | beep or "Three. Two. One." | Segmen waktu >= 8 dtk, ada berikutnya | Antar segmen = Mati |
| A06 | Cue jarak | Sisa jarak <= jarak cue | "Siap-siap. Sebentar lagi ganti ke {tipe}." | "Get ready. Switching to {type} soon." | Segmen jarak, limit >= 2x cue, ada berikutnya | Jarak cue = Mati |
| A07 | Segmen selesai | Batas tercapai | dengan target: "Segmen selesai. Rata-rata {pace}. {Lulus/Gagal/Borderline}." Tanpa target: "Segmen selesai. Rata-rata {pace}." | "Segment done. Average {pace}. {Passed/Failed/Borderline}." / "Segment done. Average {pace}." | Semua | (tidak ada) |
| A08 | Terlalu lambat | F13/F14 | "Terlalu lambat. Pace sekarang {p}, target {t}. Butuh {r} untuk sisa jarak ini." | "Too slow. Current pace {p}, target {t}. You need {r} for the rest." | **Hanya segmen dengan target** | Alert pace = Off |
| A09 | Terlalu cepat | F13/F14 | "Terlalu cepat. Pace sekarang {p}, target {t}." | "Too fast. Current pace {p}, target {t}." | **Hanya segmen dengan target** | Alert pace = Off |
| A10 | Kembali stabil | Keluar dari A08/A09 | "Pace stabil. Sekarang {p}, target {t}." | "Pace is steady. Now {p}, target {t}." | **Hanya segmen dengan target** | Alert pace = Off |
| A11 | Penanda km | Tiap 1 km kumulatif workout | "{k} kilometer. Waktu {t}. Pace {p}." | "{k} kilometer. Time {t}. Pace {p}." | Semua | Km marker = Off |
| A12 | Target jarak tercapai | Sekali, saat jarak total >= goal | "Target jarak tercapai. Lanjut atau berhenti kapan saja." | "Distance goal reached. Keep going or stop anytime." | Workout dengan goal | (tidak ada) |
| A13 | Rencana selesai | Langkah terakhir selesai | "Rencana workout selesai." | "Workout plan complete." | Semua | (tidak ada) |
| A14 | GPS hilang | 10 dtk tanpa sample valid | "Sinyal GPS hilang." | "GPS signal lost." | Semua | (tidak ada) |
| A15 | GPS pulih | Sample valid kembali | "Sinyal GPS kembali." | "GPS signal is back." | Semua | (tidak ada) |
| A16 | Auto-pause | Auto-pause aktif | "Dijeda otomatis." | "Auto-paused." | Semua | Auto-pause = Off |
| A17 | Auto-resume | Auto-resume | "Dilanjutkan." | "Resumed." | Semua | Auto-pause = Off |

Aturan tambahan:
- Format angka/pace/waktu/jarak mengikuti F20 plan. `{tipe}`: ID pemanasan/lari/jalan/pemulihan/pendinginan; EN warm up/run/walk/recovery/cool down.
- Segmen **tanpa target**: A03 **tidak** menyebut "Target pace". Segmen berbasis jumlah ulangan tak diketahui (Repeat sampai jarak): A03 memakai "Segmen {i}." tanpa "dari {n}". Segmen Open: "Lari bebas."
- **Aturan gabung:** bila A07 dan A03 jatuh pada saat yang sama (pergantian segmen), keduanya digabung **satu ucapan** (A07 dulu, lalu A03). Bila A11 jatuh dalam 5 detik dari ucapan prioritas lebih tinggi, ia ikut aturan antrean plan (dibuang bila tertunda > 5 dtk).
- Skip segmen: A07 tidak diucapkan untuk segmen yang dilewati, A03 untuk segmen berikutnya tetap. Perpanjang segmen: tanpa ucapan. **Stop: tidak ada ucapan.**
- Prioritas, hysteresis, cooldown, gerbang, dan format mengikuti `RUNCOACH_PLAN.md` (6.7, F13-F15, F20). Alert koreksi tetap tidak berbunyi pada: 100 m / 20 dtk pertama segmen, segmen jarak < 400 m atau segmen waktu < 60 dtk, 15 dtk terakhir segmen.

**Urutan ucapan yang diharapkan pada run/walk (Run 6 mnt, Walk 2 mnt, sampai 5 km, countdown awal 5, countdown segmen 3, tanpa target):**
`t=-5..-1` A01 -> `t=0` A02 + A03 ("Segmen 1. Lari 6 menit.") -> `5:50` A04 ("10 detik lagi ganti ke jalan.") -> `5:57, 5:58, 5:59` A05 -> `6:00` A07+A03 gabung ("Segmen selesai. Rata-rata {p}. Segmen 2. Jalan 2 menit.") -> `7:50` A04 ("10 detik lagi ganti ke lari.") -> `7:57-7:59` A05 -> `8:00` A07+A03 ("... Segmen 3. Lari 6 menit.") -> dan seterusnya. Setiap kilometer kumulatif: A11. Saat total jarak mencapai 5 km: A12; setelah segmen berjalan selesai: A07, lalu A13. **Tidak ada A08/A09/A10.**

**Ringkasan per workout contoh (untuk test):**

| Workout | Wajib ada | Tidak boleh ada |
|---|---|---|
| Stable 5 km @7:00 | A01, A02, A03, A11 x5, A07, A13; A08/A09/A10 sesuai skenario (setelah 100 m/20 dtk) | A04, A05, A06 (tidak ada segmen berikutnya) |
| Interval (warm up 1500 m, 3x [200 m, 400 m], cooldown 1000 m) | A03 tiap segmen, A06 di segmen jarak yang ada berikutnya, A07 tiap segmen, A11 | A08/A09/A10 di segmen **200 m** (di bawah 400 m). Segmen 400 m **boleh** mendapat alert koreksi (memenuhi gerbang, setelah 100 m/20 dtk pertama dan di luar 15 dtk terakhir) |
| Tempo (4 x 1 km + cooldown) | A03, A06, A07, A08/A09/A10 (segmen 1 km >= 400 m), A11 | A04, A05 |
| Run/walk berbasis menit | Urutan di atas | A06, A08, A09, A10 |
| Lari Bebas | A01, A02, A03 ("Lari bebas."), A11, A14-A17 | A04-A10, A12, A13 |

**AC:**
- [ ] Untuk **setiap** ID A01-A17: ada unit test engine yang memicu kondisinya dan memeriksa teks ID dan EN persis (snapshot).
- [ ] `SpeechSink` diperkenalkan sebagai antarmuka; `RecordingSpeechSink` (fake) merekam (waktu, teks, prioritas).
- [ ] Test service/simulator di 60x: kelima workout contoh menghasilkan urutan ucapan sesuai tabel di atas (golden file).
- [ ] Run/walk berbasis menit: seluruh urutan di atas muncul, dengan atau tanpa sample GPS selama 10 detik (tick mandiri).
- [ ] Mematikan "Alert pace" menghilangkan hanya A08-A10; "Km marker" hanya A11.
- [ ] Uji lapangan oleh pemilik (checklist di bagian 10) lulus: semua alert terdengar dengan layar mati dan musik di-duck.

---

### B3. Segmen tanpa target tidak menampilkan pace aktual di riwayat
**Gejala:** di S08 (tab Segmen), kartu segmen tanpa target tidak menampilkan pace aktual.
**Perilaku yang benar:** pace aktual **selalu** tampil untuk setiap segmen yang punya data jarak. Lihat `mock/M4-history-segments.html` (kartu "#2 Jalan 2 mnt").
**Rincian:**
- `SegmentResult.avgPace` dihitung dan **disimpan untuk semua segmen** (termasuk NO_TARGET): `segMovingSec / (Ds/1000)` (F9). Kolom boleh null hanya bila `Ds < 50 m` (tampil `--:--`).
- Kartu segmen: baris "TARGET" tampil `—` untuk segmen tanpa target; "AKTUAL" menampilkan pace; "SELISIH" tampil `—`. Chip status "TANPA TARGET" (neutral).
- Segmen berbasis waktu juga menampilkan jarak aktual dan waktu pada baris "Batas ... · jarak · waktu".
**AC:**
- [ ] Run/walk: tiap segmen Run dan Walk menampilkan pace aktual yang sama dengan `waktu/jarak` segmen.
- [ ] Segmen dengan `Ds < 50 m`: `--:--`, bukan kosong dan bukan 0:00.
- [ ] Unit test `SegmentEvaluator`: NO_TARGET menghasilkan `avgPace` non-null untuk `Ds >= 50 m`.

---

### B4. Peta hanya punya titik start
**Perilaku yang benar:** peta (S08 tab Peta) menampilkan titik **start dan end (finish)**. Lihat `mock/M3-map-segments.html`.
**Rincian:**
- **Start:** lingkaran 18 px (9 dp) isi `pass` (#34D399), cincin putih 2,5 px.
- **End:** lingkaran 20 px (10 dp) isi `#F2F5F7` dengan titik tengah 8 px `#0B0D0F`.
- End = titik GPS valid **terakhir** sesi (termasuk bila sesi dihentikan di tengah segmen). Bila start dan end berjarak < 20 m (rute melingkar), keduanya digambar; end di atas start.
- Label kecil "Start" dan "Finish" muncul di dekat marker (sebagai tooltip statis 12 px).
**AC:**
- [ ] Sesi biasa: kedua marker tampil di posisi yang benar.
- [ ] Sesi melingkar: kedua marker tetap tampil (end di atas).
- [ ] Sesi dihentikan di tengah segmen: end di posisi terakhir.

---

## 4. Investigasi

### I1. Rekor 1 km 6:20 tidak ada di split km
**Kondisi:** pemilik sudah memastikan sesi pemilik rekor ada dan tidak ada split km 6:20.
**Penjelasan desain:** rekor dihitung sebagai **bagian tercepat 1 km di mana pun dalam sesi** (sliding window, plan 6.10/F19), bukan split km yang sejajar dengan km 1, 2, 3. Jadi 6:20 bisa terjadi dari window yang melintasi batas km, misalnya dari km 2,3 sampai km 3,3, meski split km sejajar semuanya lebih lambat. Itu **bukan bug** jika perhitungannya benar.
**Langkah wajib:**
1. Dari S10 -> S11, ambil sesi pemilik rekor (id sesi, `startOffsetM`).
2. Muat `TrackPoint` sesi itu di unit test (atau salin DB debug lewat `adb pull`), jalankan `BestEffortCalculator` untuk K1 secara independen, lalu bandingkan dengan `BestEffort` yang tersimpan.
3. Putuskan:
   - **Cocok** (selisih <= 1 s): bukan bug. Terapkan **I1-UI** di bawah.
   - **Tidak cocok:** bug di kalkulator atau data. Perbaiki, hitung ulang `BestEffort`, tambahkan test regresi dengan track itu.
   - **Sesi/rekor berasal dari mock atau sesi yang sudah dihapus:** bagian dari B1 (bersihkan `BestEffort` yatim).
4. Catat hasil di `docs/DECISIONS.md`.
**I1-UI (default, diterapkan bila "Cocok"):**
- S08 tab **Split km**, di bawah footer total: satu baris "**1 km tercepat di sesi ini**" dengan nilai `6:20 · km 2,3–3,3` (angka `Barlow` 24, aksen). Tampil hanya bila sesi punya `BestEffort` K1. Rentang = `startOffsetM` sampai `startOffsetM + 1000`, dua desimal km dengan koma.
- S11 sudah memuat "dari km y" (plan); pastikan nilainya sama.
- S10 tetap memuat catatan "Dihitung dari bagian tercepat dalam sesi."
**AC:**
- [ ] Hasil investigasi tercatat (cocok / tidak cocok / mock).
- [ ] Bila cocok: baris "1 km tercepat di sesi ini" tampil dengan rentang yang benar (lihat `M4`, bagian bawah).
- [ ] Bila tidak cocok: test regresi dengan track bermasalah hijau.

---

## 5. Perubahan perilaku

### P1. Stop di tengah segmen
**Aturan (saat pengguna Stop):**
- Misal `cur` = segmen yang sedang berjalan, dan "sesudahnya" = langkah terencana yang belum dimulai.
- Segmen **sebelum** `cur` tetap dengan statusnya (LULUS/GAGAL/BORDERLINE/TANPA TARGET/DILEWATI/DATA KURANG).
- Untuk `cur` dan semua segmen sesudahnya:
  - punya **target pace** -> status **GAGAL**.
  - **tanpa target** -> tetap **TANPA TARGET** (keputusan final).
- Segmen `Open` (lari bebas setelah rencana selesai) tidak dinilai gagal.
- Bila rencana sudah selesai (sudah di segmen Open): tidak ada segmen yang ditandai gagal.
- Repeat berbasis **sampai jarak**: sisa langkah yang belum diketahui tidak dihitung; hanya `cur` dan sisa anak di iterasi berjalan.
**Data yang disimpan:**
- Segmen `cur`: jarak dan waktu **parsial** yang sebenarnya, `avgPace` dari data parsial (`--:--` bila `Ds < 50 m`).
- Segmen belum dimulai: jarak 0, waktu 0, `avgPace` null.
- `SegmentResult.stopped = true` untuk `cur` dan semua sesudahnya.
- `Session.planCompleted = false`; `segFail` bertambah sesuai jumlah segmen bertarget yang gagal; `segNoTarget` untuk yang tanpa target.
**Tampilan:**
- S06: result strip memakai warna `fail` untuk yang gagal dan `neutral` untuk tanpa target.
- S08 tab Segmen (lihat `M4`): chip GAGAL atau TANPA TARGET, dan baris keterangan `caption` "**Dihentikan sebelum selesai**" pada semua segmen `stopped`. Segmen belum dimulai menampilkan `0 m · 00:00` dan pace `--:--`.
- Header S08: "Dihentikan sebelum selesai" (sudah ada di plan) tetap.
- Statistik mingguan (F21): segmen gagal ini **dihitung** sebagai gagal dalam persentase lulus.
**Contoh (Workout: Pemanasan @≥8:30, Lari 400 @≤6:30, Pemulihan 200 tanpa target, Lari 400 @≤6:30, Pendinginan @≥8:30; Stop saat Lari #2 berjalan 210 m):**
| # | Segmen | Hasil |
|---|---|---|
| 1 | Pemanasan | LULUS (tetap) |
| 2 | Lari 400 | LULUS (tetap) |
| 3 | Pemulihan 200 | TANPA TARGET (tetap) |
| 4 | Lari 400 (berjalan, 210 m) | **GAGAL**, parsial 210 m |
| 5 | Pendinginan | **GAGAL**, 0 m |
**AC:**
- [ ] Test engine untuk contoh di atas, termasuk Stop saat segmen tanpa target berjalan (segmen itu TANPA TARGET, sesudahnya yang bertarget GAGAL).
- [ ] Stop saat sudah di segmen Open: tidak ada tambahan gagal.
- [ ] Stop tepat sesudah segmen terakhir selesai (belum Open): tidak ada gagal.
- [ ] Persentase lulus mingguan memasukkan segmen gagal akibat stop.
- [ ] Tidak ada ucapan saat Stop.

### P2. Warna polyline oranye
- Token baru (lihat bagian 6): `route` **#FF7A1A** (gelap) / **#E5600A** (terang).
- Berlaku di: peta S08 (semua segmen), polyline pada gambar share (F3/F4). Tebal 8 px (4 dp), ujung bulat, **halo** 3 px warna kontras @45% agar terbaca di semua latar.
- Warna oranye **bukan** warna status; jangan dipakai di tempat lain.
**AC:** [ ] polyline di peta dan di PNG memakai token `route`; tidak ada warna polyline lama yang tersisa (grep).

---

## 6. Fitur baru

### F1. Salin struktur workout sebagai teks
**Tujuan:** menempel "ini plan saya" di deskripsi Strava. **Hanya struktur.**
**Lokasi:** S08, tab **Segmen**: tombol teks aksen "**⧉ Salin sebagai teks**" di baris header kanan atas daftar segmen (lihat `M4`, kanan atas), dan item yang sama di menu ⋮ S08. Setelah ditekan: salin ke clipboard dan tampilkan toast "**Disalin**".
**Sumber data:** `workoutSnapshotJson` sesi (rencana, bukan hasil). Termasuk semua segmen apa pun statusnya (lulus, gagal, dilewati, dihentikan). Batas yang dipakai adalah batas **rencana** (bukan yang diperpanjang).
**Format (teks polos, tanpa markdown dan tanpa emoji; baris dipisah `\n`):**
- Baris 1: nama workout (`templateNameSnapshot`).
- Segmen: `{Label} {batas}` lalu `· {target}` bila ada target.
  - Batas jarak: `<1000 m` -> `400 m`; selebihnya km dengan satu atau dua desimal tanpa nol berlebih (`1,5 km`, `5 km`).
  - Batas durasi: `6 mnt`, `2 mnt 30 dtk`, `45 dtk`.
  - Tanpa batas: `tanpa batas`.
  - Target: `≤ 6:30 /km` (batas paling lambat) atau `≥ 8:30 /km`; tanpa target: tidak ada bagian target.
- Repeat berjumlah: `Ulangi 6×:` lalu anak-anaknya menjorok **2 spasi**. Repeat sampai jarak: `Ulangi sampai 5 km:`.
- Pemisah desimal mengikuti bahasa UI (ID koma, EN titik); label segmen apa adanya (yang diisi pengguna).
**Contoh:**
```
Interval 6×400
Pemanasan 1,5 km · ≥ 8:30 /km
Ulangi 6×:
  Lari 400 m · ≤ 6:30 /km
  Pemulihan 200 m
Pendinginan 1 km · ≥ 8:30 /km
```
```
Run/Walk
Ulangi sampai 5 km:
  Lari 6 mnt
  Jalan 2 mnt
```
**AC:**
- [ ] Teks persis sesuai format untuk lima workout contoh (golden test).
- [ ] Tidak mengandung pace aktual, status, atau jarak/waktu hasil.
- [ ] Toast "Disalin" muncul; clipboard berisi teks.
- [ ] Sesi dari Lari Bebas menghasilkan dua baris: `Lari Bebas` dan `Lari bebas tanpa batas` (label segmen + batas).

---

### F2. Urut ulang blok Repeat dan isinya di editor
**Lokasi:** S03 Editor Workout. Lihat `mock/M5-editor-reorder.html`.
**Aturan:**
- Setiap **kartu segmen** (di luar atau di dalam Repeat) dan **header blok Repeat** memiliki menu ⋮ dengan item **Naik, Turun, Duplikat, Hapus** (pola yang sama).
- **Repeat** bergerak **sebagai satu kesatuan** (header + semua anaknya) di antara item tingkat atas.
- **Anak di dalam Repeat** hanya bisa bergeser **di dalam blok itu**. Tombol Naik pada anak pertama dan Turun pada anak terakhir **nonaktif** (teks `#5A646E`, tidak bisa ditekan). Anak tidak bisa dikeluarkan dari blok lewat Naik/Turun.
- Item tingkat atas pertama: Naik nonaktif; terakhir: Turun nonaktif.
- Duplikat Repeat menduplikasi seluruh blok (dan anak-anaknya) tepat di bawahnya. Hapus Repeat menghapus blok beserta anaknya (dengan konfirmasi D09 gaya hapus).
- Setelah memindahkan, validasi dijalankan ulang; aturan `Open` hanya di segmen terakhir tetap berlaku (memindahkan segmen `Open` ke tengah menimbulkan pesan validasi, bukan crash).
- Ringkasan (bar atas) dan SegmentStrip diperbarui seketika.
**AC:**
- [ ] Repeat naik/turun mempertahankan seluruh anaknya dan urutan anak.
- [ ] Anak naik/turun hanya di dalam blok; nonaktif di tepi.
- [ ] Test UI/state: urutan sebelum dan sesudah untuk workout interval (Pemanasan, Repeat[Lari, Pemulihan], Pendinginan).
- [ ] Setelah menyimpan, workout dijalankan dengan urutan yang baru.

---

### F3. Share PNG, dan F4. Simpan sebagai gambar
**Lokasi:** S08 ⋮ -> **Bagikan** membuka halaman **"Bagikan sesi"** (`mock/M1-share-picker.html`). Hasil gambar ada di `mock/M2-share-output.html`.

**Alur satu halaman, tiga langkah:**
1. **Pilih data** (centang, bisa kosong sebagian, minimal satu):
   | Item | Isi | Default |
   |---|---|---|
   | Nama sesi, tanggal & jam | nama sesi, tanggal, jam mulai | dicentang |
   | Jarak | km 2 desimal | dicentang |
   | Waktu | waktu berjalan | dicentang |
   | Pace rata-rata | `m:ss /km` | dicentang |
   | Split km | batang pace per km (sama dengan CH2 S08) | tidak |
   | Hasil segmen (hanya yang lulus) | bar chart segmen **LULUS** (F5) | dicentang bila ada segmen lulus; **dinonaktifkan** dengan keterangan "Tidak ada segmen lulus" bila tidak ada |
   | Polyline rute | garis rute tanpa peta dasar | dicentang bila ada >= 2 titik GPS, selain itu dinonaktifkan |
2. **Pilih template** (tiga kartu, terpilih diberi border aksen):
   | Template | Kanvas | Blok yang didukung |
   |---|---|---|
   | **Ringkas** | 1080 x 1080 px | nama/tanggal, jarak, waktu, pace, polyline |
   | **Split** | 1080 x 1350 px | nama/tanggal, jarak, waktu, pace (baris kecil), split km, hasil segmen |
   | **Lengkap** | 1080 x 1920 px | semua yang dicentang |
   Bila ada item dicentang yang tidak didukung template terpilih, item itu **tampil abu-abu** dengan keterangan "Tidak ditampilkan di template ini" (status centang dipertahankan). (Mock M1 tidak menggambar state abu-abu; aturan teks ini yang berlaku.)
3. **Warna teks:** kontrol segmen **Putih | Hitam** (default Putih). Di bawahnya **pratinjau** di atas latar kotak-kotak (checkerboard) yang menunjukkan transparansi.
Tombol bawah (menempel): **Bagikan** (utama) dan **Simpan sebagai gambar** (sekunder).

**Aturan render (semua PNG):**
- Bitmap `ARGB_8888`, **latar transparan penuh** (tanpa warna latar apa pun).
- **Warna teks Putih:** isi `#FFFFFF`, kontur 6 px `#000000` @45%. **Hitam:** isi `#000000`, kontur 6 px `#FFFFFF` @45%.
- Font: Barlow Condensed 800 (angka, judul, label), Manrope (sisa) yang sudah dibundel.
- Padding kanvas 72 px. Label (`LABEL`) 36 px kapital, spacing .14em. Angka mengikuti skala mock (`M2`).
- **Layout Ringkas:** nama + tanggal (label 40 px + nama 64 px); jarak (angka 220 px + "km" 64 px); baris waktu dan pace (label 36 px, nilai 96 px); polyline mengisi area sisa (kotak 936 x 448 px, rasio terjaga). Blok yang tidak dicentang dibuang, sisanya naik rapat dari atas.
- **Layout Split:** nama/tanggal; baris jarak/waktu/pace (nilai 72 px); bagan split km (tinggi 300 px) bila dicentang; bagan hasil segmen (tinggi 300 px) bila dicentang.
- **Layout Lengkap:** urutan tetap: nama/tanggal, jarak, waktu + pace, polyline (tinggi 520 px), split km (300 px), hasil segmen (300 px); blok yang tidak dicentang dibuang dan ruang dibagi rata. Bila konten melebihi 1920 px: perkecil polyline sampai minimum 300 px, bagan sampai minimum 220 px, lalu kurangi jarak antar blok.
- **Polyline:** dari `TrackPoint`, disederhanakan (Douglas-Peucker 3 m, maksimal 500 titik), digambar per segmen (F6) dengan warna `route`, tebal 8 px, halo 3 px kontras @45%, titik batas segmen 10 px (putih, cincin `route`), marker start dan end seperti B4. Tanpa peta dasar.
- **Bagan batang (F5 dan split km):** tinggi batang **sebanding kecepatan** (1/pace), lebih tinggi = lebih cepat; label pace di atas, penanda di bawah (`#n` untuk segmen, `1,2,3` untuk km, km parsial berlabel jaraknya). Isi batang `#C6F432`, kontur 4 px kontras @45%. Untuk hasil segmen, **hanya segmen LULUS**; garis target pendek (4 px, selebar batang) di level target pace bila segmen bertarget. Maksimal 24 batang dalam satu baris; bila lebih, label diperjarang (tiap 2 batang).
- Batang segmen memakai **nomor segmen pada rencana lengkap** (mis. `#3`), bukan nomor urut di antara yang lulus.

**Bagikan (F3):** simpan ke cache `shared_images/` lalu `Intent.ACTION_SEND` (`image/png`) lewat `FileProvider`, chooser berjudul "Bagikan". Hapus berkas cache lama saat app dibuka.
**Simpan sebagai gambar (F4):** simpan lewat `MediaStore` ke `Pictures/PacerNih/PacerNih_{yyyyMMdd_HHmm}.png`; API 29+ memakai `RELATIVE_PATH`, API 26-28 meminta `WRITE_EXTERNAL_STORAGE` saat itu juga. Toast "**Tersimpan di galeri**"; bila gagal: Banner `fail` dengan pesan penyebab.
**Catatan:** beberapa aplikasi (mis. Instagram) menampilkan latar transparan sebagai hitam; itu perilaku aplikasi penerima, bukan bug.

**AC:**
- [ ] Hasil PNG memiliki kanal alfa; piksel latar memiliki alfa 0 (test membaca piksel pojok).
- [ ] Ukuran kanvas tepat 1080x1080, 1080x1350, 1080x1920 sesuai template.
- [ ] Hanya blok yang dicentang yang tergambar; blok tak didukung template tidak tergambar dan tampil abu-abu di pemilih.
- [ ] Bagan hasil segmen hanya memuat segmen LULUS; BORDERLINE, GAGAL, DILEWATI, TANPA TARGET tidak ada.
- [ ] Warna teks Putih/Hitam sesuai, dengan kontur.
- [ ] Polyline berwarna `route`, titik batas segmen, marker start/end tampak.
- [ ] Share membuka chooser; Simpan menaruh berkas di galeri dan toast muncul.
- [ ] Preview di halaman memakai checkerboard dan tidak menambahkan latar ke PNG.
- [ ] Sesi 3,1 km: split km menampilkan 4 batang dengan batang terakhir berlabel `0,1`.

---

### F5. Bar chart segmen yang lulus
Bagian dari F3/F4 (blok "Hasil segmen (hanya yang lulus)"). Aturan di F3. **AC tambahan:** bagan terbaca tanpa data lain (hanya blok ini dicentang menghasilkan gambar valid); bila tidak ada segmen lulus, blok dinonaktifkan di pemilih.

---

### F6. Peta per segmen, tampil sebagai satu rute
**Lokasi:** S08 tab Peta (`mock/M3-map-segments.html`).
**Rincian:**
- Rute digambar sebagai **polyline terpisah per segmen** (data: `TrackPoint.segmentIdx`) dengan warna `route`. Titik batas antar segmen dibagikan (titik interpolasi di batas), jadi **tidak ada celah** antar segmen.
- Skema: tambahkan ke `SegmentResult` kolom `startLat`, `startLon`, `endLat`, `endLon` (titik batas hasil interpolasi F6 saat sesi disimpan; null untuk segmen belum dimulai). Migrasi Room tertulis dan dites; dilarang `fallbackToDestructiveMigration`.
- Titik kecil di setiap batas antar segmen: lingkaran 9 px, isi `#FFFFFF`, cincin 2 px `route`.
- Tampilan awal: semua segmen oranye penuh sehingga terlihat satu rute utuh.
- Segmen yang dilewati atau belum dimulai (P1) tidak punya polyline.
**AC:**
- [ ] Jumlah polyline = jumlah segmen yang punya titik; ujung segmen n sama dengan awal n+1.
- [ ] Titik batas tampil di sambungan antar segmen; tanpa celah visual.
- [ ] Rute pada tab Peta sama bentuknya dengan rute gabungan (tidak ada titik hilang).

### F7. (Opsional) Tooltip segmen di peta
- Ketuk polyline suatu segmen (area sentuh +24 dp di sekitar garis): segmen itu **disorot** (tebal 12 px, penuh), segmen lain diredupkan (opacity 40%), dan muncul **tooltip** kartu `surfaceRaised` radius 12: judul `#{n} {label}`, baris "**Jarak {m} · Waktu {mm:ss}**" (dari `SegmentResult`: jarak dan durasi aktual).
- Ketuk area kosong atau segmen yang sama: sorotan dan tooltip hilang. Hanya satu tooltip pada satu waktu.
- Segmen tanpa polyline tidak bisa diketuk.
**AC:** [ ] tooltip menampilkan jarak dan waktu yang sama dengan kartu segmen di tab Segmen; [ ] sorot dan redup berfungsi; [ ] ketuk ulang menutup.

---

## 7. Token baru

| Token | Gelap | Terang | Pakai |
|---|---|---|---|
| `route` | #FF7A1A | #E5600A | Polyline rute (peta dan PNG) |
| `routeHalo` | kontras @45% | kontras @45% | Halo polyline di PNG |
| `markerStart` | #34D399 | #0B7A55 | Marker start |
| `markerEnd` / `markerEndCore` | #F2F5F7 / #0B0D0F | #0E1114 / #FFFFFF | Marker end |
| `boundaryDot` | #FFFFFF (cincin `route`) | #FFFFFF (cincin `route`) | Titik batas segmen |
| `menuDisabled` | #5A646E | #A3ADB7 | Item menu nonaktif (F2) |
Semua lewat `RcColors`. Uji kontras otomatis diperluas ke pasangan baru.

## 8. Perubahan data dan string

**Skema (migrasi eksplisit, tanpa FK):**
- `SegmentResult`: tambah `stopped` (boolean), `startLat`, `startLon`, `endLat`, `endLon` (nullable). `avgPace` boleh null hanya bila `Ds < 50 m`.
- Tidak ada perubahan `Session` selain nilai `planCompleted`/`seg*` (P1).
- Flag `debug_mock_seeded` di DataStore **terpisah** (`debug_flags`), hanya debug.

**String baru (ID / EN):**
| Kunci | ID | EN |
|---|---|---|
| copy_text | Salin sebagai teks | Copy as text |
| copied | Disalin | Copied |
| share | Bagikan | Share |
| share_title | Bagikan sesi | Share session |
| share_pick_data | Pilih data | Choose data |
| share_pick_template | Pilih template | Choose template |
| share_text_color | Warna teks | Text color |
| share_white / share_black | Putih / Hitam | White / Black |
| save_image | Simpan sebagai gambar | Save as image |
| saved_gallery | Tersimpan di galeri | Saved to gallery |
| tpl_compact / tpl_split / tpl_full | Ringkas / Split / Lengkap | Compact / Split / Full |
| tpl_unsupported | Tidak ditampilkan di template ini | Not shown in this template |
| no_passed_segments | Tidak ada segmen lulus | No passed segments |
| stopped_early | Dihentikan sebelum selesai | Stopped before finishing |
| map_start / map_finish | Start / Finish | Start / Finish |
| fastest_km | 1 km tercepat di sesi ini | Fastest 1 km in this session |
| seed_mock / clear_mock | Isi mock data / Hapus mock data | Fill mock data / Clear mock data |
| move_up / move_down | Naik / Turun | Move up / Move down |

## 9. Urutan pengerjaan
1. **B1** (fondasi data bersih; I1 bergantung padanya).
2. **I1** (investigasi, cepat) dan **B2** (paling besar; mulai dengan reproduksi di S15).
3. **P1** (aturan stop) lalu **B3** (pace aktual) karena menyentuh `SegmentResult` yang sama.
4. **Skema `SegmentResult`** (kolom batas) -> **F6**, **B4**, **P2**, **F7**.
5. **F2** (editor), **F1** (salin teks).
6. **F3/F4/F5** (share dan gambar), terakhir karena memakai data/peta dari langkah sebelumnya.
Satu commit per item. Perbarui `docs/PROGRESS.md` setelah tiap item dengan hasil AC.

## 10. Pengujian dan checklist lapangan
- Semua AC di atas menjadi test otomatis bila memungkinkan; sisanya lewat uji manual di perangkat.
- **Uji lapangan oleh pemilik (setelah B1-B2):**
  1. Release bersih: tidak ada mock data sama sekali.
  2. Run/walk berbasis menit 20 menit: dengarkan A01-A07, A11, A13 (layar mati, musik di-duck).
  3. Stable run 3 km dengan target: A08/A09/A10 muncul saat melambat/ngebut.
  4. Stop di tengah segmen: lihat hasil di S06 dan S08 (P1).
  5. Share Lengkap dengan beberapa data tercentang, simpan ke galeri, tempel ke gambar lain (transparan).
  6. Peta: start dan end tampak, rute oranye per segmen, ketuk segmen memunculkan tooltip.
- **DoD tampilan:** layar baru/berubah dibandingkan dengan mock; screenshot sebelum/sesudah di `docs/PROGRESS.md`; tema terang diturunkan dari token terang.

## 11. Di luar lingkup
- N1 (pace rata-rata sejak mulai di layar live): **dibatalkan**.
- Rekor/skala capek/catatan/jumlah segmen lulus pada gambar share: tidak ada.
- Peta dasar (tile) pada gambar share: tidak ada, hanya polyline.
