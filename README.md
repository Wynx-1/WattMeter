# ⚡ WattMeter

A real-time battery charging monitor for Android that reads actual hardware 
sensors to display live charging wattage, voltage, current, and detailed 
battery statistics.

## Features
- ⚡ Live wattage display
- 🔋 Charging type detection (Slow / Normal / Fast / Super Fast)
- 📊 Voltage, current, temperature, health, source
- 📈 Peak & average wattage per session
- 🕓 Estimated time to full charge
- 📋 Charging session history with date, duration, and stats
- 🔔 System notification when fully charged
- 🌡️ Celsius / Fahrenheit toggle
- 📋 Tap wattage to copy stats to clipboard
- 🌑 Dark minimalist UI

## Screenshots
<p align="center">
  <img src="https://github.com/user-attachments/assets/1bd08bb4-c044-4c30-8106-d785ff67ed9d" width="240" style="margin:10px"/>
  <img src="https://github.com/user-attachments/assets/8fe79e86-e493-40cd-920a-e7425a9d4e80" width="240" style="margin:10px"/>
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/9478d094-90d5-4a9e-957b-7888e646dae2" width="240" style="margin:10px"/>
  <img src="https://github.com/user-attachments/assets/666925c1-3317-49fc-8517-c334d32c08e9" width="240" style="margin:10px"/>
</p>


## Requirements
- Android 8.0+ (API 26)
- Physical device (emulators don't report battery data)

## Tech Stack
- Kotlin
- Android BatteryManager API
- ViewBinding
- Coroutines
- RecyclerView
- SharedPreferences for session history

## Build
1. Clone the repo
2. Open in Android Studio
3. Run on a physical Android device

## License
MIT
