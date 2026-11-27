# Panduan Penggunaan C.O CALCULATOR SC-100

Panduan lengkap tombol, fitur, dan contoh perhitungan. Notasi yang dipakai:

| Notasi | Arti |
|---|---|
| `2 + 3 =` | ketuk tombol angka 2, tombol +, tombol 3, lalu tombol = |
| `SHIFT` + `7` | ketuk SHIFT, lalu ketuk 7 |
| `( )` | tombol kurung buka dan kurung tutup |
| `→` | hasil yang tampil di layar LCD |

Di dalam aplikasi, legenda kuning kecil di dalam tombol adalah fungsi sekunder. Tekan `SHIFT`
lalu tombolnya, atau **tahan** tombol tersebut sekitar setengah detik.

---

## 1. Mengenal tampilan

| Bagian | Keterangan |
|---|---|
| **LCD hijau** | Menampilkan ekspresi di baris atas dan hasil di baris utama setelah `=` ditekan. Baris ekspresi kini berukuran dua kali lebih besar agar mudah dibaca. |
| **Baris utilitas** | `MENU` pengaturan, `DRG▸` ganti sudut, `TAB` jumlah digit, `RCL` buka memori, `STO` simpan memori, `M−` kurangi memori. |
| **Blok fungsi** | `SHIFT`, `DRG`, `FSE`, `MR`, `MS`, `M+`; `hyp`, `sin`, `cos`, `tan`, `ln`, `log`; `yˣ`, `√`, `x²`, `%`, `(`, `)`. |
| **Blok angka** | `7 8 9 DEL AC`, `4 5 6 × ÷`, `1 2 3 + −`, `0 ± . EXP =`. |
| **Legenda kuning** | Fungsi alternatif: `n!`, `nCr`, `nPr`, `DEC`, `HEX`, `π`, `e`, `OCT`, `BIN`, `CONV`, `CNST`, `MOD`, `ANS`, `sin⁻¹`, `cos⁻¹`, `tan⁻¹`, `eˣ`, `10ˣ`, `x³`, `1/x`, `∛`. |

Aplikasi mendukung **portrait** dan **landscape**. Dalam landscape, tombol `MENU`
membuka pengaturan.

---

## 2. Perhitungan dasar

### Kapan hasil muncul?

Saat mengetik, LCD utama menampilkan angka yang sedang dimasukkan. Tekan `=` untuk
menghitung. Untuk fungsi, tekan fungsinya dulu, lalu masukkan angka:

- `sin` `30` `=` → **0.5**
- `√` `144` `=` → **12**
- `12` `x²` `=` → **144**
- `10` `SHIFT` `7` `=` → **3,628,800**

Aplikasi otomatis menutup kurung fungsi saat `=` ditekan, jadi `sin` `30` `=` sama dengan
`sin(30)`. Urutan ini juga berlaku untuk `cos`, `tan`, `ln`, `log`, `√`, `∛`, dan fungsi
lainnya.

Untuk ekspresi bercampur, gunakan `=` setelah fungsi: `2 + 3 + 5 + sin 30 =` → **10.5**.

### Angka dan operator

Contoh:

- `2 + 3 × 4 =` → **14** (perkalian didahulukan)
- `( 2 + 3 ) × 4 =` → **20**
- `12 ÷ 4 + 1 =` → **4**
- `− 5 + 2 =` → **−3** (`−` adalah tombol kurang; untuk angka negatif gunakan `±`)
- `2 ( 3 + 4 ) =` → **14** (perkalian implisit)
- `2 π =` → **6.28318530718**

### Menghapus dan membatalkan

- `DEL` menghapus satu karakter terakhir.
- `AC` menghapus seluruh perhitungan (riwayat dan memori tetap tersimpan).
- `±` mengubah tanda angka terakhir, misalnya `5 ±` → `−5`.
- `EXP` memasukkan notasi ilmiah: `2 EXP 12 =` → **2 × 10¹²**.

### Persen

Persen bersifat kontekstual, seperti kalkulator saku:

- `200 + 10 % =` → **220** (10% dari 200 ditambahkan)
- `200 − 10 % =` → **180**
- `200 × 10 % =` → **20**
- `50 % =` → **0.5**

---

## 3. Tombol SHIFT dan fungsi sekunder

Tekan `SHIFT` sekali; indikator `2nd` muncul. Tekan tombol tujuan. Setelah satu fungsi
sekunder dijalankan, mode SHIFT otomatis mati. Tekan `SHIFT` lagi untuk membatalkan.

Cara lain: **tahan** tombol yang punya legenda kuning.

| Tombol | Fungsi normal | `SHIFT` + tombol |
|---|---|---|
| `sin` | sin | `sin⁻¹` (arcsin) |
| `cos` | cos | `cos⁻¹` (arccos) |
| `tan` | tan | `tan⁻¹` (arctan) |
| `ln` | logaritma natural | `eˣ` |
| `log` | logaritma basis 10 | `10ˣ` |
| `√` | akar kuadrat | `∛` (akar kubik) |
| `x²` | kuadrat | `x³` (kubik) |
| `%` | persen | `1/x` (kebalikan) |
| `7` | angka 7 | `n!` (faktorial) |
| `8` | angka 8 | `nCr` (kombinasi) |
| `9` | angka 9 | `nPr` (permutasi) |
| `4` | angka 4 | `π` |
| `5` | angka 5 | `e` |
| `6` | angka 6 | buka tampilan radix (OCT) |
| `DEL` | hapus | buka tampilan radix (DEC) |
| `AC` | hapus semua | buka tampilan radix (HEX) |
| `×` | kali | buka tampilan radix (BIN) |
| `1` | angka 1 | buka konverter satuan (`CONV`) |
| `2` | angka 2 | buka konstanta (`CNST`) |
| `3` | angka 3 | sisipkan `mod` |
| `=` | hasil | buka riwayat hasil (`ANS`) |

### HYP

- `HYP` lalu `sin` → `sinh`
- `HYP` lalu `cos` → `cosh`
- `HYP` lalu `tan` → `tanh`

Untuk invers hiperbolik: nyalakan `HYP`, lalu `SHIFT`, lalu fungsi trigonometri.

Contoh:

- `HYP` `sin` `4` `)` `=` → **27.2899171971**
- `HYP` `SHIFT` `sin` `9` `)` `=` → **2.8934439865** (`asinh(9)`)

---

## 4. Sudut: DEG, RAD, GRAD

`DRG` mengganti mode sudut. Status di LCD berubah:

- **DEG** — derajat
- **RAD** — radian
- **GRAD** — gradian

Contoh (fungsi dulu, lalu angka):

- DEG: `sin` `30` `=` → **0.5**
- DEG: `cos` `60` `=` → **0.5**
- DEG: `tan` `45` `=` → **1**
- RAD: `sin` `π` `÷` `2` `=` → **1**
- DEG: `SHIFT` `sin` `0.5` `=` → **30** (arcsin)
- GRAD: pilih **GRAD**, lalu `sin` `100` `=` → **1**

Kurung tutup tidak perlu diketik; aplikasi menutupnya otomatis saat `=`.

---

## 5. Pangkat, akar, faktorial, kombinasi

| Perhitungan | Urutan tombol | Hasil |
|---|---|---|
| 2 pangkat 10 | `2` `yˣ` `10` `=` | 1024 |
| Akar 144 | `√` `144` `=` | 12 |
| Akar kubik 27 | `SHIFT` `√` `27` `=` | 3 |
| 12 kuadrat | `12` `x²` `=` | 144 |
| 5 kubik | `5` `SHIFT` `x²` `=` | 125 |
| 4 faktorial | `4` `SHIFT` `7` `=` | 24 |
| 10 faktorial | `10` `SHIFT` `7` `=` | 3,628,800 |
| 5 kombinasi 3 | `5` `SHIFT` `8` `3` `=` | 10 |
| 5 permutasi 3 | `5` `SHIFT` `9` `3` `=` | 60 |
| 17 mod 5 | `17` `SHIFT` `3` `5` `=` | 2 |
| 1/4 | `4` `SHIFT` `%` `=` | 0.25 |

Catatan: `n!`, `nCr`, dan `nPr` membutuhkan bilangan bulat. `mod` juga menerima desimal;
hasilnya mengikuti tanda pembagi, contoh `−8 mod 5 = 2` dan `8 mod −5 = −2`.

---

## 6. Logaritma, eksponensial, dan konstanta

| Perhitungan | Urutan tombol | Hasil |
|---|---|---|
| ln(e) | `ln` `SHIFT` `5` `=` | 1 |
| log(1000) | `log` `1000` `=` | 3 |
| e² | `SHIFT` `ln` `2` `=` | 7.38905609893 |
| 10² | `SHIFT` `log` `2` `=` | 100 |
| 2π | `2` `SHIFT` `4` `=` | 6.28318530718 |
| φ (rasio emas) | masukkan lewat `CNST` → MATHEMATICS | 1.61803398875 |

Konstanta lengkap:

- **MATHEMATICS:** π, e, φ, √2, ln 2, ln 10.
- **PHYSICS:** c, G, h, ħ, k, Nₐ, R, e⁻, mₑ, mₚ, ε₀, μ₀, g, atm, R∞, σ.

Buka `SHIFT` + `2`, pilih tab, lalu ketuk konstanta untuk memasukkannya.

---

## 7. Format angka: AUTO, FIX, SCI, ENG

`FSE` mengganti format. `TAB` mengganti jumlah digit (2, 3, 4, 5, 6, 8, 10).

| Mode | Arti | Contoh `1 ÷ 3` |
|---|---|---|
| **AUTO** | otomatis, 12 digit signifikan | 0.333333333333 |
| **FIX 4** | 4 angka di belakang koma | 0.3333 |
| **SCI 6** | notasi ilmiah, 6 digit | 3.33333E−1 |
| **ENG 6** | eksponen kelipatan 3 | 333.333E−3 |

Pemisah ribuan bisa dimatikan di pengaturan. Di layar seven-segment, pemisah tampil sebagai
spasi tipis.

---

## 8. Memori

Ada 10 slot: `M` (utama) dan `1`–`9`.

| Tombol | Fungsi |
|---|---|
| `MS` | simpan nilai tampil ke memori utama |
| `MR` | ambil nilai memori utama |
| `M+` | tambahkan nilai tampil ke memori utama |
| `M−` | kurangi memori utama dengan nilai tampil |
| `RCL` | buka dialog memori untuk mengambil slot |
| `STO` | buka dialog memori untuk menyimpan slot |
| `M−` di baris utilitas | sama dengan mengurangi memori utama |

Contoh:

1. `5` `MS` → menyimpan 5.
2. `AC` lalu `MR` → menampilkan 5.
3. `3` `M+` → memori menjadi 8.
4. `RCL`, pilih kartu `3` → memasukkan nilai slot 3 ke perhitungan.
5. `STO`, pilih kartu `2` → menyimpan nilai tampil ke slot 2.

Huruf **M** di LCD menandakan memori utama berisi nilai bukan nol. Tombol
`CLEAR ALL MEMORIES` menghapus semua slot.

---

## 9. Riwayat hasil

Setiap penekanan `=` menyimpan hasil ke riwayat (maksimal 50).

- Buka riwayat: `SHIFT` + `=`, atau ketuk legenda `ANS`.
- Ketuk baris untuk memasukkan nilainya kembali ke perhitungan.
- `CLEAR ALL` menghapus riwayat.
- Tahan dan geser daftar untuk menggulir.

Contoh:

1. `2 + 3 =` → 5.
2. `7 × 6 =` → 42.
3. `SHIFT` + `=` membuka riwayat; ketuk baris `5`; nilai 5 masuk ke ekspresi.

---

## 10. Konverter satuan

Buka dengan `SHIFT` + `1` (legenda `CONV`).

1. Pilih kategori: **LENGTH, MASS, TEMP, AREA, VOLUME, SPEED, TIME, DATA, ENERGY,
   PRESSURE**.
2. Nilai awal diambil dari angka yang sedang tampil. Jika tampil 0, aplikasi memakai 1
   sebagai kemudahan.
3. Di daftar satuan:
   - ketuk **nama satuan** untuk menjadikannya satuan asal;
   - ketuk **nilai** di kanan untuk menjadikannya satuan tujuan;
   - tombol `⇅` menukar asal dan tujuan.
4. Kartu **FROM** dan **TO** menampilkan hasil langsung.

Contoh:

- `1` lalu `SHIFT` + `1`; kategori LENGTH; dari **metre** ke **foot** → **3.28084**.
- Pilih **kilometre** sebagai asal, **mile** sebagai tujuan → 1 km ≈ **0.6213712 mi**.
- Kategori TEMP: 100 °C → **212 °F**; 32 °F → **0 °C**; 0 °C → **273.15 K**.
- Kategori DATA: 1 GB → **0.9313226 GiB** (GB desimal vs GiB biner).

Satuan yang tersedia mencakup panjang (12), massa (8), suhu (3), luas (9), volume (10),
kecepatan (6), waktu (7), data (10), energi (8), dan tekanan (6).

---

## 11. Radix: DEC, HEX, OCT, BIN

Buka tampilan radix dengan salah satu:

- `SHIFT` + `DEL` → mode DEC
- `SHIFT` + `AC` → mode HEX
- `SHIFT` + `6` → mode OCT
- `SHIFT` + `×` → mode BIN

Tampilan menunjukkan nilai saat ini dalam keempat basis. Ketuk `INSERT DECIMAL VALUE` untuk
memasukkan nilai desimalnya ke perhitungan.

Contoh: hitung `255`, lalu `SHIFT` + `AC`:

- DEC **255**
- HEX **FF**
- OCT **377**
- BIN **11111111**

> Pada versi ini radix adalah penampil konversi; aritmetika utama tetap desimal.

---

## 12. Pengaturan

Buka dengan tombol `MENU` di baris utilitas, baik portrait maupun landscape.

| Baris | Pilihan |
|---|---|
| **Bahasa / Language** | INDONESIA / ENGLISH — default INDONESIA; hanya mengubah bahasa menu Pengaturan |
| **Tema** | GRAFIT, PERAK, TENGAH MALAM |
| **Mode sudut** | DERAJAT, RADIAN, GRADIAN |
| **Format angka** | AUTO, FIX, SCI, ENG |
| **Tanda desimal** | KOMA (`0,001`) atau TITIK (`0.001`) — default KOMA |
| **Jumlah digit** | 2, 3, 4, 5, 6, 8, 10 |
| **Pemisah ribuan** | AKTIF/NONAKTIF |
| **Getar** | AKTIF/NONAKTIF — default AKTIF |
| **Kekuatan getar** | 20%, 40%, 60%, 80%, 100% — default 100% |
| **Tes getar** | ketuk untuk mengirim pola getar 1,5 detik |
| **Suara tombol** | AKTIF/NONAKTIF — default AKTIF |
| **Volume klik** | 10%, 30%, 50%, 70%, 100% — default 30% |
| **Layar tetap menyala** | AKTIF/NONAKTIF |
| **Reset alat** | mengembalikan tema, sudut, format, digit, pemisah ribuan, getar, kekuatan getar, suara, volume, dan data memori/riwayat ke awal |
| **Tentang C.O CALCULATOR** | info versi |

Perubahan langsung diterapkan dan disimpan. Tema juga mengubah warna body, tombol, dan LCD.

**Getar dan suara:** keduanya aktif secara default. Saat Getar diubah dari NONAKTIF ke AKTIF,
aplikasi langsung bergetar 300 ms sebagai tes. Begitu pula saat Suara tombol diubah NONAKTIF ke AKTIF,
aplikasi langsung memutar bunyi klik. Jika getar tetap tidak terasa, periksa pengaturan getar
sistem dan pastikan perangkat memiliki motor getar. Jika suara tidak terdengar, naikkan volume
media; suara tombol diputar melalui SoundPool dan tidak bergantung pada pengaturan
“touch sounds” sistem.

---

## 13. Portrait dan landscape

- Putar perangkat; aplikasi menyesuaikan sendiri.
- Portrait: 6 kolom fungsi dan 5 kolom angka.
- Landscape tablet 1920 × 1200: dua panel; tombol fungsi di kiri dan blok angka 5 kolom di kanan. Angka 1–9 tetap tersusun **3×3**, dengan `0`, `.`, dan `=` di bawahnya.
- Di landscape, `MENU` membuka pengaturan; `CONV`, `CNST`, `MOD`, `ANS`, `STO`, `DEC`,
  `HEX`, `OCT`, `BIN` tetap tersedia lewat `SHIFT` atau tombol utilitas.

---

## 14. Pesan error

| Pesan | Penyebab | Solusi |
|---|---|---|
| `Error` | ekspresi belum lengkap atau tidak valid | periksa kurung dan operator, tekan `AC` |
| `Division by zero` | pembagian dengan 0 | ubah pembagi |
| `Logarithm domain` | log/ln dari 0 atau negatif | gunakan bilangan positif |
| `Square root domain` | akar kuadrat bilangan negatif | gunakan bilangan ≥ 0 |
| `Factorial ...` | faktorial negatif/pecahan/terlalu besar | gunakan bilangan bulat 0–170 |
| `Combination domain` | `nCr`/`nPr` dengan `k > n` atau negatif | pastikan 0 ≤ k ≤ n |
| `Result is not a number` | hasil di luar jangkauan | periksa input |

---

## 15. Contoh lengkap dalam satu alur

Hitung `sin(30) + √144 × 2`:

1. `sin` `30` `)` → ekspresi `sin(30)`.
2. `+` → `+`.
3. `√` `144` `)` → `√(144)`.
4. `×` `2` → `× 2`.
5. `=` → **24.5** (sin 30 = 0.5; √144 = 12; 0.5 + 12 × 2 = 24.5).
6. Buka riwayat `SHIFT` + `=` untuk melihat hasil.

Contoh lain yang sudah diuji:

- `sin(30)` DEG → **0.5**
- `1 metre` → **3.28084 foot**
- `200 + 10%` → **220**
- `10!` → **3,628,800**
- `17 mod 5` → **2**

---

## 16. Batasan versi ini

- Belum ada mode **RPN**.
- Belum ada pecahan, derajat–menit–detik, dan widget homescreen.
- Radix hanya penampil konversi, belum aritmetika per basis.
- Belum ada copy/paste dari LCD.
- Build ini memakai **debug signing**; untuk distribusi Play Store perlu keystore rilis.

---

## 17. Berkas terkait

- [README](../README.md)
- [Contoh penggunaan setiap fitur](CONTOH-PENGGUNAAN.md)
- [Desain FigCraft](../design/cokalku-sc100.figcraft)
- [Screenshot aplikasi](../design/app)
- [Build script](../app/build.ps1)






















