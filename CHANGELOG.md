# Changelog

## 1.1.0 — 2026-11-11
- **Engine sound follows braking and stopping right away.** Before, the sound kept going for up to
  ~3 s after the vehicle had stopped, because three smoothing stages (GPS filter, speed glide and
  engine spool-down) each slowed the fall. Now:
  - lower GPS speeds are applied immediately (only increases are still filtered);
  - between GPS fixes an ongoing deceleration is followed along its measured slope instead of
    waiting for the next fix, and the fall uses a ~60 ms response;
  - off the throttle the engine RPM follows the speed down within ~0.1 s (turbines used to take
    2.6 s); a throttle blip still spools down naturally.
- Speeding up stays smooth, without a surge on every GPS fix.
- **New sound: Big Twin Classic** — an old-school 1340 cc 45° V-twin with a low, hunting idle,
  open pipes, valvetrain clatter and a 4-speed gearbox. It short-shifts around 3,000 rpm, and every
  gear change has a throttle cut, a gearbox clunk and pops on the overrun.
- **New inline-four family** (6 gears, quick-shifter upshifts with a short ignition cut):
  - **Inline-4 250** — quarter-litre four to 17,000 rpm, intake howl, hard rev limiter.
  - **Inline-4 400** — more body than the 250, to 16,000 rpm.
  - **Inline-4 600** — middleweight supersport howl to 16,000 rpm.
  - **Inline-4 1000** — litre superbike: deep bark down low, howl to 14,000 rpm, loud pops.
  - **Supercharged 1000** — superbike exhaust plus a supercharger whine that rises with rpm and
    boost, and a fluttering chirp when the throttle closes.
- **Gear shifts when testing standing still:** holding the throttle button for more than a second
  on a piston engine now pulls away through the gears (with the shift sound), and releasing it
  coasts back down to N. A short blip still revs in neutral. Before, the gear always stayed at N
  until the GPS speed passed 2.5 km/h. Jet and electric sounds are unchanged.
- **Lighter home screen:** the engine row now shows only 4 shortcuts (the selected sound and the
  ones you used recently, else one of each family) plus a **More sounds** card that opens the full
  list. The sounds screen groups the list by family: jet engines, electric motors, V-twins, inline
  fours, two-strokes.
- Sound engine: geared sounds can now set their shift points (`gearRpmLow` / `gearRpmHigh`) and a
  gear-change time (`shiftSeconds`); piston sounds gain idle lope, idle misfires, valve clatter,
  shift clunk, shift dip, supercharger whine and blow-off chirp. Existing sounds are unchanged.

## 1.0.0 — 2026-11-05 (rilis publik pertama / first public release)
- First public release on GitHub. Includes all features of the internal 0.1.0 build below.
- Package name is now `com.logicsekai.omoz` (Omo Ecosystem by Logic Sekai).
- **App update:** checks GitHub Releases about twice a day, notifies you about a new version, and
  downloads and installs it in-app (SHA-256, package name and version are checked first).
  New "App update" screen with automatic checks on/off.
- **Support OmoZ** screen: GitHub Sponsors, Ko-fi, Trakteer, source code, bug reports and sharing.
- About: links to support, update, source code and the license.
- License changed from MIT to **AGPL-3.0-or-later** with additional terms, plus a commercial license
  for sponsors at the Komersial tier (see `COMMERCIAL-LICENSE.md`).
- Part of the Omo Ecosystem by Logic Sekai; not affiliated with OMOWAY.

## 0.1.0 — 2026-10-24
- First release: Jet Turbine, Jet Whine, Jet Hum, Bullet Train Motor, V-Twin Cruiser, Inline-4 Sport, 2-Stroke Racer.
- GPS-driven RPM with virtual gearbox, hold-to-throttle button and notification "Rev" action.
- Foreground service that keeps running with the app closed or swiped away.
- Bluetooth auto start/stop for registered devices.
- Settings: background, battery optimization, OEM autostart screens, wake lock, GPS rate, audio quality, auto-stop.
- English + Indonesian UI.
