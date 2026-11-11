# Adding an engine sound

You do **not** need a phone to design a sound. Everything happens in `engine-core` and can be rendered to WAV on your computer.

## 1. Piston engine (no DSP code needed)

Open `engine-core/src/main/kotlin/com/logicsekai/omoz/engine/EngineCatalog.kt` and add a sound with `pistonSound(...)`:

```kotlin
val vespaTwoStroke: EngineSound = pistonSound(
    EngineInfo(
        id = "vespa_two_stroke",          // lowercase snake_case, NEVER change after release
        name = "Vespa 2T",
        description = "Classic scooter putt-putt.",
        kind = EngineKind.PISTON_2_STROKE,
        idleRpm = 1500, redlineRpm = 7000,
        gears = 1,                         // 1 = CVT / automatic scooter
        spoolUpSeconds = 0.25f, spoolDownSeconds = 0.4f,
        specs = "1 cyl · 150cc · 2-stroke",
        author = "your-github-name",
    ),
    PistonConfig(
        cylinders = 1, strokes = 2,
        firingOffsets = floatArrayOf(0f),
        idleRpm = 1500, redlineRpm = 7000,
        pulseWidth = 0.3f,
        resonances = listOf(Resonance(250f, 1.8f, 0.7f), Resonance(900f, 3f, 0.3f)),
        lowpassIdleHz = 1200f, lowpassRedlineHz = 4000f,
        roughness = 0.2f, pulseNoise = 0.4f, hiss = 0.06f,
        drive = 1.5f, crackle = 0.2f,
    ),
)
```

Then add it to `all`.

### What the numbers do

| Parameter | Effect |
|---|---|
| `cylinders`, `strokes`, `firingOffsets` | Rhythm. Offsets are fractions of one engine cycle (720° for 4-stroke). Even firing = `i / cylinders`. A 45° V-twin is `0, 315/720`. A crossplane I4 is uneven. |
| `pulseWidth` | Length of each exhaust pulse. Short = sharp/clicky, long = buzzy. |
| `resonances` | Exhaust/body colour. Low Hz + high gain = deep & boomy; mid Hz = bark; high Hz = rasp. |
| `lowpassIdleHz` / `lowpassRedlineHz` | Brightness at idle vs. redline. |
| `roughness` | Random cycle-to-cycle variation. 0.05 = smooth I4, 0.25 = lumpy single. |
| `pulseNoise`, `hiss` | Combustion texture and intake/mechanical noise. |
| `drive` | Saturation at full load. |
| `crackle` | Over-run pops when the throttle closes at high RPM. |
| `trackedResonance*` | Expansion-chamber resonance that follows RPM, strongest in the power band (2-strokes). |
| `outputGain` | Loudness trim — aim for a similar level to the existing sounds. |
| `idleLope`, `missChance` | Old big-twin idle: slow wander of the idle speed and occasional weak firings. Both fade out off idle. |
| `valveTick` | Valvetrain / primary-chain clatter, added after the exhaust filters so it stays crisp. |
| `shiftClunk`, `shiftDip` | Gearbox clunk and exhaust dip on a gear change. Needs `shiftSeconds` in `EngineInfo`. |
| `superchargerWhine`, `superchargerRatio`, `blowOff` | Supercharger whine (pitch = crank speed × ratio, louder with rpm and load) and a chirp when the load collapses at speed. |

Gearbox settings live in `EngineInfo`: `gears`, `gearRpmLow` / `gearRpmHigh` (rpm after and before an
upshift, 0 = idle, 1 = redline) and `shiftSeconds` (clutch-in time with a throttle cut; 0 = instant).
See `bigTwinClassic` in `EngineCatalog` for an example that uses all of them.

## 2. Something completely different

Implement `EngineSynth` yourself (see `JetTurbineSynth` for a noise + oscillator design). Rules:

- `render()` must not allocate — preallocate filters and buffers in the constructor.
- Smooth `params.rpm/load/volume` across the block with `Ramp`.
- Keep output within `[-1, 1]`; use `softClip`.

## 3. Listen and test

```bash
./gradlew :engine-core:renderPreviews   # → engine-core/build/previews/<id>.wav
./gradlew :engine-core:test             # checks: finite, not clipping, not silent, unique id
```

The preview scenario is idle → two throttle blips → accelerate to 90 km/h → coast.

## 4. App polish (optional)

- Name/description translations: add `engine_<id>_name` and `engine_<id>_desc` to `app/src/main/res/values/strings.xml` and `values-in/strings.xml`.
- Card colour & illustration: add your id to `engineStyle()` in `app/.../ui/components/EngineArt.kt` (otherwise a palette colour is chosen automatically).

Open a PR with the WAV preview attached. 🎉
