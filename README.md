# PushUp Pro

**Track reps, follow a plan, and see progress.**

PushUp Pro is an offline-first Android push-up tracker built with Kotlin, Jetpack Compose, and Room. It helps beginners and experienced athletes build upper-body consistency with guided programs, weekly goal tracking, hands-free or tap-based counting, scheduled training reminders, and optional progress sharing.

## Core Features

- **Quick First-Run Setup**: Choose a starting level (Beginner first), realistic weekly goal, and preferred workout days—or skip setup and start a workout in one tap.
- **7-Day Weekly Progress Ring**: Measures push-ups completed over the last 7 days against your weekly target alongside lifetime totals, streaks, and personal records.
- **Guided Programs & Manual Logging**: Built-in progressive programs (Beginner Foundations, High-Intensity Endurance, Military Prep Drills, 100 Push-up Pyramids) plus offline set logging that preserves exact rep counts across sets.
- **Hands-Free Sensor & Tap Fallback**: Proximity-sensor rep counter with a state-transition guard (prevents double-counting while pausing at the bottom), a quick 2-step calibration test, and an automatic tap-to-count fallback on devices without a proximity sensor.
- **Scheduled Local Reminders**: Day-by-day workout schedule with real `AlarmManager` notifications and Android 13+ `POST_NOTIFICATIONS` permission handling.
- **Voice Coaching & Haptics**: Optional Text-to-Speech cues and haptic feedback during sets and rest intervals.
- **Optional Progress Cards & Friend Challenges**: Share weekly totals, streaks, personal bests, or friendly challenges via Android's native share sheet.

## Get the App & Build

- **Google Play Package ID**: `com.aistudio.pushuptracker.fylsqb`
- **Build Locally**:
  ```bash
  gradle assembleDebug
  gradle :app:testDebugUnitTest
  ```

## Support & Privacy

- **Support Contact**: [elevizion876@gmail.com](mailto:elevizion876@gmail.com)
- **Privacy Policy**: See [PRIVACY_POLICY.md](PRIVACY_POLICY.md)
- **Play Store Listing Notes**: See [STORE_LISTING.md](STORE_LISTING.md)
