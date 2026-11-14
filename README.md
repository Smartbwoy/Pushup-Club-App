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
  - **Experimental Front-Camera Optical Mode (CameraX)**: On-device luminance-dip detection using a 2-second plank countdown, post-calibration `AE` exposure lock, low-light warning, and continuous calibration across rest intervals.
- **Real Clubs (Optional Google Sign-In)**: Create a club or join via a 6-character code or `pushupclub://join` invite link using a privacy-safe custom nickname (never exposing your real Google name unless you choose). Aggregate stats sync automatically after workouts and daily via `WorkManager`, and full in-app cloud account deletion is built into the Clubs tab.
- **Scheduled Local Reminders & Smart Timing Nudge**: Day-by-day `AlarmManager` notifications restored across reboots, app updates, and time zone changes (automatically skipped if you already trained today), Android 13+ permission handling, Send Test Alert, battery-optimization help, and automatic detection of your usual training hour.
- **Home-Screen Widget & Safe JSON Backup/Restore**: One-tap Quick Start widget showing today's reps and streak, plus full JSON export/import with a confirmation dialog and automatic pre-restore safety snapshot.
- **Voice Coaching, Haptics & Share Cards**: Optional Text-to-Speech cues (opt-in), haptic feedback, and optional shareable progress/challenge cards.

## Get the App & Contributor Setup

- **Google Play Package ID**: `com.aistudio.pushuptracker.fylsqb`
- **Requirements**: JDK 17, Android SDK platform `android-36.1`, and Build Tools `36.0.0`. Open this directory in Android Studio or set `ANDROID_HOME` to your SDK path.
- **Firebase setup**:
  1. Choose a Firebase project you control and register an Android app with package `com.aistudio.pushuptracker.fylsqb`.
  2. Enable Google as an Authentication provider. Register your debug certificate SHA-1/SHA-256 (run `./gradlew signingReport`) and, for distribution, your **Play app signing** certificate fingerprints. The upload certificate alone is insufficient for Play installs.
  3. Download the updated `google-services.json` into `app/`. It must include the web OAuth client used by Google Sign-In. This file identifies a project; it does not replace server security rules. Keep production configuration out of public build artifacts and use a separate development project locally.
  4. Create the named Firestore database in `firebase.json`, or update both that file and `app/src/main/res/values/firebase_applet_config.xml` to the database you intend to use. That database name is not proof of project ownership.
  5. Follow [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) before enabling public access.
- **Build locally** (from this directory):
  ```bash
  ./gradlew :app:assembleDebug
  ```
  Android generates the standard local debug keystore automatically; no repository-root keystore is required. Install `app/build/outputs/apk/debug/app-debug.apk` on a device. Use `gradlew.bat` on Windows.
- **Tests**: The repository integration tests require the Auth and Firestore emulators. With Firebase CLI installed, run:
  ```bash
  firebase emulators:exec --project demo-no-project --only auth,firestore './gradlew :app:testDebugUnitTest'
  ```
  CI uses a dummy Firebase configuration and emulators. Its APK verifies compilation and tests; it cannot authenticate against production Firebase.
- **Release signing**: Supply `KEYSTORE_PATH` (absolute path), `STORE_PASSWORD`, and `KEY_PASSWORD`; the keystore must contain alias `upload`. Then run `./gradlew :app:bundleRelease`. Never commit keystores or passwords. A production release also needs the production Firebase configuration and the checks below.
- **CI**: The GitHub Actions Android workflow builds a debug APK and runs unit/integration tests with Firebase emulators on every push and pull request.

## License

Licensed under the [MIT License](LICENSE).

## Support & Privacy

- **Support Contact**: [elevizion876@gmail.com](mailto:elevizion876@gmail.com)
- **Privacy Policy**: See [PRIVACY_POLICY.md](PRIVACY_POLICY.md)
- **Play Store Listing Notes**: See [STORE_LISTING.md](STORE_LISTING.md)
