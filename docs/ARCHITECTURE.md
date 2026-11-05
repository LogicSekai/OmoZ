# Architecture

```
                 ┌──────────────────────── app (Android) ───────────────────────────┐
  GPS ─────────▶ │ SpeedTracker ─┐                                                  │
                 │               ▼                                                  │
  UI throttle ─▶ │ EngineRuntime ─▶ EngineService (foreground) ── tick 20 Hz ──┐    │
  Notif "Rev" ─▶ │      ▲   (StateFlows)        │                              │    │
                 │      └───── publish ─────────┘                              ▼    │
  BT ACL ──────▶ │ BluetoothAutoReceiver ─ start/stop        EngineAudioPlayer      │
                 │                                           (AudioTrack thread)    │
                 └───────────────────────────────────────────────┬──────────────────┘
                                                                  │ uses
                 ┌──────────────── engine-core (pure Kotlin/JVM) ─▼──────────────────┐
                 │ EngineSimulator: speed + throttle → rpm, load, gear               │
                 │ EngineSound / EngineSynth: rpm + load → PCM samples               │
                 │   JetTurbineSynth · PistonEngineSynth(PistonConfig) · dsp/*       │
                 └────────────────────────────────────────────────────────────────────┘
```

## Modules

| Module | Type | Responsibility |
|---|---|---|
| `engine-core` | Kotlin/JVM library | All sound synthesis and RPM simulation. No Android APIs → unit-testable, renderable to WAV on a PC. |
| `app` | Android application | Service lifecycle, GPS, Bluetooth, permissions/OEM quirks, Compose UI. |

## Key flows

**Starting the engine** — `EngineService.start(context, reason, fromForeground)` calls `startForegroundService`. The service enters the foreground with type `mediaPlayback` (+ `location` when allowed — Android 14 forbids the location type from the background without *Allow all the time*). If Android 12+ refuses a background start, a one-tap notification is shown instead.

**Tick loop (50 ms)** — reads speed (`SpeedTracker`), throttle (`EngineRuntime.throttleInput()`), runs `EngineSimulator.update`, writes `rpm/load/volume` into volatile fields of `EngineAudioPlayer`, publishes UI state, refreshes the notification every 2 s, and handles auto-stop.

**Audio thread** — renders 256-frame blocks with the current `EngineSynth` and blocks on `AudioTrack.write`. Parameters are ramped inside the synth per block, so updates at 20 Hz are click-free. Switching sounds fades the old synth for one block. No audio focus is requested, so music/navigation keep playing.

**Bluetooth auto-start** — `BluetoothAutoReceiver` is registered in the manifest for `ACL_CONNECTED/DISCONNECTED` (exempt from the implicit-broadcast ban). It checks the registered device list in `SettingsRepository` and starts/stops the service. Sessions started manually are never stopped by a disconnect.

**Settings** — `SettingsRepository` (SharedPreferences → `StateFlow<AppSettings>`). The service collects it, so changing the engine, sample rate or GPS mode while running applies immediately.

## Why these choices

- **Procedural synthesis instead of recordings**: tiny APK, infinitely smooth RPM, no licensing problems, easy to tweak with numbers.
- **`LocationManager` instead of FusedLocationProvider**: works without Google Play Services.
- **SharedPreferences instead of DataStore**: synchronous read inside the Bluetooth receiver.
- **No DI framework**: `AppGraph` is a 10-line service locator that new contributors can read at a glance.
