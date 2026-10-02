# FITHUB Project Summary and Architecture

## 1. Overview (សេចក្តីផ្តើមទូទៅ)
FITHUB គឺជាកម្មវិធីទូរស័ព្ទ Android កម្រិតខ្ពស់សម្រាប់តាមដានសុខភាព និងការហាត់ប្រាណ (Fitness & Health Tracking Application) ដែលត្រូវបានអភិវឌ្ឍឡើងដោយប្រើប្រាស់ Modern Android Architecture, Jetpack Compose Material 3, Room Database, Kotlin Coroutines & Flow, និង Multi-Cloud Synchronization System (Cloud Firestore និង External Cloud API)។ កម្មវិធីនេះដំណើរការតាមបែប Offline-First Architecture ដែលអាចឱ្យអ្នកប្រើប្រាស់ហាត់ប្រាណ តាមដានអាហារ ដំណេក និងកាលវិភាគបានយ៉ាងរលូន ទោះបីជាគ្មានការតភ្ជាប់អ៊ីនធឺណិតក៏ដោយ។

---

## 2. Project Directory Structure (រចនាសម្ព័ន្ធគម្រោង)

```
D:/FITHUBBB/
├── .github/
│   └── workflows/
│       └── build.yml               # CI/CD Pipeline ស្វ័យប្រវត្តសម្រាប់ Build Release APK និង Upload Release
├── app/
│   ├── build.gradle.kts            # Gradle Configuration (Dependencies, SDKs, Signing Configs, Versions)
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml # App Manifest, Permissions (Exact Alarm, Notifications, GPS, Service)
│           ├── java/com/example/
│           │   ├── MainActivity.kt # Entry Point, Permissions Launcher, Deep Links & Lifecycle
│           │   ├── account/
│           │   │   └── AccountManager.kt     # ប្រព័ន្ធគ្រប់គ្រងគណនី, Auth, Firestore & Server Cloud Sync
│           │   ├── calculations/
│           │   │   ├── CalorieCalculator.kt  # រូបមន្តគណនា BMR, TDEE, Calorie Deficit/Surplus
│           │   │   ├── DistanceCalculator.kt # គណនាចម្ងាយតាម GPS Haversine Formula
│           │   │   ├── FormatUtils.kt        # Format ម៉ោង, ចម្ងាយ (km/mi), កាឡូរី, ទម្ងន់
│           │   │   ├── NutritionCalculator.kt# គណនាម៉ាក្រូអាហារូបត្ថម្ភ (Protein, Carbs, Fat)
│           │   │   └── PaceCalculator.kt     # គណនាល្បឿនរត់ និង Pace (min/km)
│           │   ├── data/
│           │   │   ├── Daos.kt               # Room DAOs (WorkoutDao, PlanDao, NutritionDao, SleepDao, StepDao)
│           │   │   ├── Entities.kt           # Room Entities (Workouts, Plans, Nutrition, Sleep, Steps, Schedule)
│           │   │   ├── FithubDatabase.kt     # Room Database Instance, Migrations & TypeConverters
│           │   │   ├── FithubRepository.kt   # Single Source of Truth Repository Pattern
│           │   │   └── SeedData.kt           # ទិន្នន័យគំរូដំបូង (Food Database, Workout Routines)
│           │   ├── localization/
│           │   │   ├── AppLanguage.kt        # Enum ភាសា (ខ្មែរ / អង់គ្លេស)
│           │   │   ├── StringsEn.kt          # វចនានុក្រមពាក្យភាសាអង់គ្លេស
│           │   │   ├── StringsKm.kt          # វចនានុក្រមពាក្យភាសាខ្មែរ
│           │   │   └── StringKey.kt          # Translation Key References
│           │   ├── network/
│           │   │   ├── CloudSyncService.kt   # Network Client សម្រាប់ sync ជាមួយ Server
│           │   │   └── NetworkMonitor.kt     # Connectivity Manager ពិនិត្យស្ថានភាពអ៊ីនធឺណិត Live
│           │   ├── notifications/
│           │   │   └── NotificationHelper.kt # ប្រព័ន្ធ Exact AlarmManager, Channel, និង BroadcastReceiver
│           │   ├── sensors/
│           │   │   ├── JumpDetector.kt       # SensorEventListener (Accelerometer) រាប់ការលោតខ្សែ
│           │   │   ├── LocationTracker.kt    # FusedLocationProviderClient តាមដាន GPS ពេលរត់/ដើរ
│           │   │   └── StepSensorManager.kt  # Sensor.TYPE_STEP_COUNTER និង Step Detector
│           │   ├── services/
│           │   │   └── WorkoutForegroundService.kt # Foreground Service ដំណើរការសកម្មភាពពេលបិទអេក្រង់
│           │   ├── ui/
│           │   │   ├── components/
│           │   │   │   ├── CommonComponents.kt   # Header, Cards, Badges, Modals
│           │   │   │   └── DashboardComponents.kt# Metric Cards, Graphs, Progress Bars
│           │   │   ├── screens/
│           │   │   │   ├── AuthGateScreen.kt     # Login, Register, Cloud Sync Dialog & Guest Mode
│           │   │   │   ├── HomeScreen.kt         # ទំព័រដើម Dashboard (Summary, Steps, Calorie Goal, Daily Ring)
│           │   │   │   ├── PlansScreen.kt        # កាលវិភាគរំលឹក (Schedules) & AI Workout Routine Generator
│           │   │   │   ├── ProfileScreen.kt      # កែប្រែព័ត៌មានផ្ទាល់ខ្លួន, BMR, TDEE, Profile Photo
│           │   │   │   ├── ProgressScreen.kt     # ប្រវត្តិហាត់ប្រាណ (Workouts History, Delete, Charts, Filter)
│           │   │   │   ├── SettingsScreen.kt     # ការកំណត់ភាសា, Theme, Units (Metric/Imperial), Account
│           │   │   │   └── WorkoutScreen.kt      # អេក្រង់ហាត់ប្រាណ Live (Running, Walking, Jumping, Lifting)
│           │   │   ├── theme/
│           │   │   │   ├── Color.kt              # Luxury Sport Theme (Neon Lime, Deep Charcoal, Accents)
│           │   │   │   ├── Theme.kt              # Jetpack Compose Material 3 Dark & Light Theme
│           │   │   │   └── Type.kt               # Typography System
│           │   │   └── viewmodel/
│           │   │       └── FithubViewModel.kt    # Central State Management, Live Flows & Actions
│           │   └── updates/
│           │       ├── ApkInstaller.kt       # ទាញយក APK និងដំឡើងតាមរយៈ Android PackageInstaller
│           │       └── UpdateManager.kt      # OTA Version Checker តាម GitHub Raw & jsDelivr CDN
│           └── res/
│               ├── drawable/
│               │   ├── ic_notification.xml   # ស្តង់ដារ Monochrome 24dp Vector Icon សម្រាប់ Notification
│               │   ├── ic_launcher_background.xml
│               │   └── ic_launcher_foreground.xml
│               └── values/
│                   └── strings.xml
├── version.json                              # Metadata កំណែ App សម្រាប់ OTA In-App Updater
├── Fithub.apk                                # Built Production APK
├── build.gradle.kts                          # Root Project Gradle Build Script
└── settings.gradle.kts                       # Gradle Project Settings
```

---

## 3. Core Features and Technical Capabilities (មុខងារស្នូល និងបច្ចេកវិទ្យា)

### 3.1. Authentication & Multi-Cloud Synchronization (ការគ្រប់គ្រងគណនី និង Cloud Sync)
- **Offline-First Authentication:** អ្នកប្រើប្រាស់អាចចុះឈ្មោះ ឬ Login ទាំងពេលមាន និងគ្មានអ៊ីនធឺណិត (Local Hashing + SharedPreferences)។ មានជម្រើស Continue as Guest ដោយមិនចាំបាច់មានគណនី។
- **Dual-Layer Cloud Database Sync:**
  1. **External Server Cloud Database:** បញ្ជូន និងទាញយកទិន្នន័យតាមរយៈ HTTP REST JSON Payload រួមមាន Profile, Workouts, Sleep, Nutrition, Daily Steps។
  2. **Cloud Firestore Sync:** ធ្វើសមកាលកម្មទិន្នន័យដោយផ្ទាល់ទៅកាន់ Google Cloud Firestore Collections (`users`, `workouts`, `nutrition`, `sleep`, `daily_steps`)។
- **Tombstone Deletion & Resurrection Prevention:** ដោះស្រាយបញ្ហាទិន្នន័យចាស់រស់ឡើងវិញ (Resurrection Bug) ដោយកត់ត្រា ID ដែលបានលុបក្នុង Tombstone Set ដើម្បីកុំឱ្យទាញយកមកវិញពេល Sync និងផ្ញើបញ្ជាទៅលុបលើ Firestore និង Cloud Server ជារៀងរហូត។

### 3.2. Real-Time Activity & Workout Tracking (ការតាមដានសកម្មភាពហាត់ប្រាណផ្ទាល់)
- **Running & Walking (រត់ និងដើរ):**
  - ប្រើប្រាស់ `LocationTracker` និង `FusedLocationProviderClient`។
  - គណនាចម្ងាយតាមរូបមន្ត Haversine Formula ត្រឹមត្រូវកម្រិតម៉ែត្រ។
  - Pace Engine គណនាល្បឿនរត់ជាក់ស្តែង (Pace min/km ឬ min/mi)។
- **Jump Rope (លោតខ្សែ) ជាមួយ Voice Coach និង Audio Alerts:**
  - ប្រើប្រាស់ Accelerometer Sensor (`JumpDetector`) ជាមួយកម្រិត Threshold ដែលអាចក្រិត (Calibrate) បានតាមតម្រូវការ។
  - **Milestone Alerts:** ពេលលោតដល់ 10, 50, 100, 200, 300, 400, 500+ ដង៖
    - **Voice Coach (Text-To-Speech):** បញ្ចេញសំឡេងអានប្រាប់ចំនួនដង (ឧ. "លោតបាន 100 ដងហើយ" ឬ "100 jumps completed!")។
    - **Loud Chime & Tone:** បន្លឺសំឡេងរោទ៍កម្រិតខ្លាំងតាមរយៈ Media Stream និង Notification Stream។
    - **Haptic Feedback:** ញ័រទ្វេដង (Double-pulse vibration) លើទូរស័ព្ទ។
    - **Celebration Banner:** បង្ហាញផ្ទាំងបដាអបអរសាទរពណ៌បៃតងលើអេក្រង់។
- **Weightlifting (លើកទម្ងន់):**
  - កត់ត្រាលំហាត់តាមក្រុមសាច់ដុំ (Muscle Groups)។
  - គណនាទម្ងន់សរុប (Total Volume / Tonnage in kg) និងចំនួនដងសរុប (Total Reps)។
  - Rest Timer ស្វ័យប្រវត្តរវាង Set នីមួយៗជាមួយប៊ូតុង Skip Rest។
- **WorkoutForegroundService:** សេវា Foreground Service ជាមួយ Notification Lock Screen ការពារកុំឱ្យ Android System បិទការគណនាពេលអ្នកប្រើប្រាស់ចាក់សោអេក្រង់ទូរស័ព្ទ។

### 3.3. Nutrition & Meal Tracking (ការតាមដានអាហារូបត្ថម្ភ)
- កត់ត្រាអាហារតាមពេលនីមួយៗ (Breakfast, Lunch, Dinner, Snack)។
- មូលដ្ឋានទិន្នន័យអាហារ (Food Database) គិតតាមក្រាម ជាមួយកាឡូរី (Calories), ប្រូតេអ៊ីន (Protein), កាបូអ៊ីដ្រាត (Carbs), និងខ្លាញ់ (Fat)។
- របារ Macro Distribution Bar បង្ហាញសមាមាត្រកាឡូរី និងសារធាតុចិញ្ចឹមធៀបនឹងគោលដៅប្រចាំថ្ងៃ។

### 3.4. Sleep Tracking & Sleep Hygiene (ការតាមដានដំណេក)
- កត់ត្រាម៉ោងចូលគេង (Bedtime), ម៉ោងភ្ញាក់ (Wake time), និងរយៈពេលគេងសរុបគិតជានាទី។
- វាយតម្លៃគុណភាពដំណេក (Sleep Quality Rating 1 ដល់ 5 តារា)។
- គណនាពិន្ទុគេង (Sleep Score) ធៀបនឹងស្តង់ដារ ៧ ទៅ ៩ ម៉ោងក្នុងមួយយប់។

### 3.5. Smart Scheduling & Exact Reminders (កាលវិភាគ និងប្រព័ន្ធរំលឹក)
- **Everyday or Select Day:** អ្នកប្រើប្រាស់អាចជ្រើសរើសរំលឹក "រាល់ថ្ងៃ (Everyday)" ឬជ្រើសរើសថ្ងៃជាក់លាក់ក្នុងសប្តាហ៍។
- **Exact Alarm Engine:** ប្រើប្រាស់ `AlarmManager.setAlarmClock()` ដែលជាកម្រិតខ្ពស់បំផុតរបស់ Android ធានាការរោទ៍ចំម៉ោង និងនាទីពិតប្រាកដ ទោះបីជាទូរស័ព្ទស្ថិតក្នុង Doze Mode ឬ Battery Saver ក៏ដោយ។
- **Dedicated Stencil Icon:** បង្កើត `ic_notification.xml` (24x24dp monochrome) ធានាថា Notification បង្ហាញពេញលេញលើ Android គ្រប់ម៉ាកទាំងអស់។
- **Auto-Rescheduling:** `WorkoutReminderReceiver` នឹងរៀបចំម៉ោងរំលឹកសម្រាប់វដ្តបន្ទាប់ដោយស្វ័យប្រវត្តរាល់ពេលដែលម៉ោងរោទ៍បានបន្លឺឡើង។
- **Notification Testing & Permission Alerts:** មានប៊ូតុង "សាកល្បង (Test)" សម្រាប់តេស្ត Notification ភ្លាមៗ និងផ្ទាំងប្រាប់ឱ្យបើក Permission ក្នុង Settings ប្រសិនបើតម្រូវការត្រូវបានបិទ។

### 3.6. In-App OTA Update System (ប្រព័ន្ធអាប់ដែតក្នុង App ផ្ទាល់)
- ពិនិត្យកំណែថ្មីតាមរយៈ `version.json` លើ GitHub Raw និង jsDelivr CDN Mirror។
- បង្ហាញផ្ទាំង Update Modal ជាមួយ Release Notes ជាភាសាខ្មែរ និងអង់គ្លេស។
- Download APK ផ្ទាល់ក្នុង App ជាមួយ Progress Bar និង MB Indicator។
- បើក Android PackageInstaller ស្វ័យប្រវត្តដើម្បីដំឡើងជាន់កំណែចាស់ដោយរក្សាទុកទិន្នន័យទាំងអស់។

### 3.7. Localization & Design System (ភាសា និងរូបរាង)
- **ទ្វេភាសាពេញលេញ (Dual Language):** គាំទ្រភាសាខ្មែរ (Khmer) និងភាសាអង់គ្លេស (English) ដោយប្តូរភ្លាមៗមិនបាច់ Restart App។
- **Luxury Sport UI Theme:** ពណ៌ Neon Lime (`#C6FF00`), Deep Charcoal (`#121212`), Dark Surface (`#1E1E1E`), Cyan, Purple និង Amber Accents។
- **Dark Mode & Light Mode:** គាំទ្រការប្តូរពណ៌ងងឹត និងពន្លឺយ៉ាងរលូន។

---

## 4. Technology Stack (បច្ចេកវិទ្យាប្រើប្រាស់)

| ផ្នែក | បច្ចេកវិទ្យា / បណ្ណាល័យ |
|---|---|
| ភាសាសរសេរកូដ | Kotlin 2.0+ |
| UI Framework | Jetpack Compose (Material 3) |
| Architecture Pattern | MVVM (Model-View-ViewModel) + Offline-First Repository Pattern |
| Async & Reactive Streams | Kotlin Coroutines & StateFlow / SharedFlow |
| Local Database | Room Database (SQLite ORM) ជាមួយ TypeConverters |
| Cloud Database | Google Cloud Firestore & External REST JSON Sync |
| Sensors & Hardware | SensorManager (Accelerometer, Step Counter), FusedLocationProviderClient |
| Voice & Audio | Android TextToSpeech (TTS), RingtoneManager, ToneGenerator |
| Background Processing | Foreground Services, BroadcastReceivers, AlarmManager (setAlarmClock) |
| Image & Media | Coil Compose (Async Image Loading) |
| Network & HTTP | HttpURLConnection ជាមួយ SSL, Redirect Handling & Identity Encoding |
| CI/CD & Build System | Gradle Kotlin DSL (`build.gradle.kts`), GitHub Actions |

---

## 5. Version History (ប្រវត្តិកំណែ)

- **v1.1.2 (Version Code: 13):**
  - បន្ថែមសំឡេង Voice Coach (TTS) និង Loud Chime & Media Tone ពេលលោតខ្សែដល់ Milestone (10, 50, 100, 200, 300+ ដង)។
  - ពង្រឹងប្រព័ន្ធ Notification ជាមួយ `setAlarmClock()`, Dedicated `ic_notification.xml`, និងប៊ូតុងតេស្តសាកល្បង។
  - បន្ថែមជម្រើសរំលឹក "រាល់ថ្ងៃ (Everyday)" លើកាលវិភាគ។
  - ដោះស្រាយបញ្ហាលុបប្រវត្តិហាត់ប្រាណមិនបាត់ (Tombstone Deletion & Firestore sync)។
- **v1.1.1 (Version Code: 12):**
  - បន្ថែម Jumping Milestone Alert បឋម និងជម្រើស Everyday Schedule។
- **v1.1.0 (Version Code: 11):**
  - រៀបចំ Header ជា ២ ជួរស្អាតមិនឱ្យដាច់ប៊ូតុង Theme, ដាក់ប៊ូតុង Save ជាប់នៅបាតក្រោមអចិន្ត្រៃយ៍លើ Schedule, Profile និង Sleep Sheets។
- **v1.0.0 - v1.0.9:**
  - បង្កើតគ្រោងឆ្អឹងដើមនៃ FITHUB App, មុខងារហាត់ប្រាណ, អាហារ, ដំណេក, និងប្រព័ន្ធ Cloud Sync។
