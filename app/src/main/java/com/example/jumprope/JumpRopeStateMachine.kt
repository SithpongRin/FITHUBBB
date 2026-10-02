package com.example.jumprope

/**
 * State machine states for Jump Rope Goal-Based Training.
 */
enum class JumpRopeWorkoutState {
    IDLE,
    READY,
    WORKOUT,
    PAUSED,
    REST,
    COMPLETED,
    CANCELLED;

    val canCountJumps: Boolean
        get() = this == WORKOUT
}

/**
 * Live state snapshot of an ongoing Jump Rope session.
 */
data class JumpRopeSessionState(
    val config: JumpRopeConfig = JumpRopeConfig(),
    val state: JumpRopeWorkoutState = JumpRopeWorkoutState.IDLE,
    val currentRound: Int = 1,
    val totalRounds: Int = 3,
    val currentRoundJumps: Int = 0,
    val totalSessionJumps: Int = 0,
    val currentRoundElapsedSeconds: Long = 0L,
    val remainingWorkSeconds: Int = 0,
    val remainingRestSeconds: Int = 0,
    val activeElapsedSeconds: Long = 0L,
    val restElapsedSeconds: Long = 0L,
    val totalElapsedSeconds: Long = 0L,
    val caloriesBurned: Double = 0.0,
    val lastAlertedRoundJumpCount: Int = 0,
    val previousStateBeforePause: JumpRopeWorkoutState = JumpRopeWorkoutState.WORKOUT
) {
    val isWorkActive: Boolean
        get() = state == JumpRopeWorkoutState.WORKOUT

    val isRestActive: Boolean
        get() = state == JumpRopeWorkoutState.REST

    val isPaused: Boolean
        get() = state == JumpRopeWorkoutState.PAUSED

    val isFinished: Boolean
        get() = state == JumpRopeWorkoutState.COMPLETED || state == JumpRopeWorkoutState.CANCELLED
}
