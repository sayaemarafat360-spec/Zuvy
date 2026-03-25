# 🎲 Ludo Blitz

<div align="center">

![Ludo Blitz Logo](app/src/main/res/drawable/ic_logo.png)

**Roll. Race. Reign Supreme!**

A beautiful, feature-rich Ludo game for Android with superior UI/UX

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-blue.svg)](https://kotlinlang.org)
[![MinSDK](https://img.shields.io/badge/MinSDK-26-orange.svg)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-red.svg)](#license)

</div>

---

## 📖 Table of Contents

- [Features](#-features)
- [Screenshots](#-screenshots)
- [Tech Stack](#-️-tech-stack)
- [Getting Started](#-getting-started)
- [Building with Codemagic](#-building-with-codemagic)
- [Firebase Setup](#-firebase-setup)
- [Project Structure](#-project-structure)
- [Game Rules](#-game-rules)
- [Monetization](#-monetization)
- [Contributing](#-contributing)
- [License](#-license)

---

## ✨ Features

### 🎮 Game Modes
- **Local Multiplayer** - Pass & play with friends on the same device (2-4 players)
- **Online Multiplayer** - Play with friends or random players worldwide
- **Play vs AI** - Challenge AI opponents with 4 difficulty levels (Easy, Medium, Hard, Expert)
- **Quick Match** - Instant matchmaking for online games
- **Private Rooms** - Create/join rooms with unique codes

### 🎨 UI/UX
- Beautiful, modern Material Design 3
- Dark & Light theme support
- Smooth animations with Lottie
- Multiple board themes & token styles
- Custom dice designs

### 🔥 Gamification
- Coins & Gems currency system
- Daily login rewards (7-day streak bonus)
- Lucky Spin Wheel
- XP & Level progression
- Achievements & Badges
- Global & Friends Leaderboards

### 💰 Monetization
- AdMob integration with smart offline caching
- Rewarded video ads for bonuses
- In-app purchases (Remove ads, Coins, Cosmetics)
- Non-intrusive ad placement

### 🛠 Technical
- Firebase Authentication (Email, Google, Guest)
- Real-time multiplayer with Firebase Realtime Database
- Offline support with Room Database
- Optimized for low-end devices
- Clean Architecture with MVVM

---

## 📱 Screenshots

| Home Screen | Game Board | Shop |
|-------------|------------|------|
| ![Home](screenshots/home.png) | ![Game](screenshots/game.png) | ![Shop](screenshots/shop.png) |

| Profile | Leaderboard | Daily Reward |
|---------|-------------|--------------|
| ![Profile](screenshots/profile.png) | ![Leaderboard](screenshots/leaderboard.png) | ![Reward](screenshots/reward.png) |

---

## 🛠️ Tech Stack

| Category | Technology |
|----------|------------|
| **Language** | Kotlin 1.9.20 |
| **UI Framework** | XML + Material Design 3 |
| **Architecture** | MVVM + Clean Architecture |
| **Dependency Injection** | Hilt |
| **Backend** | Firebase (Auth, Firestore, Realtime DB, Storage) |
| **Local Database** | Room |
| **Data Storage** | DataStore Preferences |
| **Animations** | Lottie |
| **Image Loading** | Glide |
| **Ads** | Google AdMob |
| **Payments** | Google Play Billing |

---

## 🚀 Getting Started

### Prerequisites

- No PC required! Build in cloud with Codemagic
- OR Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34

### Setup Instructions

1. **Clone or Download the project**
   ```bash
   git clone https://github.com/yourusername/ludo-blitz.git
   cd ludo-blitz
   ```

2. **Firebase Setup** (See [Firebase Setup](#-firebase-setup))

3. **Build the project**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on device**
   ```bash
   ./gradlew installDebug
   ```

---

## ☁️ Building with Codemagic

Since you don't have a powerful PC, you can build the app entirely in the cloud using Codemagic!

### Steps:

1. **Create a Codemagic Account**
   - Go to [codemagic.io](https://codemagic.io)
   - Sign up with your GitHub/GitLab/Bitbucket account

2. **Connect your Repository**
   - Push this project to a Git repository
   - Add the repository to Codemagic

3. **Configure Build**
   - The `codemagic.yaml` file is already configured
   - Just connect your repository and start building!

4. **Download APK**
   - After successful build, download the APK from Codemagic
   - Install directly on your Android device

### Build Environment:
- Android SDK: Stable
- Java: 17
- Build Duration: ~15-20 minutes

---

## 🔥 Firebase Setup

### Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Click "Add project"
3. Enter project name: "Ludo Blitz"
4. Enable Google Analytics (optional)
5. Create project

### Step 2: Add Android App

1. In Firebase Console, click the Android icon
2. Enter package name: `com.ludoblitz.app`
3. Download `google-services.json`
4. Place it in `app/google-services.json`

### Step 3: Enable Services

Enable these Firebase services:

1. **Authentication**
   - Email/Password
   - Google Sign-In
   - Anonymous (Guest mode)

2. **Firestore Database**
   - Create database in test mode
   - Configure security rules

3. **Realtime Database**
   - For multiplayer game state
   - Configure rules

4. **Storage**
   - For user avatars

### Step 4: Get SHA-1 Certificate

For Google Sign-In, you need to add SHA-1 fingerprint:

```bash
# Debug SHA-1
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# For Codemagic builds, get SHA-1 from Codemagic dashboard
```

Add SHA-1 to Firebase Project Settings > Your Apps > SHA certificate fingerprints

---

## 📁 Project Structure

```
LudoBlitz/
├── app/
│   ├── src/main/
│   │   ├── java/com/ludoblitz/app/
│   │   │   ├── data/                 # Data Layer
│   │   │   │   ├── local/           # Local data (Room, DataStore)
│   │   │   │   ├── model/           # Data models
│   │   │   │   ├── remote/          # API services
│   │   │   │   └── repository/      # Repositories
│   │   │   ├── domain/              # Domain Layer
│   │   │   │   ├── ai/              # AI logic
│   │   │   │   ├── gamelogic/       # Game engine
│   │   │   │   └── usecase/         # Use cases
│   │   │   ├── ui/                  # UI Layer
│   │   │   │   ├── adapters/        # RecyclerView adapters
│   │   │   │   ├── animations/      # Animation helpers
│   │   │   │   ├── components/      # Custom views
│   │   │   │   ├── screens/         # Activities & Fragments
│   │   │   │   ├── theme/           # App theme
│   │   │   │   └── viewmodel/       # ViewModels
│   │   │   ├── utils/               # Utility classes
│   │   │   ├── services/            # Services
│   │   │   └── LudoBlitzApp.kt      # Application class
│   │   ├── res/                     # Resources
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── codemagic.yaml                   # CI/CD config
└── README.md
```

---

## 🎯 Game Rules

### Classic Mode
- Roll a 6 to bring tokens out of base
- Roll again after rolling 6
- 3 consecutive 6s = skip turn
- Land on opponent's token to capture and send back to base
- Safe zones (stars) protect from capture
- First to get all 4 tokens home wins

### Modern Mode
- Any roll can release tokens
- Same rules otherwise

---

## 💵 Monetization

### AdMob Integration
- **Test IDs** are configured by default
- Replace with real IDs in `build.gradle.kts`:
  ```kotlin
  buildConfigField("String", "ADMOB_BANNER_ID", "\"your-banner-id\"")
  buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"your-interstitial-id\"")
  buildConfigField("String", "ADMOB_REWARDED_ID", "\"your-rewarded-id\"")
  ```

### Smart Offline Ad Strategy
1. **Preloading** - Ads are cached when online
2. **Rewarded Ads** - Optional for bonuses
3. **Non-intrusive** - Ads shown after games, not during

### In-App Purchases
- Remove Ads ($2.99)
- Coin Packs ($0.99 - $9.99)
- Cosmetic Items ($0.99 - $4.99)

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- Inspired by Ludo King
- Material Design 3 by Google
- Firebase by Google
- Lottie Animations by Airbnb

---

<div align="center">

**Made with ❤️ by Sayaem**

</div>
