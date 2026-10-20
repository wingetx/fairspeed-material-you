# FairSpeed Material You

An **unofficial, work-in-progress** Android UI customization of [FairSpeed](https://git.naxod.com/luca/fairspeed) by Luca (Naxod).

The original project is licensed under **GNU AGPL v3 or later**, and this modified version is distributed under the same license. See [LICENSE](LICENSE).

## Changes (2026-10-20)

- Use Android 12+ Material You / dynamic wallpaper colors, respecting the existing Light / Dark / System theme setting.
- Replace fixed yellow accent backgrounds with Material 3 theme colors, including matching contrast colors for text and icons.
- Add space between radio controls and labels (12 dp) and between the extended-test checkbox and its label (8 dp).
- Use an independent Android application ID (`io.github.wingetx.fairspeed`) so the unofficial build can coexist with FairSpeed from F-Droid.
- Remove the upstream release signing configuration and key from this publication; anyone building a release must provide their own signing credentials.
- Add a persistent Accent Color setting: Material You, five muted presets, and a six-digit RGB hex option, independent of light/dark mode.
- Replace the original yellow logo with a tintable vector gauge and a neutral launcher icon; unused yellow bitmaps have been removed.
- Improve status/navigation bar icon contrast in light/dark mode.

## Development status

**Development preview.** The APK and existing JVM tests build successfully. No signed release APK is published yet. This is not affiliated with or endorsed by the upstream FairSpeed developer.

## Build

The project uses the Gradle wrapper, Android SDK and JDK 17+ (check Android Gradle Plugin compatibility). Build a debug APK with `./gradlew assembleDebug`. Android debug builds use Android's generated debug signing key. Do not use upstream release signing material. Release signing must be configured independently.

## Upstream and attribution

- Upstream: https://git.naxod.com/luca/fairspeed
- Source snapshot based on upstream commit: `5bd3bcee34d698970edb97e42ec7da912d17f957` (2026-10-17).
- Original author: Luca / Naxod; original source headers and license retained.
- UI changes: modified 2026-10-20 for this independent repository.

The GNU AGPLv3-or-later terms and original attribution remain in effect. Contributions to this fork must comply with the license.
