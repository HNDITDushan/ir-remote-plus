# IR Remote Plus

An Android app that uses a phone's built-in IR blaster (e.g. Redmi Note 14) to control devices that stock remote apps don't list, such as Inovax fans and DB (Dunham-Bush) fan coil ACs.

## Features
- **AC remote** – power, mode, temperature, fan speed and swing. Protocols: AUX / YKR (Electra), Gree, Midea / Coolix.
- **Custom remotes** – buttons with your own codes (NEC, Symphony, Coolix, Pronto hex or raw timings).
- **Code finder** – steps through Symphony and NEC codes to discover the codes of unknown fans and devices.

The phone needs an IR blaster (`android.hardware.consumerir`). Phones without one, like the Huawei nova 5T, can't send IR.

## Build
```bash
./gradlew assembleDebug
```
The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Requires the Android SDK (compileSdk 35) and JDK 17+.

Protocol timings are based on [IRremoteESP8266](https://github.com/crankyoldgit/IRremoteESP8266).
