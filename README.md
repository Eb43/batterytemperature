# Android Battery Temperature – Real-Time Monitoring

📥 **Download:** [batterytemperature.apk](https://github.com/Eb43/batterytemperature/releases/download/v1.0/batterytemperature.apk)

BatteryTemperatureDisplay is an Android app that shows your phone’s **real-time battery temperature**.  
The information is always visible in three places:  

- 📱 On the app’s main screen.  
- 🔔 As a persistent notification.  
- 📊 As an icon in the status bar, even while you use other apps.  

Monitoring your battery temperature helps prevent overheating, extend battery lifespan, and avoid dangerous situations.  

---

## 🔑 Key Features

- Real-time battery temperature display.  
- Minimalistic and intuitive interface.  
- Temperature shown in **status bar, notification, and app screen**.
- Temperature alarm
- Temperaturre logging in CSV file for analysis in Excel or other analytics tool
- Autostart on boot – always running when your phone is on.  
- Exits fully on button press, freeing memory.  
- Extremely low RAM consumption, ideal for older devices.  
- Free to use, no unnecessary permissions.  
- Compatible with **Android 8+**.  

---

## 📊 Practical Uses of Monitoring Battery Temperature

- 🔥 **Overheating detection** during gaming, 4K video recording, or charging.  
  Li-ion batteries degrade when heated over **40 °C** for long periods. An alarm can notify you before overheating causes permanent damage.  

- 🧩 **Prevent solder cracks under phone’s processor.**  
  The battery is the main heat sink for the processor. Repeated overheating cycles weaken solder joints, potentially leading to device failure.  

- ⚠ **Reduce risk of li-ion battery fire.**  
  At **50 °C**, batteries may enter thermal runaway. Setting an alarm helps prevent fire, especially while charging overnight.  

- 📊 **Use old Android as a temperature logger.**  
  Log environment temperature in timestamped CSV format for analysis in Excel or Google Sheets. Useful for 🌱 greenhouses, 🐕 barns, or home monitoring.  

- 🏃 **Heat stroke prevention.**  
  If your phone reaches **40 °C** in your pocket, conditions may cause heat exhaustion.  

- ❄ **Body heat conservation.**  
  Monitor how well your clothing retains warmth in cold weather emergencies.  

- 🚗 **Hot car safety.**  
  Track when parked car interiors reach dangerous levels for children or pets.  

- 🥶 **Food safety monitoring.**  
  Place phone in a cooler during camping or power outages – temperatures above **+4 °C** mean food is unsafe.  

- 🛏 **Bed assessment.**  
  Logging shows if bedding traps too much heat, affecting sleep quality.  

- 🏠 **Insulation problems.**  
  Detect heat leaks by comparing temperature variations across rooms.  

- 🌡 **“Poor man’s thermometer.”**  
  Use your phone as an ambient temperature monitor if no thermometer is available.  

---

## 📷 Screenshots

<div>
<img alt="android battery thermometer" src="https://raw.githubusercontent.com/Eb43/batterytemperature/refs/heads/main/fastlane/metadata/android/en-US/images/phoneScreenshots/1.jpg" style="width:300px;"/>
<img alt="android battery thermometer" src="https://raw.githubusercontent.com/Eb43/batterytemperature/refs/heads/main/fastlane/metadata/android/en-US/images/phoneScreenshots/4.jpg" style="width:300px; margin-left:30px;"/>
</div>

<div>
<img alt="android battery thermometer" src="https://raw.githubusercontent.com/Eb43/batterytemperature/refs/heads/main/fastlane/metadata/android/en-US/images/phoneScreenshots/2.jpg" style="width:300px; margin-left:30px;"/>
<img alt="android battery thermometer" src="https://raw.githubusercontent.com/Eb43/batterytemperature/refs/heads/main/fastlane/metadata/android/en-US/images/phoneScreenshots/3.jpg" style="width:300px; margin-left:30px;"/>
</div>

---

## ⚙ Technical Information

Battery temperature is retrieved from Android system files:  

/sys/class/power_supply/battery/temp

The `temp` file reports values in **tenths of degrees Celsius**. Example:  
350 → 35.0 °C


### ✅ Accuracy
- Values are read directly from **internal battery sensors**.  
- Small variations may occur due to:  
  - Manufacturer sensor calibration.  
  - Ambient temperature and airflow.  
  - CPU/GPU heat transfer near the battery.  

### 📐 Why Temperature Changes Gradually
- Smartphone batteries weigh **30–40 g**, so their thermal mass makes temperature rise/fall slowly.  
- Influencing factors: charging rate, CPU/GPU load, ambient temperature.  


---

## 📝 Summary

The **Battery Temperature Display** app is both a **safety tool** and a **multi-purpose temperature monitoring solution**.  
It helps users:  

- 🔋 Protect their smartphone’s battery health.  
- 🛡 Reduce fire risks.  
- 📊 Repurpose old phones into environment temperature loggers.  
