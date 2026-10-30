# Donut Tracker 1.0 — Minecraft 26.2 (Fabric)

A client-side tracker for manually recording wins/losses and cautiously detecting clearly worded game chat messages.

## Target toolchain
- Minecraft Java 26.2
- Java 25 JDK
- Fabric Loader 0.19.3
- Fabric API 0.161.0+26.2
- Fabric Loom 1.17.x
- Gradle 9.5.1

Fabric's 26.2 guidance recommends Loom 1.17 and Gradle 9.5.1. Use the latest stable Fabric Loader. See https://fabricmc.net/2026/06/15/262.html and https://docs.fabricmc.net/develop/porting/.

## Build
1. Install a Java 25 JDK.
2. Install Gradle 9.5.1 or use an IDE with Gradle support.
3. Open this folder in IntelliJ IDEA and allow Gradle to sync.
4. Run the `build` Gradle task.
5. The output JAR will be in `build/libs/` (choose the JAR without `-sources`).

## Use
- Press Right Shift to open the tracker.
- Enter an amount and press `+ WIN` or `- LOSS`.
- Toggle automatic detection in the GUI.
- Stats are stored in `.minecraft/config/donut-tracker.properties`.

## Important compatibility note
This is an updated 26.2 source project, not a compiled or tested JAR. Minecraft 26.2 changed modding/toolchain details; the Java/Fabric screen and event APIs used by this starter may need small code adjustments during the first Gradle compile. Share the first build error and I can help fix it. Do not install an unverified build into your main instance.

## Automatic detection caveat
DonutSMP chat wording varies. Version 1.0 only counts clearly worded messages with a win/loss keyword and a visible amount. Ambiguous messages are ignored. Send example messages (hide usernames) to tune the parser.

## Privacy
No networking code, HTTP clients, account/session-token access, browser access, or payment functionality. It reads game chat delivered to the Minecraft client and writes totals to a local config file.
