# COKALKU — responsivitas layar

COKALKU menggambar seluruh antarmuka di atas `Canvas`, jadi ukuran tidak bergantung pada
layout XML. Geometri memakai kanvas virtual **412 × 915** (portrait) dan **915 × 412**
(landscape), lalu dipetakan ke ukuran layar sebenarnya.

## Rumus yang dipakai

```
SX = lebar layar  / 412   (portrait)   atau lebar  / 915   (landscape)
SY = tinggi layar / 915   (portrait)   atau tinggi / 412   (landscape)

S  = min(SX, SY × 1.12)   (portrait)
S  = min(SY, SX × 1.18)   (landscape)
```

- **Posisi dan lebar** tombol memakai `SX`.
- **Posisi dan tinggi** tombol memakai `SY`.
- **Teks, sudut, bayangan, dan ketebalan** memakai `S`.
- Faktor 1.12/1.18 membuat teks tidak terlalu kecil di layar pendek (misalnya 16:9),
  sementara layar 20:9 tetap memakai skala asli.
- Overlay (riwayat, konverter, konstanta, pengaturan, memori) memakai koordinat piksel
  relatif terhadap lebar/tinggi layar, sehingga selalu berada di dalam layar.

## Matriks pengujian

Diuji langsung pada emulator Android 11 dengan mengubah `wm size`/`wm density`:

| Resolusi | Orientasi | Hasil |
|---|---|---|
| 480 × 800 | portrait | semua tombol, legend, dan footer masuk; tidak ada clipping |
| 1080 × 1920 | portrait | layout 16:9 pas, teks diperbesar otomatis |
| 1080 × 2400 | portrait | layout utama 20:9 |
| 1600 × 2560 | portrait tablet | tombol melebar, teks tetap terbaca |
| 800 × 480 | landscape | 10 kolom × 5 baris muat |
| 1920 × 1200 | landscape tablet | layout lebar, tidak ada clipping |

Overlay juga diuji pada 480 × 800: panel pengaturan dan konverter tetap berada di dalam
layar dan bisa digunakan.

## Perilaku saat rotasi

`MainActivity` memakai `configChanges` untuk orientasi dan ukuran layar. Saat perangkat
diputar, `CalculatorView.onSizeChanged()` menghitung ulang `SX`, `SY`, `S`, posisi tombol,
dan bitmap chrome — tanpa membuat ulang proses atau kehilangan perhitungan.

## Batas praktis

- Lebar minimum yang nyaman: sekitar **320 dp**.
- Layout landscape aktif saat lebar > tinggi × 1,2.
- Di layar sangat kecil, teks overlay memang kecil, tetapi tidak terpotong; pengaturan
  tetap dapat dibaca dan disentuh.
