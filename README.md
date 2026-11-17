# PUBG Ping Master

Starter Android/Kotlin project for KlencodIDE.

## What it measures
- TCP connection latency to Cloudflare (1.1.1.1:443) and Google (8.8.8.8:443).
- Displays the lower successful TCP connection time, average absolute change between consecutive successful samples (jitter estimate), and failed test percentage.

## Important limitation
This is **not PUBG Mobile's in-game ping**. PUBG does not expose a public third-party API for reading the live game-server latency. TCP connection time to public endpoints is only a network diagnostic indicator. Use PUBG's own in-game network indicator for actual game ping.

## Open/build
1. Extract the ZIP.
2. Open the project folder in KlencodIDE.
3. Allow Gradle sync and install Android SDK Platform 35 if requested.
4. Build the APK and install it on the phone.

The project has not been compiled in this environment. If KlencodIDE reports a Gradle or SDK error, share the complete log so it can be corrected.

## Build compatibility fix (v1.0.1)
Java source/target compatibility and Kotlin jvmTarget are both set to 17 to resolve the reported JVM-target mismatch. compileSdk/targetSdk are set to 34 and Build Tools 34.0.0 to match the supplied KlencodIDE build environment. This updated project still needs a fresh build run to verify compilation.
