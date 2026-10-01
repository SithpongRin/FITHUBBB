# FITHUB

[![Build Android APK](https://github.com/SithpongRin/FITHUBBB/actions/workflows/build.yml/badge.svg)](https://github.com/SithpongRin/FITHUBBB/actions/workflows/build.yml)
[![Download APK](https://img.shields.io/badge/Download-APK%20(v1.0.0)-brightgreen?logo=android)](https://github.com/SithpongRin/FITHUBBB/releases)

**[📥 Click here to Download the latest Android APK](https://github.com/SithpongRin/FITHUBBB/releases)**

---

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

## Offline-First Architecture & Cloud Sync

FITHUB is built offline-first. All workouts, biometric calculations, nutrition tracking, sleep recovery, and customized plans operate 100% locally using Room SQLite:
- **Offline Mode:** When there is no internet connection, the app remains fully functional. Records are stored locally with zero disruptions.
- **Online Mode & Auto-Sync:** Integrated with real-time network state monitoring (`NetworkMonitor`). As soon as internet connectivity returns, the app automatically triggers bidirectional cloud sync with Firebase Firestore to backup local changes and download remote updates.
- **Live Status:** A real-time status banner on the Home dashboard clearly indicates whether the app is currently Cloud Synced, Offline (Saved Locally), or Syncing.

## In-App Over-The-Air (OTA) Feature Updates (No APK Reinstall Needed)

FITHUB features an instant Over-The-Air (OTA) dynamic feature and content delivery engine (`DynamicFeatureManager`):
- **Instant Updates Without APK Reinstall:** New workout programs (e.g. Kun Khmer Conditioning, HIIT Metabolic Shredder), new exercise libraries, healthy whole food additions, and fitness challenges can be updated directly inside the app over the air.
- **How to Push New Features:**
  1. Edit `features.json` in the root of your repository with new workout routines, exercises, or food items.
  2. Commit and push `features.json` to GitHub (`https://github.com/YOUR_USERNAME/YOUR_REPO/main/features.json`).
  3. When users open the app or tap **Check for Feature Updates**, FITHUB downloads the feature pack and imports the new routines and items directly into the local Room database.
  4. The new features activate immediately without requiring an APK download, Android permissions, or app restart!

## GitHub Releases & Core Engine APK Updater (Option 2)

For core native binary engine updates (e.g., changes to native Android sensors, core services, or low-level components):
1. **Publish a Release on GitHub:**
   - Attach your built `app-release.apk` to a new GitHub Release.
2. **Update `version.json`:**
   - Update `version.json` with the new `versionCode`, `versionName`, and APK download link.
3. Users can download and install the package with live progress directly from the Settings screen.


