# Glass Launcher

Launcher Android bergaya iOS "liquid glass": ikon squircle, dock kaca, status bar ala iPhone,
widget (jam analog, baterai, penyimpanan, kalender, saran, catatan), App Library, dan
**folder otomatis** (Game, Foto & Video, Google, Sosial, dst).

## Cara build lewat GitHub (tanpa Android Studio)

1. Buat repository baru di GitHub (misalnya `GlassLauncher`).
2. Upload **semua isi** folder ini (termasuk folder tersembunyi `.github`) ke repository.
   - Cara mudah: di halaman repo klik **Add file -> Upload files**, lalu drag seluruh isi folder.
     Pastikan `.github/workflows/build-apk.yml` ikut ter-upload.
3. Buka tab **Actions** -> workflow **Build APK** akan jalan otomatis (sekitar 5-8 menit).
   Kalau tidak jalan, klik **Build APK -> Run workflow**.
4. Setelah hijau, buka run tersebut -> bagian **Artifacts** -> unduh **GlassLauncher-APK**
   (isinya `GlassLauncher.apk`).
5. Pindahkan APK ke HP, install (izinkan "Install dari sumber tidak dikenal").
6. Tekan tombol Home -> pilih **Glass Launcher** -> **Selalu**.

Opsional: buat tag `v1.0` untuk menerbitkan APK otomatis di tab **Releases**.

## Fitur

- Geser kiri: Today View (widget kaca). Tengah: Home. Kanan: App Library.
- Tekan lama area kosong: pengaturan (wallpaper, auto-sort folder, status bar kustom).
- Tekan lama ikon: tambah/hapus Dock, sembunyikan, info, uninstall.
- Geser ke bawah di Home: pencarian.
- Auto-sort: aplikasi terdeteksi otomatis berdasarkan nama paket (game, kamera/galeri, Google, sosial, dll).

## Catatan

- Efek kaca memakai gradasi transparan + blur saat overlay. Android tidak mengizinkan launcher
  mem-blur wallpaper sistem, jadi wallpaper bawaan launcher digambar sendiri (5 pilihan).
- Status bar atas adalah tampilan kustom; status bar asli disembunyikan, geser dari atas untuk menampilkannya (notifikasi).
- Ikon iPhone/Apple asli tidak dipakai (hak cipta); ikon aplikasi Anda sendiri dipotong bentuk squircle.
