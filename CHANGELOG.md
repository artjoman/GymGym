# Changelog

User-facing release notes for GymGym, newest first. Updated on every version
bump with the changes made since the previous release (see AGENTS.md).

## 2.0.4 (versionCode 13)

- **Notifications now open the right screen.** Tapping a reminder deep-links to
  where it's about — body-measurement → Profile, workout reminder → Next
  Mission, cycle-progress → Workout Plans — whether the app was closed, in the
  background, or already open. Unknown links fall back to Home, and no duplicate
  screens are stacked.
- Added a **mic mute** button during a workout: tap it to stop listening for
  voice commands. The choice sticks between exercises and sessions.
- **Every exercise is now testable from the library** — non-AI moves and your
  own custom exercises open a manual session where you enter the reps yourself.
  AI-counted moves are labelled **AI Count**.
- History now shows each exercise's **reps done vs. planned** (e.g. 4/4), so it
  lines up with the workout percentage.
- The home screen now shows a **Last workout** card — name, date, completion %
  and duration at a glance (tap it to open Statistics).
- Fixed a crash that could occur right after finishing a set.

## 2.0.3 (versionCode 12)

- **Custom (non-AI) exercises now run during a workout**, set by set: each set
  shows the target reps (editable), a **Finish set** button, then the rest timer —
  the same rhythm as AI-counted exercises.
- Workouts now record a **completion %** — how many reps you actually did versus
  the planned reps × sets — and that % is shown on each finished block of the
  progress bar.
- Every workout now stores its **true total duration** (including rests, excluding
  pauses), so manual-only workouts get a duration too.
- **Weekly Schedule** lets you assign a **weekday** to each workout; the next
  mission follows the weekday order and the progress bar shows each day.
- All recovery timers (between workouts, sets, and exercises) are now set in
  **seconds, in steps of 10**.
- **Deleting** a plan, workout, exercise, custom exercise, or recording now asks
  for confirmation first.

## 2.0.2 (versionCode 11)

- Added a **Skip rest** button so you can jump to the next set/exercise without
  waiting out the rest timer.
- The plan editor now has an explicit **Save plan** button at every level (cycle
  and workout), so edits aren't lost by tapping Back.
- **History** and **Statistics** are now one screen with tabs.
- Charts show **min/max values** on the axis and the date range, and are
  **tappable** to view the raw data as a table.

## 2.0.1 (versionCode 10)

- Fixed: **Start** and **Swap** on the Next Mission screen now actually launch the
  workout (the active plan wasn't being read).
- Home is cleaner: removed the redundant "Choose your exercise" buttons — browse
  and test exercises in the Exercise Library instead (Auto-detect moved there too).
- New: **body-parameter trends** in Stats — see how your weight and measurements
  change over time. Included in backup export/import.

## 2.0.0 (versionCode 9)

GymGym grows from a rep counter into a full workout planner:

- **Exercise library** — browse ~40 exercises grouped by muscle group, test the
  AI-counted ones, and add your own custom exercises.
- **Programs** — six ready-made programs (Full Body Beginner/Standard, Home,
  Street, Weighted Base, Push/Pull/Legs); pick one to set it as your active plan.
- **Workout plans, restructured** — build Plan → Cycle → Workout → Exercise, set
  an end date, and mark one plan active.
- **Home dashboard** — a Next Mission card with cycle progress; open it to Start,
  Swap or Skip the next workout. Smart Cycle or Weekly Schedule.
- **Run a whole workout** — rest timers between sets and exercises with spoken
  "next set / start the set" cues; durations now exclude paused time.
- **Profile** — training mode, recovery timeouts (between workouts/sets/
  exercises), centimeters/inches, and body measurements (weight, arm, leg, chest,
  shoulders, calves, waist) with history.
- **History** — completed workouts with average form % and duration, on top of
  per-exercise history.
- **Reminders** (Motivation & Control) — upcoming/missed workout reminders,
  a low-cycle-average alert, and a body-measurement reminder.
- **Expert Support** — a preview of AI Coach and human coaching tiers
  (coming soon).
- Everything localized across all seven supported languages.

## 1.2.4 (versionCode 8)

- Smaller download and a leaner on-device footprint (enabled R8 code shrinking,
  obfuscation and resource shrinking for release builds).

## 1.2.3 (versionCode 7)

- Redesigned the on-camera pose skeleton as a soft neon overlay that follows the
  selected color scheme (was flat green).

## 1.2.2 (versionCode 6)

- Form check, part 2: flags wobbly/unstable reps ("Steady" cue), a form
  **Sensitivity** setting (Lenient / Standard / Strict), and a form-quality
  trend chart in Stats.

## 1.2.1 (versionCode 5)

- Real-time form check: grades each rep for depth ("Go deeper") and tempo
  ("Slow down"), tracks good vs. total reps with an optional **Strict counting**
  mode, and shows a form score in Stats and session detail.
- More hands-free voice commands: start/stop recording and switch camera.

## 1.2.0 (versionCode 4)

- Appearance overhaul: 14 accent color schemes, choosable or custom-uploaded
  backgrounds shown on every screen, and redesigned buttons. Removed the menu
  mascot animation.

## 1.1.0 (versionCode 3)

- Improved push-up counting (more reliable from a side/floor angle).
- New Plank hold timer with voice start/stop, and timed exercises inside workout
  plans.

## 1.0.1 (versionCode 2)

- Initial Play Store release: camera rep counting for squat, push-up, pull-up
  and dumbbell press; auto-detect; workout plans; history & stats; on-device
  workout recording; backup/restore; hands-free voice control; countdown, voice
  cues and arcade set-complete callouts.
