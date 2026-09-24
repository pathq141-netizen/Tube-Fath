<div align="center">
  <img src="Assets/logo.png" alt="FathTube Logo" width="140" height="140">
  <br><br>
  
  <div align="center">

<img src="https://img.shields.io/badge/Status-Active_Development-success?style=for-the-badge&logo=github-actions">
<br>

<!-- Tech Stack -->
<img src="https://img.shields.io/badge/Platform-Android_8.0+-3DDC84?style=for-the-badge&logo=android&logoColor=white">
<img src="https://img.shields.io/badge/Kotlin-100%25-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white">
<img src="https://img.shields.io/badge/Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white">

<br>

<img src="https://img.shields.io/badge/License-GPL_v3.0-blue?style=for-the-badge&logo=gnu-bash&logoColor=white">
<img src="https://img.shields.io/badge/Package-com.fathtube.app-red?style=for-the-badge">

</div>

  <br><br>
  
  <h3>A fast, privacy-respecting YouTube client for Android with YouTube-like layout and Nanz Mode.</h3>
  <p>
    <b>FathTube</b> is built with Jetpack Compose and Material 3.<br>
    Includes <b>Nanz Mode</b> for intelligent, distraction-free local content recommendation — no accounts, no tracking, no data leaves your phone.<br>
    <b>Dev oleh Nanz (nanas)</b>
  </p>
  
  <p>
    <a href="https://github.com/nanasmuda121/FathTube/releases"><b>Download APK</b></a> · 
    <a href="https://github.com/nanasmuda121/FathTube"><b>GitHub Repository</b></a>
  </p>
</div>

---

## What is FathTube?

FathTube is an Android application designed for a streamlined, privacy-conscious YouTube experience. Inspired by classic YouTube design principles, it delivers a clean, intuitive layout (Home, Shorts, Subscriptions, and You) while keeping all intelligence local to your device.

---

## Key Features

### 🎬 Video Playback & Experience
- High-quality playback via ExoPlayer (Media3) with resolution switching (1080p, 720p, 480p, 360p)
- **YouTube-Style Layout**: Sleek rounded cards, duration badges, channel details, and familiar navigation (Home, Shorts, Subscriptions, You).
- **SponsorBlock Integration**: Automatically skips sponsors, intros, outros, and non-music filler.
- **DeArrow**: Replaces sensationalized clickbait thumbnails and titles.
- **Return YouTube Dislike**: Displays community dislike counts and ratios.
- **Background & PiP**: Listen to audio with screen off or multitask with Picture-in-Picture.
- **Gesture Controls**: Smooth swipe gestures for volume, brightness, and seeking.
- **Subtitles & Chapters**: Native subtitle rendering and chapter jumping.

### ⚡ Nanz Mode
- **Neural On-Device Intelligence**: Learns your viewing patterns locally without telemetry or tracking.
- **Instant Mode Switch**: Toggle Nanz Mode right from the top bar with a single tap.
- **Boredom Detection & Exploration**: Intelligently mixes fresh content while preventing repetitive echo chambers.
- **Data Transparency**: Inspect, reset, or backup your local profile anytime.

### 🛡️ Privacy & Performance
- Zero Google account dependency.
- No analytics, ad trackers, or third-party tracking libraries.
- Clean YouTube-style theme options: System Default, Modern Dark, Pure OLED Black, and Light.

---

## 🛠️ Build & Development

FathTube is built using standard Android Gradle tooling.

```bash
# Debug Build
./gradlew assembleGithubDebug

# Release Build
./gradlew assembleGithubRelease
```

Builds are also automated via **GitHub Actions** on every push.

---

## 🙏 Acknowledgments

FathTube is built upon open-source foundations:
- **[Flow (A-EDev/Flow)](https://github.com/A-EDev/Flow)**: Base architecture and local neural engine foundations.
- **[NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor)**: Data extraction engine.
- **[PipePipe](https://codeberg.org/NullPointerException/PipePipe)**: Streaming pipeline insights and SABR handling.
- **[LibreTube](https://github.com/LibreTube/LibreTube)**: SponsorBlock & DeArrow integration patterns.
- **[ExoPlayer / Media3](https://github.com/google/ExoPlayer)**: Media playback engine.

---

## 📄 License

FathTube is free software licensed under the **GNU General Public License v3 (GPLv3)**.

**Dev oleh Nanz (nanas)**
Original upstream copyright © 2025-2026 A-EDev.
