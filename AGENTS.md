# AGENTS.md

Guidance for AI agents working in the GymGym repository.

## Project

GymGym is a native Android app (Kotlin + Jetpack Compose) that counts gym reps
from the phone camera using CameraX + ML Kit Pose Detection. It is local-only:
no backend, no accounts, no network permission. Workout history/stats/profile
are persisted on-device with Room and DataStore.

Build: `./gradlew assembleDebug`

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
