package com.example.wattmeter

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlin.math.abs

data class BatteryStats(
    val wattage: Double,
    val voltage: Double,
    val currentMa: Double,
    val batteryPercent: Int,
    val chargingType: ChargingType,
    val minutesToFull: Int,
    val isCharging: Boolean,
    val temperatureCelsius: Float,
    val health: String,
    val chargingSource: String,
    val peakWattage: Double,
    val averageWattage: Double,
    val efficiencyScore: Int
)

enum class ChargingType(val label: String, val color: Long) {
    NOT_CHARGING("Not Charging", 0xFF6B6B6B),
    SLOW("Slow Charging", 0xFFFF9800),
    NORMAL("Normal Charging", 0xFF4CAF50),
    FAST("Fast Charging", 0xFF2196F3),
    SUPER_FAST("Super Fast Charging", 0xFFE040FB)
}

object BatteryMonitor {

    private var peakWattage = 0.0
    private var wattageReadings = mutableListOf<Double>()

    fun getStats(context: Context): BatteryStats {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val currentMicroAmps = batteryManager.getLongProperty(
            BatteryManager.BATTERY_PROPERTY_CURRENT_NOW
        )

        val voltageMv = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPercent = if (scale > 0) (level * 100) / scale else 0

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val currentMa = abs(currentMicroAmps / 1000.0)
        val voltageV = voltageMv / 1000.0
        val wattage = if (voltageV > 0 && currentMa > 0) voltageV * (currentMa / 1000.0) else 0.0

        // Peak wattage
        // Peak wattage
        if (wattage > peakWattage) peakWattage = wattage

        // Track readings for average
        if (isCharging && wattage > 0) wattageReadings.add(wattage)

        // Average wattage
        val averageWattage = if (wattageReadings.isNotEmpty())
            wattageReadings.average() else 0.0

        // Charging efficiency (watts per percent charged, scaled to 0-100)
        val efficiency = if (averageWattage > 0 && voltageV > 0) {
            val maxPossible = voltageV * 5.0
            ((averageWattage / maxPossible) * 100).coerceIn(0.0, 100.0).toInt()
        } else 0

        // Temperature (tenths of degrees celsius)
        val tempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val temperatureCelsius = tempRaw / 10.0f

        // Health
        val healthRaw = batteryIntent?.getIntExtra(
            BatteryManager.EXTRA_HEALTH,
            BatteryManager.BATTERY_HEALTH_UNKNOWN
        ) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val health = when (healthRaw) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }

        // Charging source
        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val chargingSource = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Adapter"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Unplugged"
        }

        val chargingType = when {
            !isCharging -> ChargingType.NOT_CHARGING
            wattage >= 45.0 -> ChargingType.SUPER_FAST
            wattage >= 18.0 -> ChargingType.FAST
            wattage >= 7.5 -> ChargingType.NORMAL
            wattage > 0.0 -> ChargingType.SLOW
            else -> ChargingType.NOT_CHARGING
        }

        val minutesToFull = if (isCharging && batteryPercent < 100) {
            val remainingCapacityUah = batteryManager.getLongProperty(
                BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER
            )
            if (currentMa > 50 && remainingCapacityUah > 0) {
                val estimatedFullUah = if (batteryPercent > 0) {
                    (remainingCapacityUah * 100L) / batteryPercent
                } else 0L
                val remainingToChargeMah = (estimatedFullUah - remainingCapacityUah) / 1000.0
                if (remainingToChargeMah > 0 && currentMa > 0) {
                    ((remainingToChargeMah / currentMa) * 60).toInt()
                } else -1
            } else {
                if (currentMa > 50) {
                    val remainingPercent = 100 - batteryPercent
                    val estimatedMah = 3500.0 * remainingPercent / 100.0
                    ((estimatedMah / currentMa) * 60).toInt()
                } else -1
            }
        } else -1

        return BatteryStats(
            wattage = wattage,
            voltage = voltageV,
            currentMa = currentMa,
            batteryPercent = batteryPercent,
            chargingType = chargingType,
            minutesToFull = minutesToFull,
            isCharging = isCharging,
            temperatureCelsius = temperatureCelsius,
            health = health,
            chargingSource = chargingSource,
            peakWattage = peakWattage,
            averageWattage = averageWattage,
            efficiencyScore = efficiency
        )
    }
}