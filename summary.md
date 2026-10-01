# FITHUB - Specification and Architecture Summary

## 1. Product Overview

FITHUB is an offline-first, local-primary personal fitness manager for Android devices. The app operates completely offline without requiring cloud dependencies for primary tracking, database persistence, plan generation, or biometrics analysis.

## 2. Technical Stack and Architecture

- **UI & Presentation:** Jetpack Compose (M3) utilizing an edge-to-edge layout, responsive container dimensions, custom Canvas graphs, and a floating rounded navigation bar.
- **Architecture Pattern:** Model-View-ViewModel (MVVM) + Repository Pattern.
- **Domain Calculations:** Pure, deterministic functions in `com.example.calculations` with zero UI or framework dependencies.
- **Local Storage:** SQLite managed through Android Room Database (`fithub_database`) with KSP.
- **Sensors & Location:** Android SensorManager (linear accelerometer) and Google Play Services Fused Location Provider.
- **Notifications:** Android AlarmManager and NotificationCompat with dedicated notification channel `fithub_workout_reminders`.
- **Styling:** Custom FithubTheme adhering to the reference aesthetic (neon lime `#D4F23A` on charcoal `#121316` surface, rounded cards 24-28dp, high contrast typography).

## 3. Database Schema

1. `profile`: id, fullName, age, biologicalSex, heightCm, weightKg, activityLevel, fitnessGoal, unitSystem, updatedAt.
2. `workouts`: id, type, startTime, endTime, durationSeconds, caloriesBurned, distanceMeters, averagePaceSecPerKm, jumpCount, totalVolumeKg, totalReps, localDate, tzOffsetMinutes, notes, createdAt, updatedAt, deletedAt.
3. `workout_exercises`: id, workoutId, name, muscleGroup, isBodyweight, orderIndex.
4. `workout_sets`: id, exerciseId, workoutId, setIndex, reps, weightKg, completed.
5. `workout_plans`: id, name, daysPerWeek, sessionMinutes, equipment, goal, createdAt.
6. `plan_exercises`: id, planId, dayIndex, orderIndex, name, sets, reps, weightKg, restSeconds, isBodyweight.
7. `workout_schedules`: id, dayOfWeek (0=Sun..6=Sat), timeString, workoutType, planId, enabled, reminderOffsetMinutes.
8. `foods`: id, name, servingGrams, caloriesPer100g, proteinPer100g, carbsPer100g, fatPer100g, isCustom.
9. `nutrition_entries`: id, localDate, mealType, foodName, grams, calories, protein, carbs, fat, completed, createdAt.
10. `sleep_entries`: id, localDate, durationMinutes, bedtime, wakeTime, qualityRating, createdAt.
11. `active_session`: singleton row for crash recovery holding live workout type, elapsed seconds, distance, and calories.

## 4. Calculation Specifications

- **BMR (Mifflin-St Jeor):**
  - Male: `10 * weightKg + 6.25 * heightCm - 5 * age + 5`
  - Female: `10 * weightKg + 6.25 * heightCm - 5 * age - 161`
  - Other: `10 * weightKg + 6.25 * heightCm - 5 * age - 78` (midpoint estimate)
- **TDEE:** `BMR * activityMultiplier` (Sedentary 1.2, Light 1.375, Moderate 1.55, Very Active 1.725, Extra Active 1.9).
- **Calorie Targets:** `TDEE + goalAdjustment` (Weight Management -400, Build Strength +300, Improve Endurance +150, Maintain 0), clamped to a safe floor of 1200 kcal.
- **Protein Targets:** Grams per kg body weight: Build Strength 1.6-2.2, Weight Management 1.6-2.2, Improve Endurance 1.4-1.8, General Fitness 1.2-1.6.
- **Macronutrient Split:** Fat 25% of calories (9 kcal/g), Protein midpoint (4 kcal/g), Carbohydrates remainder (4 kcal/g).
- **Workout Calories:** `kcal = MET * weightKg * hours`.
  - Running MET table: 6.4 km/h -> 6.0, 8.0 km/h -> 8.3, 9.7 km/h -> 9.8, 11.3 km/h -> 11.0, 12.9 km/h -> 11.8, 14.5 km/h -> 12.8.
  - Walking MET table: 3.2 km/h -> 2.8, 4.8 km/h -> 3.5, 5.6 km/h -> 4.3, 6.4 km/h -> 5.0.
  - Jump rope: 11.8. Weightlifting: 3.5.
- **GPS Distance & Filtering:**
  - Haversine distance formula with Earth radius 6,371,000 meters.
  - Rejection filters: accuracy > 20m running / 25m walking, non-increasing timestamps, duplicate coordinates, speeds > 15 m/s running / 5 m/s walking, stationary drift < 2m.
  - Pause/resume clears previous reference point to prevent teleport distance accumulation.
- **Jump Detection:** 5-state machine (READY, TAKE_OFF, AIRBORNE, LANDING, COMPLETED) checking gravity drop below 4.5 m/s^2, airborne duration between 120ms and 900ms, impact spike above 17.5 m/s^2, and 250ms cooldown. Includes 10-jump calibration routine.
- **Streak Calculation:** Evaluated on unique local dates (`YYYY-MM-DD`). Active streak requires a workout today or yesterday. Computes current streak and historical longest streak.

## 5. Known Limitations

- Step counting requires an onboard hardware step counter sensor; otherwise distance and pace derive from GPS coordinates.
- Sensor sensitivity and accelerometer calibration vary across Android phone hardware models; a calibration mode is provided.
- Exact-alarm scheduling (`SCHEDULE_EXACT_ALARM`) may require explicit battery optimization exemption on aggressive OEM Android flavors.
- Calorie burn figures and metabolic targets are estimates based on standard clinical formulas and the Compendium of Physical Activities.
