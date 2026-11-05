# Design system

Inspired by the reference shots (playful learning app): soft peach background, white cards with warm shadows, one strong orange accent, cool blue secondary.

## Colour tokens (`ui/theme/Color.kt`)

| Token | Hex | Use |
|---|---|---|
| Orange | `#FF7A4F` | Primary actions, throttle, selected rows, drawer |
| Sky | `#CAF4FF` | Unselected list rows, soft secondary surfaces |
| Blue | `#255FA0` | Text on Sky, progress |
| Navy | `#3C467D` | Turbine card, deep accents |
| Peach | `#FBF3EF` | App background |
| Ink / InkMuted | `#262A41` / `#8D90A5` | Text |

## Type

**Poppins** (bundled, OFL) — Regular, Medium, SemiBold, Bold. The reference's *Corsa Grotesk* is commercial, so Poppins (its pair typeface) is used everywhere.

## Shapes & elevation

- Cards 24–28 dp radius, buttons 16 dp, pills 12 dp.
- Shadows are tinted (orange/peach), never grey-black, `shadow(10.dp)`.

## Components (`ui/components/`)

`OmoCard`, `SquareIconButton` (menu/back/close), `EngineCard`, `EngineBanner` (status), `AccentLinkRow` (orange left bar), `SpeedWidget`, `RpmWidget`, `ThrottleDock` (hold-to-throttle), `Segmented`, `SettingRow`/`ToggleRow`/`SliderRow`, `StatusChip`.

Illustrations are drawn in code (`EngineArt`), so they scale perfectly and add no image weight. Please keep new artwork original.
