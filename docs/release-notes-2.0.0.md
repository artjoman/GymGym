# GymGym 2.0.0 — Release notes

**Version:** 2.0.0 (versionCode 9) · free flavor
**Artifacts:**
- Play upload: `app/build/outputs/bundle/freeRelease/app-free-release.aab` (signed)
- Sideload/QA: `app/build/outputs/apk/free/release/app-free-release.apk` (signed)
- Signed with the release key (CN=Artjoms Petrovs, Projectorum) via `keystore.properties`.

---

## Play Store "What's new" (short — paste into the release)

GymGym 2.0 turns your AI rep counter into a full workout planner:

• Exercise library + your own custom exercises
• 6 ready‑made programs (Full Body, Home, Street, Push/Pull/Legs…)
• Build plans with cycles & workouts
• Home dashboard: your Next Mission + cycle progress
• Guided workouts with rest timers and voice cues
• Profile: recovery settings, units & body measurements
• Workout history with form scores
• Reminders to keep you on track

Now in 7 languages.

*(~470 characters — within the 500‑char Play limit.)*

## Full release notes (for the store description / blog)

**New**
- **Exercise library** — ~40 exercises grouped by muscle group; test the
  AI‑counted moves and add your own custom exercises.
- **Programs** — six preset programs (Full Body Beginner/Standard, Home Workout,
  Street Workout, Weighted Base, Push/Pull/Legs). One tap sets it as your active
  plan with a 3‑month end date.
- **Workout plans** rebuilt as **Plan → Cycle → Workout → Exercise**, with an end
  date and a single active plan.
- **Home dashboard** — a **Next Mission** card and cycle‑progress bar; Start, Swap
  or Skip your next workout. Choose **Smart Cycle** or a **Weekly Schedule**.
- **Guided workout runs** — rest timers between sets and exercises with spoken
  "next set / start the set" cues; recorded durations exclude paused time.
- **Profile** — training mode, recovery timeouts (between workouts / sets /
  exercises), centimeters/inches, and body measurements (weight, arm, leg, chest,
  shoulders, calves, waist) with history.
- **History** — completed workouts with average form % and duration.
- **Motivation & Control** — upcoming/missed workout reminders, a low‑cycle‑average
  alert, and a body‑measurement reminder.
- **Expert Support** — a preview of the AI Coach and human‑coaching tiers
  (coming soon).

**Also**
- Fully localized across English, Spanish, French, Latvian, Russian, Chinese
  (Simplified) and Arabic.
- The paid flavor stays fully offline (no internet permission); the free flavor
  keeps ads unchanged.

## Known follow‑ups (not blockers)
- Manual/custom (non‑AI) exercises are listed and plannable but are skipped when
  running a workout — full manual execution is the next increment.
- Exercise **names** are English‑first (UI chrome is fully translated);
  localizing the ~30 catalog names is tracked.
- The AI Coach itself is deferred (Expert Support shows it as "coming soon").

## Suggested QA before publishing
1. Fresh install + upgrade install both migrate cleanly (Room v4→v8).
2. Pick a program → run its first workout end‑to‑end → confirm it appears in
   History and updates cycle progress.
3. Toggle a reminder in Settings → Motivation & Control and confirm delivery.
