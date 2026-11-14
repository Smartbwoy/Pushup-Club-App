# Pushup Club

**Track reps, follow a plan, and see progress—solo or with friends.**

Pushup Club is an offline-first Android push-up tracker built with Kotlin, Jetpack Compose, Room, CameraX, and optional Firebase Cloud Firestore clubs. It helps beginners and experienced athletes build upper-body consistency with guided adaptive programs, a 30-day challenge, weekly goal tracking, hands-free proximity or camera rep counting, smart local reminders, and optional social clubs.

## Core Features

- **Quick First-Run Setup**: Choose a starting level (Beginner first), realistic weekly goal, and preferred workout days—or skip setup and start a workout in one tap.
- **7-Day Weekly Progress Ring**: Measures push-ups completed over the last 7 days against your weekly target alongside lifetime totals, rest-day-aware streaks, and personal records.
- **30-Day Push-Up Challenge**: Baseline max-rep test, dynamic day-by-day workouts with built-in recovery days every 5th day, and a Day 30 final retest.
- **Adaptive Training Programs**: Built-in progressive programs (Beginner Foundations, High-Intensity Endurance, Military Prep Drills, 100 Push-up Pyramids) that automatically scale difficulty up or ease down (+10% / -10%) based on how your last session went.
- **Hands-Free Proximity, Front-Camera Optical Mode & Tap Fallback**:
  - **Proximity Sensor Mode**: Calibrated 2-step floor test with an animated placement guide and transition guard so pausing at the bottom never double-counts.
  - **Front-Camera Optical Mode (CameraX)**: On-device luminance-dip detection for phones without a reliable proximity sensor, with a live compact viewfinder and depth gauge.
  - **Tap Mode**: Reliable tap-to-count circle, `+1 / -1` adjustments, undo set, and screen-awake protection.
- **Real Clubs (Optional Google Sign-In)**: Create a club or join a friend's club with a 6-character invite code, sync your stats, and compete on a real-time weekly leaderboard powered by Cloud Firestore. Solo tracking stays 100% offline with zero sign-in required.
- **Scheduled Local Reminders & Smart Timing Nudge**: Day-by-day `AlarmManager` notifications, Android 13+ permission handling, Send Test Alert, battery-optimization help, and automatic detection of your usual training hour.
- **Home-Screen Widget & JSON Backup/Restore**: One-tap Quick Start widget showing today's reps and streak, plus full JSON export/import via Android Storage Access Framework.
- **Voice Coaching, Haptics & Share Cards**: Optional Text-to-Speech cues, haptic feedback, and optional shareable progress/challenge cards.

## Get the App & Build

- **Google Play Package ID**: `com.aistudio.pushuptracker.fylsqb`
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
