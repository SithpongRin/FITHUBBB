package com.example

import com.example.jumprope.*
import com.example.localization.Localization
import com.example.localization.StringKey
import org.junit.Assert.*
import org.junit.Test

class JumpRopeGoalSystemTest {

    @Test
    fun testGoalTypesAndPresets() {
        val allGoals = JumpRopeGoalType.values()
        assertEquals(6, allGoals.size)
        assertTrue(allGoals.contains(JumpRopeGoalType.GENERAL_HEALTH))
        assertTrue(allGoals.contains(JumpRopeGoalType.IMPROVE_ENDURANCE))
        assertTrue(allGoals.contains(JumpRopeGoalType.WEIGHT_MANAGEMENT))
        assertTrue(allGoals.contains(JumpRopeGoalType.FAT_LOSS))
        assertTrue(allGoals.contains(JumpRopeGoalType.IMPROVE_FITNESS))
        assertTrue(allGoals.contains(JumpRopeGoalType.CUSTOM))

        // Preset checks
        val healthPreset = JumpRopePresets.getPreset(JumpRopeGoalType.GENERAL_HEALTH)
        assertEquals(JumpRopeTargetType.REPS, healthPreset.targetType)
        assertEquals(100, healthPreset.targetValue)
        assertEquals(3, healthPreset.rounds)
        assertEquals(30, healthPreset.restDurationSeconds)

        val endurancePreset = JumpRopePresets.getPreset(JumpRopeGoalType.IMPROVE_ENDURANCE)
        assertEquals(JumpRopeTargetType.TIME, endurancePreset.targetType)
        assertEquals(180, endurancePreset.targetValue) // 3 minutes
        assertEquals(5, endurancePreset.rounds)
        assertEquals(60, endurancePreset.restDurationSeconds)

        val fatLossPreset = JumpRopePresets.getPreset(JumpRopeGoalType.FAT_LOSS)
        assertEquals(JumpRopeTargetType.REPS, fatLossPreset.targetType)
        assertEquals(150, fatLossPreset.targetValue)
        assertEquals(5, fatLossPreset.rounds)
        assertEquals(30, fatLossPreset.restDurationSeconds)

        val weightPreset = JumpRopePresets.getPreset(JumpRopeGoalType.WEIGHT_MANAGEMENT)
        assertEquals(JumpRopeTargetType.REPS, weightPreset.targetType)
        assertEquals(200, weightPreset.targetValue)
        assertEquals(4, weightPreset.rounds)
        assertEquals(45, weightPreset.restDurationSeconds)

        val fitnessPreset = JumpRopePresets.getPreset(JumpRopeGoalType.IMPROVE_FITNESS)
        assertEquals(JumpRopeTargetType.TIME, fitnessPreset.targetType)
        assertEquals(120, fitnessPreset.targetValue)
        assertEquals(4, fitnessPreset.rounds)
        assertEquals(45, fitnessPreset.restDurationSeconds)
    }

    @Test
    fun testCustomGoalNamingAndDisplay() {
        val customConfig = JumpRopeConfig(
            goalType = JumpRopeGoalType.CUSTOM,
            customGoalName = "Morning Jump Blast",
            targetType = JumpRopeTargetType.REPS,
            targetValue = 600,
            rounds = 6,
            restDurationSeconds = 45
        )

        assertEquals("Morning Jump Blast", customConfig.getDisplayName(isKm = false))
        assertEquals("Morning Jump Blast", customConfig.getDisplayName(isKm = true))

        val presetConfig = JumpRopeConfig(
            goalType = JumpRopeGoalType.IMPROVE_ENDURANCE
        )
        assertEquals("Improve Endurance", presetConfig.getDisplayName(isKm = false))
        assertEquals("បង្កើនភាពធន់", presetConfig.getDisplayName(isKm = true))
    }

    @Test
    fun testTargetTypesFromString() {
        assertEquals(JumpRopeTargetType.REPS, JumpRopeTargetType.fromString("reps"))
        assertEquals(JumpRopeTargetType.TIME, JumpRopeTargetType.fromString("TIME"))
        assertEquals(JumpRopeTargetType.REPS, JumpRopeTargetType.fromString("unknown"))

        assertEquals(VoiceCountingMode.EVERY_JUMP, VoiceCountingMode.fromString("EVERY_JUMP"))
        assertEquals(VoiceCountingMode.MILESTONES, VoiceCountingMode.fromString("milestones"))
        assertEquals(VoiceCountingMode.MILESTONES, VoiceCountingMode.fromString("invalid"))

        assertEquals(VoiceLanguage.ENGLISH, VoiceLanguage.fromCode("en"))
        assertEquals(VoiceLanguage.KHMER, VoiceLanguage.fromCode("km"))
        assertEquals(VoiceLanguage.ENGLISH, VoiceLanguage.fromCode("unknown"))
    }

    @Test
    fun testStateMachineRestSensorIsolation() {
        assertTrue(JumpRopeWorkoutState.WORKOUT.canCountJumps)
        assertFalse(JumpRopeWorkoutState.REST.canCountJumps)
        assertFalse(JumpRopeWorkoutState.PAUSED.canCountJumps)
        assertFalse(JumpRopeWorkoutState.READY.canCountJumps)
        assertFalse(JumpRopeWorkoutState.IDLE.canCountJumps)
        assertFalse(JumpRopeWorkoutState.COMPLETED.canCountJumps)
        assertFalse(JumpRopeWorkoutState.CANCELLED.canCountJumps)
    }

    @Test
    fun testCumulativeCountAcrossMultipleRounds() {
        var currentRound = 1
        val totalRounds = 3
        val targetRepsPerRound = 100
        var currentRoundJumps = 0
        var totalSessionJumps = 0
        var state = JumpRopeWorkoutState.WORKOUT

        // Round 1
        for (i in 1..100) {
            assertTrue(state.canCountJumps)
            currentRoundJumps++
            totalSessionJumps++
        }
        assertEquals(100, currentRoundJumps)
        assertEquals(100, totalSessionJumps)

        // Target reached for round 1 -> transition to REST
        if (currentRoundJumps >= targetRepsPerRound) {
            state = JumpRopeWorkoutState.REST
        }
        assertEquals(JumpRopeWorkoutState.REST, state)

        // Sensor activity during REST must NOT increment target
        assertFalse(state.canCountJumps)
        // Simulate accidental sensor event during REST
        if (state.canCountJumps) {
            currentRoundJumps++
            totalSessionJumps++
        }
        assertEquals(100, currentRoundJumps)
        assertEquals(100, totalSessionJumps)

        // Rest completed -> Next Round starts
        currentRound++
        currentRoundJumps = 0
        state = JumpRopeWorkoutState.WORKOUT
        assertEquals(2, currentRound)
        assertEquals(0, currentRoundJumps)
        assertEquals(100, totalSessionJumps) // Session count preserved!

        // Round 2
        for (i in 1..100) {
            currentRoundJumps++
            totalSessionJumps++
        }
        assertEquals(100, currentRoundJumps)
        assertEquals(200, totalSessionJumps)

        // Rest completed -> Round 3 starts
        currentRound++
        currentRoundJumps = 0
        state = JumpRopeWorkoutState.WORKOUT
        assertEquals(3, currentRound)

        // Round 3
        for (i in 1..100) {
            currentRoundJumps++
            totalSessionJumps++
        }
        assertEquals(100, currentRoundJumps)
        assertEquals(300, totalSessionJumps)

        // Final round target reached
        val isFinalRound = currentRound >= totalRounds
        assertTrue(isFinalRound)
        state = JumpRopeWorkoutState.COMPLETED
        assertEquals(JumpRopeWorkoutState.COMPLETED, state)
        assertEquals(300, totalSessionJumps)
    }

    @Test
    fun testMilestoneCalculation() {
        val interval = 50
        fun isMilestone(jump: Int): Boolean {
            return jump == 10 || jump == 50 || (jump > 0 && jump % interval == 0)
        }

        assertFalse(isMilestone(1))
        assertFalse(isMilestone(9))
        assertTrue(isMilestone(10))
        assertFalse(isMilestone(25))
        assertTrue(isMilestone(50))
        assertTrue(isMilestone(100))
        assertTrue(isMilestone(150))
        assertTrue(isMilestone(500))
        assertFalse(isMilestone(125))
    }

    @Test
    fun testLocalizationParityAndZeroEmojiPolicy() {
        fun isEmoji(codePoint: Int): Boolean {
            return (codePoint in 0x1F600..0x1F64F) || // Emoticons
                   (codePoint in 0x1F300..0x1F5FF) || // Misc Symbols & Pictographs
                   (codePoint in 0x1F680..0x1F6FF) || // Transport and Map
                   (codePoint in 0x1F1E6..0x1F1FF) || // Regional indicator (Flags)
                   (codePoint in 0x2600..0x26FF) ||   // Misc symbols
                   (codePoint in 0x2700..0x27BF) ||   // Dingbats
                   (codePoint in 0x1F900..0x1F9FF) || // Supplemental Symbols
                   (codePoint in 0x1FA00..0x1FAFF)    // Extended Pictographs
        }

        for (key in StringKey.values()) {
            val en = Localization.getString(key, com.example.localization.AppLanguage.ENGLISH)
            val km = Localization.getString(key, com.example.localization.AppLanguage.KHMER)

            assertNotNull("Missing English translation for $key", en)
            assertNotNull("Missing Khmer translation for $key", km)
            assertFalse("English translation is blank for $key", en.isBlank())
            assertFalse("Khmer translation is blank for $key", km.isBlank())

            // ZERO EMOJI POLICY verification
            en.codePoints().forEach { cp ->
                assertFalse("Emoji found in English string for key $key: '$en' (code point: 0x${Integer.toHexString(cp)})", isEmoji(cp))
            }
            km.codePoints().forEach { cp ->
                assertFalse("Emoji found in Khmer string for key $key: '$km' (code point: 0x${Integer.toHexString(cp)})", isEmoji(cp))
            }
        }

        // Verify specific Jump Rope keys exist and are non-empty
        val jrKeys = listOf(
            StringKey.JR_GOAL_TITLE,
            StringKey.JR_GOAL_RECOMMENDED,
            StringKey.JR_GOAL_CUSTOM,
            StringKey.JR_GOAL_HEALTH,
            StringKey.JR_GOAL_ENDURANCE,
            StringKey.JR_GOAL_WEIGHT,
            StringKey.JR_GOAL_FAT_LOSS,
            StringKey.JR_GOAL_FITNESS,
            StringKey.JR_TARGET_TITLE,
            StringKey.JR_TARGET_REPS,
            StringKey.JR_TARGET_TIME,
            StringKey.JR_ROUNDS_TITLE,
            StringKey.JR_REST_TITLE,
            StringKey.JR_VOICE_COACH_TITLE,
            StringKey.JR_HEALTH_GUIDANCE_NOTE
        )

        for (key in jrKeys) {
            assertTrue(Localization.getString(key, com.example.localization.AppLanguage.ENGLISH).isNotBlank())
            assertTrue(Localization.getString(key, com.example.localization.AppLanguage.KHMER).isNotBlank())
        }
    }
}
