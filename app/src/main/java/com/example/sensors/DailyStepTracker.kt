package com.example.sensors

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.sqrt

class DailyStepTracker(
    private val context: Context,
    private val onStepsUpdated: (steps: Int, distanceMeters: Double, calories: Double) -> Unit = { _, _, _ -> }
) : SensorEventListener {

    private val sensorManager = try {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    } catch (_: Throwable) {
        null
    }
    private val stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val prefs: SharedPreferences =
        context.getSharedPreferences("fithub_step_prefs", Context.MODE_PRIVATE)

    private val _isTrackingEnabled = MutableStateFlow(
        prefs.getBoolean("step_tracking_enabled", false)
    )
    val isTrackingEnabled: StateFlow<Boolean> = _isTrackingEnabled.asStateFlow()

    private val _todaySteps = MutableStateFlow(
        prefs.getInt(todayStepsKey(), 0)
    )
    val todaySteps: StateFlow<Int> = _todaySteps.asStateFlow()

    private val _goalSteps = MutableStateFlow(
        prefs.getInt("step_goal", 8000)
    )
    val goalSteps: StateFlow<Int> = _goalSteps.asStateFlow()

    private val _distanceMeters = MutableStateFlow(0.0)
    val distanceMeters: StateFlow<Double> = _distanceMeters.asStateFlow()

    private val _caloriesBurned = MutableStateFlow(0.0)
    val caloriesBurned: StateFlow<Double> = _caloriesBurned.asStateFlow()

    // Accelerometer fallback step filter
    private var lastAccMagnitude = 9.8f
    private var lastStepTimestamp = 0L

    init {
        recalculateMetrics(_todaySteps.value)
        if (_isTrackingEnabled.value) {
            registerListener()
        }
    }

    private fun todayDateString(): String = LocalDate.now().toString()
    private fun todayStepsKey(): String = "steps_${todayDateString()}"
    private fun todayBaselineKey(): String = "baseline_${todayDateString()}"

    fun setTrackingEnabled(enabled: Boolean) {
        _isTrackingEnabled.value = enabled
        prefs.edit().putBoolean("step_tracking_enabled", enabled).apply()

        if (enabled) {
            registerListener()
        } else {
            unregisterListener()
        }
    }

    private fun registerListener() {
        val manager = sensorManager ?: return
        try {
            // Prefer hardware low-power step counter
            if (stepCounterSensor != null) {
                manager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI)
            } else if (stepDetectorSensor != null) {
                manager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI)
            } else if (accelerometerSensor != null) {
                manager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
        } catch (_: SecurityException) {
            // Runtime permission not granted yet, try fallback to accelerometer
            try {
                if (accelerometerSensor != null) {
                    manager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL)
                }
            } catch (_: Throwable) {}
        } catch (_: Throwable) {}
    }

    private fun unregisterListener() {
        try {
            sensorManager?.unregisterListener(this)
        } catch (_: Throwable) {}
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!_isTrackingEnabled.value || event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val totalHardwareSteps = event.values[0].toInt()
                handleHardwareStepCounter(totalHardwareSteps)
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                if (event.values[0] == 1.0f) {
                    incrementSteps(1)
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                handleAccelerometerSteps(event.values)
            }
        }
    }

    private fun handleHardwareStepCounter(totalHardwareSteps: Int) {
        val today = todayDateString()
        val savedBaseline = prefs.getInt(todayBaselineKey(), -1)

        val baseline = if (savedBaseline == -1 || savedBaseline > totalHardwareSteps) {
            // New day or phone rebooted: set new baseline
            prefs.edit().putInt(todayBaselineKey(), totalHardwareSteps).apply()
            totalHardwareSteps
        } else {
            savedBaseline
        }

        val calculatedTodaySteps = max(0, totalHardwareSteps - baseline)
        updateSteps(calculatedTodaySteps)
    }

    private fun handleAccelerometerSteps(values: FloatArray) {
        val x = values[0]
        val y = values[1]
        val z = values[2]
        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

        val delta = magnitude - lastAccMagnitude
        lastAccMagnitude = magnitude

        val now = System.currentTimeMillis()
        // Simple peak detector with 300ms cooldown (walking cadence: ~100-120 steps/min)
        if (delta > 2.5f && (now - lastStepTimestamp > 320L)) {
            lastStepTimestamp = now
            incrementSteps(1)
        }
    }

    fun incrementSteps(delta: Int) {
        val newSteps = _todaySteps.value + delta
        updateSteps(newSteps)
    }

    private fun updateSteps(steps: Int) {
        _todaySteps.value = steps
        prefs.edit().putInt(todayStepsKey(), steps).apply()
        recalculateMetrics(steps)
        onStepsUpdated(steps, _distanceMeters.value, _caloriesBurned.value)
    }

    private fun recalculateMetrics(steps: Int) {
        // Average stride length = ~0.76 meters (76 cm)
        val distance = steps * 0.762
        _distanceMeters.value = distance

        // Approximately 0.042 kcal per step for an average adult
        val calories = steps * 0.042
        _caloriesBurned.value = calories
    }

    fun setGoal(goal: Int) {
        _goalSteps.value = goal
        prefs.edit().putInt("step_goal", goal).apply()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
