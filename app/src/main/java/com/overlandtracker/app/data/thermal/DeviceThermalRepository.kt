package com.overlandtracker.app.data.thermal

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager

class DeviceThermalRepository(
    private val context: Context
) {
    fun readThermalState(): DeviceThermalState {
        val batteryTemperatureCelsius = runCatching { readBatteryTemperatureCelsius() }.getOrNull()
        val thermalLevelFromSystem = runCatching { readSystemThermalLevel() }.getOrNull()

        val fallbackLevel = thermalLevelFromSystem ?: when {
            batteryTemperatureCelsius == null -> ThermalLevel.UNKNOWN
            batteryTemperatureCelsius < 40f -> ThermalLevel.NORMAL
            batteryTemperatureCelsius < 45f -> ThermalLevel.WARM
            else -> ThermalLevel.HOT
        }

        return DeviceThermalState(
            batteryTemperatureCelsius = batteryTemperatureCelsius,
            qualitativeStatus = fallbackLevel,
            isSupported = batteryTemperatureCelsius != null || thermalLevelFromSystem != null
        )
    }

    private fun readBatteryTemperatureCelsius(): Float? {
        val batteryStatusIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val rawTemperature = batteryStatusIntent?.getIntExtra(
            BatteryManager.EXTRA_TEMPERATURE,
            Int.MIN_VALUE
        ) ?: return null

        if (rawTemperature == Int.MIN_VALUE) {
            return null
        }

        val celsius = rawTemperature / 10f
        return celsius.takeIf { it in 0f..80f }
    }

    private fun readSystemThermalLevel(): ThermalLevel? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null
        }

        val powerManager = context.getSystemService(PowerManager::class.java) ?: return null
        return powerManager.currentThermalStatus.toThermalLevel()
    }
}

data class DeviceThermalState(
    val batteryTemperatureCelsius: Float?,
    val qualitativeStatus: ThermalLevel,
    val isSupported: Boolean
)

enum class ThermalLevel {
    NORMAL,
    WARM,
    HOT,
    CRITICAL,
    EMERGENCY,
    SHUTDOWN,
    UNKNOWN
}

private fun Int.toThermalLevel(): ThermalLevel {
    return when (this) {
        PowerManager.THERMAL_STATUS_NONE,
        PowerManager.THERMAL_STATUS_LIGHT -> ThermalLevel.NORMAL
        PowerManager.THERMAL_STATUS_MODERATE -> ThermalLevel.WARM
        PowerManager.THERMAL_STATUS_SEVERE -> ThermalLevel.HOT
        PowerManager.THERMAL_STATUS_CRITICAL -> ThermalLevel.CRITICAL
        PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalLevel.EMERGENCY
        PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalLevel.SHUTDOWN
        else -> ThermalLevel.UNKNOWN
    }
}
