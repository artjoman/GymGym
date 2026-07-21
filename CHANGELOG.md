# Changelog

User-facing release notes for GymGym, newest first. Updated on every version
bump with the changes made since the previous release (see AGENTS.md).

## 2.1.0 (versionCode 18)

- **Achievements.** 20 badges earned from what you already train — workout
  totals, day streaks, rep milestones, leg-day consistency, full-body coverage,
  100% workouts, completed cycles, and early-morning / late-night sessions.
  Find them under **Profile → Achievements**, with the earned date on the ones
  you have and a progress count on the ones you don't.
- Every badge is its own hand-drawn medal starring the GymGym mascot, played for
  laughs — jelly legs the day after leg day, juggling a week's worth of
  dumbbells, a crown and a throne of weight plates. Locked ones sit in
  greyscale until you earn them.
- Earning one pops an **arcade celebration** wherever you are in the app — after
  a workout, or the moment a measurement streak lands. Turn it off under
  **Settings → Achievements** if you'd rather it stayed quiet.
- Badges you already earned before this update are credited on first launch,
  without a wall of pop-ups.
- **Backup now includes your workout history** (and your achievement dates), so
  moving to a new device keeps Statistics and your badges intact. Older backup
  files still import.

## 2.0.8 (versionCode 17)

- New **app icon** — the GymGym mascot.
- The app now opens in the **yellow** colour scheme by default, matching the icon.
  (If you've already picked a scheme, yours is kept.)
- **Current mission** is rebuilt for long cycles: the cycle is a compact bar strip
  with the weekday above each bar, and you swipe between workouts instead of
  scrolling one huge card.
- New workout actions on that screen: **Start** and **Skip** for the current
  workout, and **Make next** to bring any future or skipped workout forward —
  the rest keep their order and each takes on its new slot's weekday.
- **Skip** now asks for confirmation before skipping.

## 2.0.7 (versionCode 16)

- Weekdays are shown wherever a workout appears when **Weekly schedule** is on —
  above the progress bars on Home, and after the workout name in the cycle card,
  the execution screen, Statistics → Cycles and Statistics → Workouts. Switching
  to Smart cycle hides them; switching back restores them.
- Editing a cycle now lists each workout's actual exercises (name, reps × sets)
  instead of just a count.
- Clearer saving: **Save workout** returns to the cycle, **Save cycle** to the
  plan, and **Save plan** to your plans list.
- Only your **first** workout plan is activated automatically; later plans are
  added without taking over. The per-plan Start button is gone — start workouts
  from Current mission.
- Names (display name, custom exercise, plan, cycle, workout) now auto-capitalize.
- "Form feedback" is now **Exercise quality assistant**.
- The 🔴 Rec button in the Exercise library matches the Test button's styling.

## 2.0.6 (versionCode 15)

- Completed cycles are now kept forever: editing or deleting a workout plan no
  longer erases past cycles from Statistics → Cycles or Home.
- Cycle % now uses hierarchical averaging (exercise → workout → cycle) instead of
  pooling all reps, so a skipped workout counts as a clean 0%.
- Redesigned the workout execution screen ("Let's go!"): it shows the whole
  current cycle expanded with the next workout highlighted; the active cycle no
  longer appears in Statistics → Cycles.
- Profile training modes each show a one-line description; the weekday picker no
  longer shows day icons in Smart cycle mode, and per-workout weekdays are kept
  when you switch modes.
- Workout-plan editor: the weekday selector only appears in Weekly Schedule mode,
  the "Any" option is gone, days already used in the cycle are highlighted (still
  reusable), and Edit Cycle shows each workout's weekday.
- Exercise library: 🔴 Rec button restyled; Auto-detect's "Change exercise" now
  returns to the library; using a preset program returns you to My Plans.
- Workout plans and Profile tabs are now swipeable, like Statistics.

## 2.0.5 (versionCode 14)

- Fixed: **completed cycles never appeared** anywhere. When a cycle finished, its
  pass was cleared and the same cycle became active again, hiding the finished
  one. Finished passes are now reconstructed properly, so they show as **Last
  cycle** on Home and in **Statistics → Cycles**.
- Exercise library: **Test** (AI) and the new **🔴 Rec** button (was "Manual
  Test") now return you to the **Exercise library** via *Change exercise* instead
  of Home. A library recording shows only *Change exercise* — no Skip/Stop —
  while a custom exercise inside a workout keeps *Skip exercise* / *Stop exercise*.
- Home **Last cycle** and **Current mission** are cleaner: coloured progress bars
  with percentages and just the workout name (and weekday) underneath — no
  exercise lists, no Statistics button. Current mission also names the upcoming
  workout. Tapping Last cycle opens Statistics → Cycles with that cycle expanded;
  tapping Current mission opens the start form with the next workout **expanded**
  showing each exercise's sets and reps.
- **Statistics → Cycles** records are collapsed by default and expand on tap.

## 2.0.4 (versionCode 13)

- Updated the creator footer to "Built by people who train and work hard. By
  projectorum.com with ❤️", with only **projectorum.com** tappable (opens the
  site in your browser); the rest is plain centered text.

- **Notifications now open the right screen.** Tapping a reminder deep-links to
  where it's about — body-measurement → Profile, workout reminder → Next
  Mission, cycle-progress → Workout Plans — whether the app was closed, in the
  background, or already open. Unknown links fall back to Home, and no duplicate
  screens are stacked.
- Added a **mic mute** button during a workout: tap it to stop listening for
  voice commands. The choice sticks between exercises and sessions.
- **Every exercise is now testable from the library** — AI-counted moves show a
  **Test** button, while non-AI moves and your own custom exercises show a
  **Manual Test** button that opens a manual session where you enter the reps
  yourself. During a manual test, a note explains that AI counting is still in
  development and invites you to contribute recordings.
- The workout screen's exit button now reads **Stop exercise** (was "Stop plan").
- The **Swap** dialog on Next Mission now lists **every** workout in the cycle,
  not just the unfinished ones — completed workouts are marked (e.g. *Completed
  100%*) and can be repeated, and the current workout is shown as *Current* but
  disabled. Repeating a workout records a new instance without touching its
  previous history.
- Finishing (or stopping) a workout now returns you to the **Home** screen — the
  same as skipping — so you always land on the refreshed dashboard (updated
  Current mission, cycle progress and Last workout) instead of the Next Mission
  screen.
- History workout cards are clearer: each exercise reads
  **`Name: 10×3 sets • 27/30 reps`** (skipped exercises show **Skipped**), custom
  exercises show their real name, and the workout **completion %** is now total
  reps done ÷ total reps planned across the workout, rounded (e.g. 75/80 → 94%).
- The home screen now shows two cycle blocks above Train smarter — **Last cycle**
  and **Current mission** — each with the cycle name + overall %, the plan name,
  every workout in order (skipped ones marked *Skipped*), and the featured
  workout's **exercise breakdown**: Current mission shows the planned setup
  (`Squat: 10×3 sets`) while Last cycle shows what you did
  (`Squat: 10×3 sets • 27/30 reps`, or *Skipped*) — the same format as History.
  Cycle % is total reps done ÷ total reps planned. Both cards have a **Statistics**
  button that opens the new **Statistics → Cycles** tab.
- Statistics now has three tabs — **Stats → Workouts → Cycles** — that you can
  swipe between. The old History tab is renamed **Workouts**. The **Cycles** tab
  lists the active cycle (on top) plus every completed cycle, each showing its
  status, dates, overall %, and every workout with its status, % and exercises.
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
- The **All Sounds** master toggle now also controls the **Set-Complete Combo
  Callout** — turning All Sounds off disables it (and suppresses the callout)
  like every other sound feature.
- Recovery timers: **Between workouts** is now set in **hours** (8-hour steps,
  8–168 h, default 48 h) with Beginner/Intermediate/Advanced presets of
  72/48/24 h — no more confusing raw-second values. Between sets and Between
  exercises stay in seconds (10-second steps).
- **Deleting** a plan, workout, exercise, custom exercise, or recording now asks
  for confirmation first.
- Simpler home screen: the **Programs** and **Recordings** buttons are gone.
  Programs now live as a tab inside **Workout Plans** (My Plans / Programs), and
  Recordings are a tab inside **Profile** (Profile / Recordings).

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
