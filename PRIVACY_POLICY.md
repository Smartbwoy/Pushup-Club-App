# Privacy Policy — PushUp Pro

**Effective Date:** November 14, 2026  
**Contact:** [elevizion876@gmail.com](mailto:elevizion876@gmail.com)

## Overview

PushUp Pro is designed to work offline on your device without requiring an account or sign-in.

## Data Stored on Your Device

- **Workout & Schedule Data:** Logged push-up sessions, set breakdowns, weekly goals, training days, and achievement progress are stored locally on your device using Android Room (SQLite).
- **App Milestones:** Basic local usage milestones (such as setup completion, first workout saved, and active days visited) are stored locally in `SharedPreferences` on your device and are not transmitted to external servers.

## Permissions & Sensors

- **Notifications (`POST_NOTIFICATIONS`):** Requested only when you turn on Daily Training Reminders in the Plan tab so the app can alert you on scheduled workout days.
- **Boot Completed (`RECEIVE_BOOT_COMPLETED`):** Used only to restore your scheduled workout reminders after your device restarts.
- **Vibration (`VIBRATE`):** Used for optional haptic feedback during rep counting and rest countdowns.
- **Proximity Sensor:** Used only during an active workout when you explicitly enable Hands-Free Sensor mode to count push-up reps. Sensor readings are processed in memory and never stored or transmitted.

## Sharing

Progress cards and friend challenges are strictly optional and only shared when you tap Share and choose an app via the Android system share sheet.

## Data Deletion

You can clear all logged workouts at any time from the History tab (**Clear History**), or delete all app data by uninstalling PushUp Pro.

## Contact

If you have questions about this privacy policy or need support, email [elevizion876@gmail.com](mailto:elevizion876@gmail.com).
