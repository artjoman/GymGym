# AGENTS.md

Guidance for AI agents working in the GymGym repository.

## Project

GymGym is a native Android app (Kotlin + Jetpack Compose) that counts gym reps
from the phone camera using CameraX + ML Kit Pose Detection. No backend, no
accounts. Workout history/stats/profile are persisted on-device with Room and
DataStore.

Two product flavors: **free** (shows AdMob ads; has INTERNET) and **paid**
(no ads, offline — the transitive INTERNET permission is stripped in
`app/src/paid/AndroidManifest.xml`). Variants: `freeDebug`, `paidDebug`,
`freeRelease`, `paidRelease` (default is `freeDebug`).

Build: `./gradlew assembleFreeDebug` (or `assemblePaidDebug`; `assembleDebug`
builds both).

## Ads / build variables

All ad tuning lives in `app/build.gradle.kts`:

- **`AD_INTERVAL_MS`** (`defaultConfig`, currently `300000L` = 5 min) — minimum
  time between interstitials. **Change the ad frequency here.**
- **`ADS_ENABLED`** — per-flavor `buildConfigField` (`free` = true, `paid` =
  false). The paid flavor also omits the `play-services-ads` dependency, so it
  never links the SDK.
- **`AD_INTERSTITIAL_UNIT_ID`** (`defaultConfig`) — currently Google's TEST
  interstitial id. **Replace with your real AdMob ad-unit id before release.**
- **AdMob App ID** — `com.google.android.gms.ads.APPLICATION_ID` meta-data in
  `app/src/free/AndroidManifest.xml` (currently Google's TEST app id). **Replace
  with your real AdMob app id.**

Ads are shown only at "open a workout" boundaries (the `startExercise` /
`startPlan` / `startAuto` functions in `MainActivity`, via `AdManager`), never
mid-set. Real ad code lives in `app/src/free/.../ads/`; `app/src/paid/.../ads/`
is a no-op. Keep TEST ids until a real AdMob account is configured (with a
privacy policy + consent), or real ads on non-test devices risk a policy strike.

## Workflow: commit & push on milestones

After every **major milestone**, once the build succeeds (`./gradlew
assembleDebug` passes), **commit and push** the work to `origin`:

```
./gradlew assembleDebug && git add -A && git commit -m "<message>" && git push
```

Rules:
- Only commit when the build is green. Never commit a broken build.
- One commit per milestone, with a clear message describing what shipped.
- **Do not add Claude (or any AI) as a commit co-author.** No `Co-Authored-By`
  trailer, no "Generated with" footer.
- Commit the exported Room schema under `app/schemas/` — it is the migration
  history and must stay in version control.

A "major milestone" means a self-contained feature or phase is complete and
verified, not every small edit.

## Workflow: release notes on every version bump

`CHANGELOG.md` holds user-facing release notes, newest first. **Whenever you
bump the version** (`versionCode` / `versionName` in `app/build.gradle.kts`),
add a new section for that version listing everything that changed since the
previous release — in the **same commit** as the bump, so the notes never fall
behind.

Rules:
- Keep entries **user-facing** (what a Play Store reader cares about), not
  internal refactors, test-only changes, or version bumps themselves.
- One section per released version, headed `## <versionName> (versionCode <n>)`.
- The changes for a version are everything committed since the previous bump; a
  quick `git log <prev-bump>..HEAD` is the source of truth.
- This doubles as the Play Console "What's new" copy.

## Architecture notes

- Single `MainViewModel` (`ui/MainViewModel.kt`) holds session, settings,
  history, stats, and profile state.
- `GymGymApp` / `AppContainer` is a hand-rolled service locator (no Hilt).
- Navigation is a `NavHost` in `MainActivity.kt`.
- Rep counting: `RepCounter` implementations wrap the generic
  `RepStateMachine`; add a new exercise by adding a counter + an `Exercise`
  enum entry.
- Pose plausibility gating lives in `pose/PoseValidator.kt` to reject ML Kit's
  phantom skeletons on background objects.
