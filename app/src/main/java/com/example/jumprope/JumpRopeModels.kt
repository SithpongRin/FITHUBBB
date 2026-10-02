package com.example.jumprope

/**
 * Built-in training goals for Jump Rope.
 * Goal = Why the user is training.
 */
enum class JumpRopeGoalType(val defaultNameEn: String, val defaultNameKm: String) {
    GENERAL_HEALTH("General Health", "សុខភាពទូទៅ"),
    IMPROVE_ENDURANCE("Improve Endurance", "បង្កើនភាពធន់"),
    WEIGHT_MANAGEMENT("Weight Management", "គ្រប់គ្រងទម្ងន់"),
    FAT_LOSS("Fat Loss", "សម្រកខ្លាញ់"),
    IMPROVE_FITNESS("Improve Fitness", "បង្កើនកាយសម្បទា"),
    CUSTOM("Custom Goal", "គោលដៅផ្ទាល់ខ្លួន");

    companion object {
        fun fromString(value: String?): JumpRopeGoalType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: GENERAL_HEALTH
        }
    }
}

/**
 * Workout target type for Jump Rope.
 * Target = What the user must do during the workout (Reps or Time).
 */
enum class JumpRopeTargetType {
    REPS,
    TIME;

    companion object {
        fun fromString(value: String?): JumpRopeTargetType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: REPS
        }
    }
}

/**
 * Voice Coach language choice.
 */
enum class VoiceLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    KHMER("km", "Khmer (ភាសាខ្មែរ)");

    companion object {
        fun fromCode(code: String?): VoiceLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}

/**
 * Voice Coach counting mode.
 */
enum class VoiceCountingMode {
    EVERY_JUMP,
    MILESTONES;

    companion object {
        fun fromString(value: String?): VoiceCountingMode {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MILESTONES
        }
    }
}

/**
 * Complete training configuration for a Jump Rope session.
 */
data class JumpRopeConfig(
    val goalType: JumpRopeGoalType = JumpRopeGoalType.GENERAL_HEALTH,
    val customGoalName: String = "",
    val targetType: JumpRopeTargetType = JumpRopeTargetType.REPS,
    val targetValue: Int = 100, // Number of jumps (REPS) or work duration in seconds (TIME)
    val rounds: Int = 3,
    val restDurationSeconds: Int = 30,
    val voiceLanguage: VoiceLanguage = VoiceLanguage.ENGLISH,
    val voiceCountingMode: VoiceCountingMode = VoiceCountingMode.MILESTONES,
    val customMilestoneInterval: Int = 50 // Announce every N jumps if in custom milestone mode
) {
    val effectiveGoalNameEn: String
        get() = if (goalType == JumpRopeGoalType.CUSTOM && customGoalName.isNotBlank()) customGoalName else goalType.defaultNameEn

    val effectiveGoalNameKm: String
        get() = if (goalType == JumpRopeGoalType.CUSTOM && customGoalName.isNotBlank()) customGoalName else goalType.defaultNameKm

    fun getDisplayName(isKm: Boolean): String = if (isKm) effectiveGoalNameKm else effectiveGoalNameEn
}

/**
 * Pre-configured presets supplied by FITHUB for built-in recommended goals.
 * Note: These presets provide sustainable workout structures and activity guidance.
 * Presets are modifiable by the user and do not lock them.
 */
object JumpRopePresets {

    fun getPreset(goalType: JumpRopeGoalType): JumpRopeConfig {
        return when (goalType) {
            JumpRopeGoalType.GENERAL_HEALTH -> JumpRopeConfig(
                goalType = JumpRopeGoalType.GENERAL_HEALTH,
                targetType = JumpRopeTargetType.REPS,
                targetValue = 100,
                rounds = 3,
                restDurationSeconds = 30,
                voiceCountingMode = VoiceCountingMode.MILESTONES,
                customMilestoneInterval = 50
            )

            JumpRopeGoalType.IMPROVE_ENDURANCE -> JumpRopeConfig(
                goalType = JumpRopeGoalType.IMPROVE_ENDURANCE,
                targetType = JumpRopeTargetType.TIME,
                targetValue = 180, // 3 minutes
                rounds = 5,
                restDurationSeconds = 60,
                voiceCountingMode = VoiceCountingMode.MILESTONES,
                customMilestoneInterval = 100
            )

            JumpRopeGoalType.WEIGHT_MANAGEMENT -> JumpRopeConfig(
                goalType = JumpRopeGoalType.WEIGHT_MANAGEMENT,
                targetType = JumpRopeTargetType.REPS,
                targetValue = 200,
                rounds = 4,
                restDurationSeconds = 45,
                voiceCountingMode = VoiceCountingMode.MILESTONES,
                customMilestoneInterval = 50
            )

            JumpRopeGoalType.FAT_LOSS -> JumpRopeConfig(
                goalType = JumpRopeGoalType.FAT_LOSS,
                targetType = JumpRopeTargetType.REPS,
                targetValue = 150,
                rounds = 5,
                restDurationSeconds = 30,
                voiceCountingMode = VoiceCountingMode.MILESTONES,
                customMilestoneInterval = 50
            )

            JumpRopeGoalType.IMPROVE_FITNESS -> JumpRopeConfig(
                goalType = JumpRopeGoalType.IMPROVE_FITNESS,
                targetType = JumpRopeTargetType.TIME,
                targetValue = 120, // 2 minutes
                rounds = 4,
                restDurationSeconds = 45,
                voiceCountingMode = VoiceCountingMode.MILESTONES,
                customMilestoneInterval = 50
            )

            JumpRopeGoalType.CUSTOM -> JumpRopeConfig(
                goalType = JumpRopeGoalType.CUSTOM,
                customGoalName = "My Daily Jump Training",
                targetType = JumpRopeTargetType.REPS,
                targetValue = 300,
                rounds = 3,
                restDurationSeconds = 60,
                voiceCountingMode = VoiceCountingMode.MILESTONES,
                customMilestoneInterval = 100
            )
        }
    }
}
