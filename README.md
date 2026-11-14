# Pushup Club

**Track reps, follow a plan, and see progress—solo or with friends.**

Pushup Club is an offline-first Android push-up tracker built with Kotlin, Jetpack Compose, Room, CameraX, and optional Firebase Cloud Firestore clubs. It helps beginners and experienced athletes build upper-body consistency with guided adaptive programs, a 30-day challenge, weekly goal tracking, hands-free proximity or camera rep counting, smart local reminders, and optional social clubs.

## Core Features

- **Quick First-Run Setup**: Choose a starting level (Beginner first), realistic weekly goal, preferred workout days, preferred counting mode, and optional reminders—or skip setup and start a workout in one tap.
- **7-Day Weekly Progress Ring**: Measures push-ups completed over the last 7 days against your weekly target alongside lifetime totals, rest-day-aware streaks, and personal records.
- **30-Day Push-Up Challenge**: Baseline max-rep test, dynamic day-by-day workouts with built-in recovery days every 5th day, and a Day 30 final retest.
- **Adaptive Training Programs**: Built-in progressive programs (Beginner Foundations, High-Intensity Endurance, Military Prep Drills, 100 Push-up Pyramids) that automatically scale difficulty up or ease down (+10% / -10%) based on how your last session went.
- **Selectable Rep Counting Modes (Tap, Proximity, Front Camera)**:
  - **Tap Mode (Default)**: Large high-contrast tap-to-count circle, `+1 / -1` adjustments, undo set, and screen-awake protection.
  - **Proximity Sensor Mode**: Calibrated 2-step floor test with an animated placement guide and transition guard so pausing at the bottom never double-counts.
  - **Front-Camera Optical Mode (CameraX)**: On-device luminance-dip detection using a 2-second plank countdown, post-calibration `AE` exposure lock, low-light warning, and continuous calibration across rest intervals.
- **Real Clubs (Optional Google Sign-In)**: Create a club or join via a 6-character code or `pushupclub://join` invite link using a privacy-safe custom nickname (never exposing your real Google name unless you choose). Aggregate stats sync automatically after workouts and daily via `WorkManager`, and full in-app cloud account deletion is built into the Clubs tab.
- **Scheduled Local Reminders & Smart Timing Nudge**: Day-by-day `AlarmManager` notifications restored across reboots, app updates, and time zone changes (automatically skipped if you already trained today), Android 13+ permission handling, Send Test Alert, battery-optimization help, and automatic detection of your usual training hour.
- **Home-Screen Widget & Safe JSON Backup/Restore**: One-tap Quick Start widget showing today's reps and streak, plus full JSON export/import with a confirmation dialog and automatic pre-restore safety snapshot.
- **Voice Coaching, Haptics & Share Cards**: Optional Text-to-Speech cues (opt-in), haptic feedback, and optional shareable progress/challenge cards.

## Get the App & Contributor Setup

- **Google Play Package ID**: `com.aistudio.pushuptracker.fylsqb`
- **Firebase & Signing Files (Contributors)**:
  - `app/google-services.json` and `debug.keystore` are excluded from public version control for security. Contributors building locally should add their own Firebase Android `google-services.json` (and enable Play Integrity App Check in Firebase Console for production builds) and provide a standard Android `debug.keystore` at the repository root.
- **Build Locally**:
  ```bash
  gradle assembleDebug
  gradle :app:testDebugUnitTest
  ```

## License

Licensed under the [MIT License](LICENSE).

## Support & Privacy

- **Support Contact**: [elevizion876@gmail.com](mailto:elevizion876@gmail.com)
- **Privacy Policy**: See [PRIVACY_POLICY.md](PRIVACY_POLICY.md)
- **Play Store Listing Notes**: See [STORE_LISTING.md](STORE_LISTING.md)
