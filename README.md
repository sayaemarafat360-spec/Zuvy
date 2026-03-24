# Zuvy - All-in-One Media Player

**Your Media, Your Way** 🎬🎵

Zuvy is a modern, feature-rich media player for Android inspired by PlayIt. It combines video playback, music player, and online content discovery in one beautiful app.

## Features

### 🏠 Home Tab
- **Videos**: Browse all videos from your device with grid/list view
- **Folders**: Navigate through folders to find your media
- **Playlists**: Create and manage custom playlists

### 🎵 Music Tab
- Browse by Songs, Artists, Albums, Genres
- Favorites and Recently Played
- Background playback with notification controls
- Built-in equalizer

### 🌐 Discover Tab
- Trending content
- Online Radio stations (free)
- Podcasts
- Free music streaming

### ⚙️ More Tab
- Appearance settings (Dark/Light theme)
- Player settings (gestures, speed, etc.)
- Storage management
- Privacy & Security (App lock)
- Premium features

### 🎬 Advanced Video Player
- Gesture controls (brightness, volume, seek)
- Playback speed (0.25x - 4x)
- Subtitle support
- Audio track selection
- Picture-in-Picture mode
- Screen lock
- Sleep timer
- Equalizer

## Tech Stack

- **Language**: Kotlin
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Database**: Room
- **Media Playback**: ExoPlayer (Media3)
- **Image Loading**: Glide & Coil
- **Backend**: Firebase (Auth, Firestore, Analytics, Crashlytics)
- **Ads**: AdMob
- **UI Components**: Material Design 3, Shimmer

## Requirements

- Android 10+ (API 29)
- Android Studio Hedgehog or later
- JDK 17
- Gradle 8.2

## Building the Project

### Prerequisites
1. Install Android Studio
2. Set up JDK 17
3. Create a Firebase project and add `google-services.json`

### Build Steps
```bash
# Clone the repository
git clone https://github.com/yourusername/zuvy.git

# Open in Android Studio
# Sync Gradle files
# Build > Make Project
```

### Build with Codemagic
This project includes `codemagic.yaml` for CI/CD. Simply connect your repository to Codemagic and it will automatically build your APKs.

1. Create a Codemagic account
2. Connect your GitHub/GitLab repository
3. The workflow will automatically detect the configuration

## Project Structure

```
app/
├── src/main/
│   ├── java/com/zuvy/app/
│   │   ├── data/           # Data layer
│   │   │   ├── model/      # Data models
│   │   │   ├── repository/ # Repositories
│   │   │   └── local/      # Local data sources
│   │   ├── di/             # Dependency injection
│   │   ├── domain/         # Domain layer
│   │   ├── services/       # Background services
│   │   ├── ui/             # UI layer
│   │   │   ├── home/       # Home tab
│   │   │   ├── music/      # Music tab
│   │   │   ├── discover/   # Discover tab
│   │   │   ├── more/       # More tab
│   │   │   └── player/     # Player screens
│   │   └── utils/          # Utilities
│   ├── res/                # Resources
│   └── AndroidManifest.xml
├── build.gradle.kts
└── google-services.json
```

## Configuration

### Firebase Setup
1. Create a new Firebase project
2. Add an Android app with package name `com.zuvy.app`
3. Download `google-services.json` and place it in `app/` folder
4. Enable Authentication, Firestore, and Analytics

### AdMob Setup
The project uses test AdMob IDs by default. Replace with your own IDs for production:
- Banner: `ca-app-pub-3940256099942544/6300978111` (test)
- Interstitial: `ca-app-pub-3940256099942544/1033173712` (test)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Inspired by [PlayIt](https://play.google.com/store/apps/details?id=com.playit.videoplayer)
- Icons from [Material Design Icons](https://material.io/icons)
- Fonts from [Google Fonts](https://fonts.google.com)

---

Made with ❤️ by Sayaem
