# 📺 VisionTV

**VisionTV** is a personal, non-commercial Android TV application for streaming live TV channels, movies, and series using custom M3U/IPTV playlists. It is optimized for the **Argentinian** market while remaining globally compatible. It uses [The Movie Database (TMDB)](https://www.themoviedb.org/) API to enrich content with professional posters, descriptions, and ratings.

> ⚠️ **Legal Notice:** This project is for personal, non-commercial use only. It is not affiliated with, endorsed by, or certified by The Movie Database (TMDB). No content is provided by the app — users supply their own M3U playlist URLs.

---

## ✨ Features

- 📡 **Live TV** — Stream live channels from M3U/IPTV playlists.
- 🇦🇷 **Argentina First** — Automatically identifies and prioritizes Argentinian channels in a dedicated "Argentina" home row.
- 🎬 **Movies & Series** — Browse and play VOD content with a Netflix-style UI, automatically enriched with TMDB metadata.
- ➕ **Playlist Manager** — Add and manage multiple M3U playlist URLs for different content types.
- 🔎 **Search & Filter** — Instant search by title and filter by category/genre within each section.
- ❤️ **Favorites & Recents** — Quick access to your favorite and recently viewed channels.
- 🎮 **Android TV Optimized** — Full D-pad / remote control support with clear focus indicators and animations.
- ▶️ **Professional Player** — Custom ExoPlayer-based player with automatic control fading, seek support, and error handling.

---

## 🏗️ Architecture & Tech Stack

VisionTV follows a clean **MVVM** architecture with a unidirectional data flow pattern.

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Android TV UI | AndroidX TV Foundation & TV Material |
| Navigation | Navigation Compose |
| State management | ViewModel + StateFlow |
| Video playback | Media3 ExoPlayer |
| Image loading | Coil 3 |
| Networking | Retrofit + OkHttp |
| Language | Kotlin (JVM 17) |
| Target SDK | Android 15 (API 37) |

---

## 🚀 Getting Started

### Prerequisites

- Android Studio (latest stable version)
- JDK 17
- A TMDB API key (free)
- One or more M3U playlist URLs

### 1. Configure TMDB API Key

VisionTV uses `local.properties` to manage sensitive information securely.

1.  Open `local.properties` in the root of the project.
2.  Add your TMDB **API Read Access Token** (JWT):
    ```properties
    TMDB_TOKEN="your_jwt_token_here"
    ```

> ℹ️ Get your token at [themoviedb.org/settings/api](https://www.themoviedb.org/settings/api) under the "API Read Access Token" section.

### 2. Build & run

Run the project on an **Android TV emulator** or physical device for the best experience. The app will also work on standard Android phones.

---

## 🎮 Remote Control / D-pad Navigation

| Key | Action |
|---|---|
| D-pad Center / Enter | Select Item / Play / Pause |
| D-pad Right | Seek +10 seconds |
| D-pad Left | Seek -10 seconds |
| Back / Escape | Exit Player / Navigate back |

---

## 📄 License

```
Copyright (c) 2026 IgnaColla

This project is provided for personal, non-commercial use only.
Commercial use, redistribution for profit, or use in any product or service
that generates revenue is strictly prohibited.
```

---

## 🙏 Acknowledgements

- [The Movie Database (TMDB)](https://www.themoviedb.org/) — metadata API
- [AndroidX Media3](https://github.com/androidx/media) — video playback
- [IPTV-org](https://iptv-org.github.io/) — playlist resources
