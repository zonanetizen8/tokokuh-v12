# Build TOKOKUH di GitHub Codespaces

## Toolchain yang dipakai
- JDK 17 (wajib untuk konfigurasi Android Gradle Plugin 9.1.1)
- Gradle 9.3.1
- Android SDK Platform 36
- Android Build Tools 36.0.0
- Kotlin Gradle Plugin 2.2.10
- KSP 2.3.10

## 1. Siapkan JDK dan Android SDK

Di terminal Codespaces (Ubuntu):

```bash
sudo apt-get update
sudo apt-get install -y openjdk-17-jdk unzip wget
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

Jika Android SDK belum tersedia, install command-line tools dari dokumentasi Android resmi, lalu set:

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"
```

Install paket SDK:

```bash
sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"
```

## 2. Pasang Gradle 9.3.1

Jika perintah `gradle --version` belum tersedia, pasang Gradle 9.3.1 melalui SDKMAN:

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install gradle 9.3.1
```

Verifikasi:

```bash
gradle --version
```

## 3. Generate Gradle Wrapper resmi satu kali

ZIP dari AI Studio tidak menyertakan `gradlew` dan `gradle-wrapper.jar`. Setelah Gradle 9.3.1 tersedia, jalankan dari root project:

```bash
gradle wrapper --gradle-version 9.3.1 --distribution-type bin
git add gradlew gradlew.bat gradle/wrapper/
git commit -m "Add Gradle 9.3.1 wrapper"
```

Setelah wrapper sudah masuk repository, build berikutnya cukup memakai:

```bash
chmod +x gradlew
./gradlew assembleDebug
./gradlew :app:testDebugUnitTest
```

APK debug:

`app/build/outputs/apk/debug/app-debug.apk`

## 4. Alternatif otomatis

Workflow `.github/workflows/android-build.yml` akan menyiapkan JDK, Android SDK, dan Gradle 9.3.1, lalu membangun APK dan menjalankan unit test setiap push ke `main`/`master` atau pull request. Hasil APK tersedia sebagai GitHub Actions artifact.

## Catatan

- `namespace` tetap `com.example` untuk menjaga seluruh referensi `com.example.R` yang sudah ada di source. `applicationId` Android sudah diatur menjadi `com.tokokuh.app`.
- Tidak ada Firebase/Google Services/Secrets Gradle plugin yang diperlukan untuk aplikasi offline ini; plugin tersebut telah dilepas dari konfigurasi build.
- `INTERNET` permission dihapus dari manifest hasil merge.
- Build belum dapat divalidasi di lingkungan pemeriksaan ZIP ini karena Gradle/Android SDK tidak tersedia dan akses unduhan jaringan dibatasi. Validasi build harus dilakukan di Codespaces atau GitHub Actions.
