package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProfileEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSetEntity::class,
        WorkoutPlanEntity::class,
        PlanExerciseEntity::class,
        WorkoutScheduleEntity::class,
        FoodEntity::class,
        NutritionEntryEntity::class,
        SleepEntryEntity::class,
        ActiveSessionEntity::class,
        DailyStepsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class FithubDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutExerciseDao(): WorkoutExerciseDao
    abstract fun workoutSetDao(): WorkoutSetDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun planExerciseDao(): PlanExerciseDao
    abstract fun workoutScheduleDao(): WorkoutScheduleDao
    abstract fun foodDao(): FoodDao
    abstract fun nutritionDao(): NutritionDao
    abstract fun sleepDao(): SleepDao
    abstract fun activeSessionDao(): ActiveSessionDao
    abstract fun dailyStepsDao(): DailyStepsDao

    companion object {
        @Volatile
        private var INSTANCE: FithubDatabase? = null

        val SEED_FOODS = listOf(
            FoodEntity(
                id = "food_white_rice",
                name = "Cooked White Rice",
                servingGrams = 100.0,
                caloriesPer100g = 130.0,
                proteinPer100g = 2.7,
                carbsPer100g = 28.2,
                fatPer100g = 0.3
            ),
            FoodEntity(
                id = "food_brown_rice",
                name = "Cooked Brown Rice",
                servingGrams = 100.0,
                caloriesPer100g = 111.0,
                proteinPer100g = 2.6,
                carbsPer100g = 23.0,
                fatPer100g = 0.9
            ),
            FoodEntity(
                id = "food_rolled_oats",
                name = "Rolled Oats",
                servingGrams = 100.0,
                caloriesPer100g = 389.0,
                proteinPer100g = 16.9,
                carbsPer100g = 66.3,
                fatPer100g = 6.9
            ),
            FoodEntity(
                id = "food_sweet_potato",
                name = "Baked Sweet Potato",
                servingGrams = 100.0,
                caloriesPer100g = 86.0,
                proteinPer100g = 1.6,
                carbsPer100g = 20.1,
                fatPer100g = 0.1
            ),
            FoodEntity(
                id = "food_banana",
                name = "Fresh Banana",
                servingGrams = 100.0,
                caloriesPer100g = 89.0,
                proteinPer100g = 1.1,
                carbsPer100g = 22.8,
                fatPer100g = 0.3
            ),
            FoodEntity(
                id = "food_chicken_breast",
                name = "Grilled Chicken Breast",
                servingGrams = 100.0,
                caloriesPer100g = 165.0,
                proteinPer100g = 31.0,
                carbsPer100g = 0.0,
                fatPer100g = 3.6
            ),
            FoodEntity(
                id = "food_salmon",
                name = "Grilled Salmon",
                servingGrams = 100.0,
                caloriesPer100g = 208.0,
                proteinPer100g = 22.0,
                carbsPer100g = 0.0,
                fatPer100g = 13.0
            ),
            FoodEntity(
                id = "food_egg_whites",
                name = "Egg Whites",
                servingGrams = 100.0,
                caloriesPer100g = 52.0,
                proteinPer100g = 11.0,
                carbsPer100g = 0.7,
                fatPer100g = 0.2
            ),
            FoodEntity(
                id = "food_whole_eggs",
                name = "Whole Eggs",
                servingGrams = 100.0,
                caloriesPer100g = 143.0,
                proteinPer100g = 12.6,
                carbsPer100g = 0.7,
                fatPer100g = 9.5
            ),
            FoodEntity(
                id = "food_tofu",
                name = "Firm Tofu",
                servingGrams = 100.0,
                caloriesPer100g = 76.0,
                proteinPer100g = 8.0,
                carbsPer100g = 1.9,
                fatPer100g = 4.8
            ),
            FoodEntity(
                id = "food_greek_yogurt",
                name = "Greek Yogurt (0%)",
                servingGrams = 100.0,
                caloriesPer100g = 59.0,
                proteinPer100g = 10.0,
                carbsPer100g = 3.6,
                fatPer100g = 0.4
            ),
            FoodEntity(
                id = "food_whey_protein",
                name = "Whey Protein Powder",
                servingGrams = 100.0,
                caloriesPer100g = 380.0,
                proteinPer100g = 78.0,
                carbsPer100g = 5.0,
                fatPer100g = 5.0
            )
        )

        fun getDatabase(context: Context): FithubDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FithubDatabase::class.java,
                    "fithub_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            getDatabase(context).foodDao().insertFoodsIfMissing(SEED_FOODS)
                            getDatabase(context).profileDao().upsertProfile(ProfileEntity())
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
