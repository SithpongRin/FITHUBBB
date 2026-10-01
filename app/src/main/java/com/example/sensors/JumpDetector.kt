package com.example.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.calculations.Constants
import kotlin.math.sqrt

class JumpDetector(
    private val context: Context,
    private val onJumpDetected: (Int) -> Unit
) : SensorEventListener {

    enum class JumpState {
        READY, TAKE_OFF, AIRBORNE, LANDING, COMPLETED
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    var takeoffThreshold: Double = Constants.JUMP_TAKEOFF_THRESHOLD_DEFAULT
    var landingThreshold: Double = Constants.JUMP_LANDING_THRESHOLD_DEFAULT

    private var currentState = JumpState.READY
    private var takeoffTimeMs = 0L
    private var lastJumpTimeMs = 0L
    private var currentJumpCount = 0

    // Calibration
    var isCalibrating = false
        private set
    var calibrationJumpsRemaining = 10
        private set
    private val recordedTakeoffMagnitudes = mutableListOf<Double>()
    private val recordedLandingMagnitudes = mutableListOf<Double>()
    var onCalibrationComplete: (() -> Unit)? = null

    fun start(initialCount: Int = 0) {
        currentJumpCount = initialCount
        currentState = JumpState.READY
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    fun startCalibration() {
        isCalibrating = true
        calibrationJumpsRemaining = 10
        recordedTakeoffMagnitudes.clear()
        recordedLandingMagnitudes.clear()
    }

    fun manualIncrement() {
        currentJumpCount++
        onJumpDetected(currentJumpCount)
    }

    fun manualDecrement() {
        if (currentJumpCount > 0) {
            currentJumpCount--
            onJumpDetected(currentJumpCount)
        }
    }

    fun resetCount() {
        currentJumpCount = 0
        onJumpDetected(0)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]
        val magnitude = sqrt((ax * ax + ay * ay + az * az).toDouble())
        val now = System.currentTimeMillis()

        when (currentState) {
            JumpState.READY -> {
                // Look for drop in acceleration indicating takeoff
                if (magnitude <= takeoffThreshold) {
                    currentState = JumpState.TAKE_OFF
                    takeoffTimeMs = now
                }
            }

            JumpState.TAKE_OFF -> {
                val durationMs = now - takeoffTimeMs
                if (durationMs >= Constants.JUMP_MIN_AIRBORNE_MS) {
                    currentState = JumpState.AIRBORNE
                } else if (magnitude > takeoffThreshold + 3.0) {
                    // False trigger, reset
                    currentState = JumpState.READY
                }
            }

            JumpState.AIRBORNE -> {
                val airborneDuration = now - takeoffTimeMs
                if (airborneDuration > Constants.JUMP_MAX_AIRBORNE_MS) {
                    // Too long for a jump, reset
                    currentState = JumpState.READY
                } else if (magnitude >= landingThreshold) {
                    currentState = JumpState.LANDING
                }
            }

            JumpState.LANDING -> {
                if (now - lastJumpTimeMs >= Constants.JUMP_COOLDOWN_MS) {
                    lastJumpTimeMs = now
                    currentJumpCount++
                    onJumpDetected(currentJumpCount)

                    if (isCalibrating) {
                        recordedTakeoffMagnitudes.add(takeoffThreshold)
                        recordedLandingMagnitudes.add(magnitude)
                        calibrationJumpsRemaining--
                        if (calibrationJumpsRemaining <= 0) {
                            isCalibrating = false
                            if (recordedLandingMagnitudes.isNotEmpty()) {
                                landingThreshold = (recordedLandingMagnitudes.average() * 0.85)
                                    .coerceIn(14.0, 25.0)
                            }
                            onCalibrationComplete?.invoke()
                        }
                    }
                }
                currentState = JumpState.READY
            }

            JumpState.COMPLETED -> {
                currentState = JumpState.READY
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
