# FITHUB

FITHUB is a production-grade, local-first personal fitness management application for Android built with Kotlin, Jetpack Compose, Room Database, and Material Design 3.

## Overview

FITHUB empowers users to track, analyze, and optimize their daily physical conditioning through offline-first local data persistence, precise biometric calculations, multi-modal workout tracking, nutrition planning, sleep recovery analysis, and custom scheduling.

## Key Features

- **Home Dashboard:** High-contrast lime hero card showing today's activity, active workout streak, active duration, calories burned, distance covered, quick action shortcuts, scheduled routines, and supportive neutral guidance.
- **Multi-Modal Workout Tracking:**
  - **Running & Walking:** GPS tracking powered by Fused Location Provider with Haversine distance calculation and multi-stage rejection filters (accuracy gating, speed bounds, stationary drift suppression).
  - **Jumping:** 5-state accelerometer state machine (READY, TAKE_OFF, AIRBORNE, LANDING, COMPLETED) with calibration mode and manual controls.
  - **Weightlifting:** Structured exercise logging, target reps, set weights, live session tonnage accumulation, and rest interval countdown timers.
  - **Crash / Restart Recovery:** In-progress workout state persists continuously to local storage, providing seamless recovery after unexpected process restarts.
- **Workout Plans & Scheduling:** Automated multi-day routine generator based on available equipment, time budget, and training goals; native Android notification reminders with customizable alert offsets.
- **Nutrition:** Mifflin-St Jeor BMR and TDEE calorie target tracking, protein range computation, macronutrient balance, and pre-seeded whole foods database with custom food logging.
- **Sleep & Recovery:** Bedtime and wake-time tracking crossing midnight, sleep quality ratings, and 7-day/30-day consistency metrics.
- **Progress & Analytics:** Interactive 7-day Canvas activity chart, streak analytics, cumulative volume and distance, and filterable history logs.
- **Localization:** 100% key parity between English and Khmer (ភាសាខ្មែរ) with instant runtime switching without application restart.
- **UI Design System:** Dark charcoal aesthetic (#121316) paired with bright neon lime accents (#D4F23A), generous padding, rounded cards (24-32dp), and floating pill navigation.
- **Zero Emoji Policy:** Absolutely zero emojis across the application, code, resources, translations, tests, and documentation.

## Architecture & Technology Stack

- **Platform:** Android (Min SDK 24, Target SDK 36)
- **Language:** Kotlin 2.2.x
- **UI Framework:** Jetpack Compose with Material Design 3
- **Local Persistence:** Room Database 2.7.0 (SQLite) via Kotlin Symbol Processing (KSP)
- **Location & Sensors:** Google Play Services Location and Android SensorManager
- **Asynchronous Execution:** Kotlin Coroutines and StateFlow

## Running Tests

To execute the unit and calculation test suite:
```bash
gradle :app:testDebugUnitTest
```

## GitHub Releases & In-App Auto-Update (Option 2)

FITHUB features a built-in direct APK In-App Auto-Updater. To host your own updates on GitHub:

1. **Push to your GitHub repository:**
   Export from AI Studio or push the repository to your GitHub account (`https://github.com/YOUR_USERNAME/YOUR_REPO`).

2. **Publish a Release on GitHub:**
   - Go to your repository on GitHub and click **Releases** -> **Create a new release**.
   - Create a version tag (e.g., `v1.1.0`).
   - Attach your built `app-release.apk` file to the release.
   - Click **Publish release**.

3. **Update `version.json`:**
   - Update `version.json` in the root of your repository with the new `versionCode`, `versionName`, release notes, and the download link:
     `https://github.com/YOUR_USERNAME/YOUR_REPO/releases/latest/download/app-release.apk`
   - Point `UpdateManager.customUpdateUrl` to your raw JSON URL:
     `https://raw.githubusercontent.com/YOUR_USERNAME/YOUR_REPO/main/version.json`

Users will automatically receive the update prompt inside the app, download the APK with live progress, and install it with one tap without losing their local database records.

