<div align="center">

  <img src="https://snitify.snitrix.in/logosnitify.png" alt="Snitify Logo" width="120" height="120" />

  # Snitify
  ### *Free Music Streaming & Offline Audio Player for Android*


  <br />

  <p align="center">
    <strong>Snitify</strong> is a high-performance, open-source Android music client built with modern Jetpack Compose. Stream millions of songs online, download tracks at maximum speed for offline listening, and seamlessly import playlists from Spotify, YouTube, and CSV files — <strong>100% ad-free and built with privacy</strong>.
  </p>

  <p align="center">
    <a href="https://snitify.snitrix.in"><strong>Explore Official Website »</strong></a>
    ·
    <a href="https://github.com/snigdhkumar/snitify/releases/latest"><strong>Download Latest APK</strong></a>
    ·
    <a href="https://developer.snitrix.in"><strong>Developer Portal</strong></a>
  </p>

</div>

---

## ✨ Features

### 🎵 1. Online Streaming & Offline Media
- **Stream Unlimited Tracks**: High-fidelity online audio playback with YouTube Music backend.
- **Top-Speed Downloader**: Fast multi-threaded track downloads directly to your device storage with embedded cover art and metadata.
- **Local Audio Player**: Full playback support for device audio files (`MP3`, `M4A`, `FLAC`, `WAV`, `OGG`, `AAC`).

### 📦 2. Universal Playlist Import & Backup
- **Spotify**: Import songs into your local library.
- **YouTube Music**: Import YouTube and YouTube Music playlists effortlessly.
- **CSV Import/Export**: Backup or migrate your favorite playlists using universal CSV format.
- **Full Database Backup**: One-tap SQLite database backup and restoration to keep your data safe.

### 🎨 3. Modern Material 3 & Fluid Gestures
- **Dynamic Theme Engine**: Automatic light/dark mode switching and personalized color accent palettes.
- **Floating Music Capsule**: Interactive music capsule with responsive swipe gestures:
  - **Swipe Left ($\leftarrow$)**: Next track.
  - **Swipe Right ($\rightarrow$)**: Previous track.
  - **Swipe Down ($\downarrow$)**: Instant stop & collapse into the bottom navigation bar.
- **Synchronized Visualizer**: Smooth real-time audio visualization bars.

### 🔒 4. Privacy-First
- **No Ads & No Tracking**: 100% ad-free experience without telemetry, tracking scripts, or data brokers.
- **Snitrix Platform Ecosystem**: Built following privacy-first principles across all Snitrix apps.

---

## 📱 Screenshots

<div align="center">
  <table>
    <tr>
      <td align="center" width="33%">
        <img src="https://snitify.snitrix.in/assets/screen-player-DvMu5_9e.jpg" alt="Now Playing Player" width="100%" />
        <br />
        <sub><b>Now Playing Player</b></sub>
      </td>
      <td align="center" width="33%">
        <img src="https://snitify.snitrix.in/assets/screen-library-CPrbN5XC.jpg" alt="Library & Downloads" width="100%" />
        <br />
        <sub><b>Library & Downloads</b></sub>
      </td>
      <td align="center" width="33%">
        <img src="https://snitify.snitrix.in/assets/screen-playlists-chaKESLG.jpg" alt="Playlists & Discovery" width="100%" />
        <br />
        <sub><b>Playlists & Discovery</b></sub>
      </td>
    </tr>
  </table>
</div>

---

## 🛠️ Architecture & Tech Stack

Snitify is crafted using modern Android development best practices and architecture components:

| Layer | Technologies & Libraries |
| :--- | :--- |
| **Language** | [Kotlin 2.1.0](https://kotlinlang.org/) |
| **UI Framework** | [Jetpack Compose](https://developer.android.com/jetpack/compose) + [Material 3](https://m3.material.io/) |
| **Audio Engine** | [Media3 ExoPlayer](https://developer.android.com/guide/topics/media/media3) + Foreground Audio Service |
| **Local Database** | Custom SQLite Database Manager (`DatabaseManager.kt`) |
| **Preferences** | Android Jetpack [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) |
| **Image Loading** | [Coil 3](https://coil-kt.github.io/coil/) with disk & memory caching |
| **Asynchronous** | Kotlin Coroutines & `StateFlow` |
| **Network & Parsing** | Innertube API client & Ktor / OkHttp |

---

## 🚀 Building from Source

### Prerequisites
- **Android Studio** Ladybug (2024.2+) or newer
- **JDK 17** or **JDK 21** (Set `JAVA_HOME` to your JDK path)
- **Android SDK** API 35 with Build Tools `35.0.0`

### Steps

1. **Clone the repository:**
   ```bash
   git clone https://github.com/snigdhkumar/snitify.git
   cd snitify
   ```

2. **Open the project in Android Studio** or build directly via CLI:

3. **Compile Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```
   The debug APK will be generated at:
   `app/build/outputs/apk/debug/app-debug.apk`

4. **Install on connected Android device:**
   ```bash
   ./gradlew installDebug
   ```

---

## 🌐 Snitrix Ecosystem

Snitify is part of the **Snitrix Platform** — a suite of tools built with privacy:

- **[Snitrix Platform](https://snitrix.in)** — End-to-end encrypted messaging across Web & Android.
- **[Brutal Speed](https://speedtest.snitrix.in)** — Ultra-fast, ad-free network speed testing.
- **[Snitify](https://snitify.snitrix.in)** — Free open-source Android music client.
- **[Developer Portal](https://developer.snitrix.in)** — Projects, APIs, and open-source software by Snigdh.

---

## 🤝 Contributing

Contributions, feature suggestions, and bug reports are welcome!

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

</div>