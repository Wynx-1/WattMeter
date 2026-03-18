package com.example.wattmeter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.example.wattmeter.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.content.Intent
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var lastStats: BatteryStats? = null
    private var isCelsius = true
    private var fullyChargedNotified = false
    private val CHANNEL_ID = "wattmeter_channel"
    private val NOTIF_ID = 1

    // Session tracking
    private var sessionStartTime = 0L
    private var sessionStartPercent = 0
    private var sessionWattageReadings = mutableListOf<Double>()
    private var sessionPeakWatts = 0.0
    private var wasCharging = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle system insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = systemBars.top + 24,
                bottom = systemBars.bottom + 24,
                left = systemBars.left + 24,
                right = systemBars.right + 24
            )
            insets

        }

        // Tap wattage to copy stats
        binding.tvWattage.setOnClickListener {
            lastStats?.let { stats ->
                val text = """
                    WattMeter Stats:
                    Wattage: ${"%.1f".format(stats.wattage)}W
                    Voltage: ${"%.2f".format(stats.voltage)}V
                    Current: ${"%.0f".format(stats.currentMa)}mA
                    Battery: ${stats.batteryPercent}%
                    Temperature: ${stats.temperatureCelsius}°C
                    Health: ${stats.health}
                    Source: ${stats.chargingSource}
                    Peak: ${"%.1f".format(stats.peakWattage)}W
                    Charging: ${stats.chargingType.label}
                """.trimIndent()

                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("WattMeter Stats", text))
                Toast.makeText(this, "Stats copied to clipboard!", Toast.LENGTH_SHORT).show()
            }
        }
        binding.tvHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        binding.tvTemperature.setOnClickListener {
            isCelsius = !isCelsius
            lastStats?.let { updateUI(it) }
        }
        createNotificationChannel()
        requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        startMonitoring()    }

    private fun startPulsingDot() {
        binding.viewTypeDot.animate()
            .scaleX(1.8f)
            .scaleY(1.8f)
            .alpha(0.3f)
            .setDuration(700)
            .withEndAction {
                binding.viewTypeDot.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(700)
                    .withEndAction { startPulsingDot() }
                    .start()
            }
            .start()
    }

    private fun stopPulsingDot() {
        binding.viewTypeDot.animate().cancel()
        binding.viewTypeDot.scaleX = 1f
        binding.viewTypeDot.scaleY = 1f
        binding.viewTypeDot.alpha = 1f
    }

    private fun startMonitoring() {
        lifecycleScope.launch {
            while (isActive) {
                val stats = BatteryMonitor.getStats(this@MainActivity)
                lastStats = stats
                updateUI(stats)

                // Session tracking
                if (stats.isCharging && !wasCharging) {
                    // Just plugged in
                    sessionStartTime = System.currentTimeMillis()
                    sessionStartPercent = stats.batteryPercent
                    sessionWattageReadings.clear()
                    sessionPeakWatts = 0.0
                }

                if (stats.isCharging) {
                    if (stats.wattage > 0) {
                        sessionWattageReadings.add(stats.wattage)
                        if (stats.wattage > sessionPeakWatts) sessionPeakWatts = stats.wattage
                    }
                }

                if (!stats.isCharging && wasCharging) {
                    // Just unplugged — save session
                    if (sessionStartTime > 0 && sessionWattageReadings.isNotEmpty()) {
                        val durationMinutes = ((System.currentTimeMillis() - sessionStartTime) / 60000).toInt()
                        val avgWatts = sessionWattageReadings.average()
                        SessionManager.addSession(
                            this@MainActivity,
                            ChargingSession(
                                date = sessionStartTime,
                                fromPercent = sessionStartPercent,
                                toPercent = stats.batteryPercent,
                                durationMinutes = durationMinutes,
                                avgWatts = avgWatts,
                                peakWatts = sessionPeakWatts
                            )
                        )
                    }
                    sessionStartTime = 0L
                }

                wasCharging = stats.isCharging

                if (stats.batteryPercent == 100 && stats.isCharging) {
                    showFullyChargedNotification()
                } else if (!stats.isCharging) {
                    fullyChargedNotified = false
                }

                delay(1500)
            }
        }
    }

    private fun showFullyChargedNotification() {
        if (!fullyChargedNotified) {
            fullyChargedNotified = true
            sendFullyChargedNotification()
        }
    }

    private fun updateUI(stats: BatteryStats) {
        binding.tvWattage.text = String.format(Locale.US, "%.1fW", stats.wattage)

        binding.tvChargingType.text = stats.chargingType.label
        binding.tvChargingType.setTextColor(stats.chargingType.color.toInt())
        binding.viewTypeDot.setBackgroundColor(stats.chargingType.color.toInt())

        binding.tvBatteryPercent.text = "${stats.batteryPercent}%"
        binding.progressBattery.progress = stats.batteryPercent

        binding.tvVoltage.text = String.format(Locale.US, "%.2f V", stats.voltage)
        binding.tvCurrent.text = String.format(Locale.US, "%.0f mA", stats.currentMa)

        val temp = if (isCelsius) stats.temperatureCelsius
        else stats.temperatureCelsius * 9 / 5 + 32
        val tempUnit = if (isCelsius) "°C" else "°F"
        binding.tvTemperature.text = String.format(Locale.US, "%.1f%s", temp, tempUnit)
        binding.tvTempLabel.text = "TEMP $tempUnit"
        binding.tvHealth.text = stats.health
        binding.tvSource.text = stats.chargingSource
        binding.tvPeak.text = String.format(Locale.US, "%.1fW", stats.peakWattage)
        binding.tvAverage.text = String.format(Locale.US, "%.1fW", stats.averageWattage)
        binding.tvEfficiency.text = "${stats.efficiencyScore}%"
        if (stats.isCharging && stats.minutesToFull > 0) {
            val hours = stats.minutesToFull / 60
            val minutes = stats.minutesToFull % 60
            binding.tvTimeToFull.text =
                if (hours > 0) "${hours}h ${minutes}m to full" else "${minutes}m to full"
            binding.tvTimeToFull.alpha = 1f
        } else if (!stats.isCharging) {
            binding.tvTimeToFull.text = ""
        } else {
            binding.tvTimeToFull.text = "Calculating…"
            binding.tvTimeToFull.alpha = 0.5f
        }

        binding.tvChargingIcon.text = if (stats.isCharging) "⚡" else "🔋"
    }
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "WattMeter Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Battery charging alerts"
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun sendFullyChargedNotification() {
        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("Battery Fully Charged ⚡")
            .setContentText("Your battery is at 100%. You can unplug now.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(NOTIF_ID, notification)
    }
}
