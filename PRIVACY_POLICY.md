# Privacy Policy — Pushup Club

**Effective Date:** November 14, 2026  
**Contact:** [elevizion876@gmail.com](mailto:elevizion876@gmail.com)

## Overview

Pushup Club works offline by default for all solo workout tracking, 30-day challenges, schedules, and reminders without requiring an account. Signing in with Google is strictly optional and only used if you choose to join or create a Pushup Club leaderboard.

## Data Stored on Your Device

- **Workout, Challenge & Schedule Data:** Logged push-up sessions, set breakdowns, weekly goals, training days, 30-day challenge progress, adaptive multipliers, and achievement progress are stored locally on your device using Android Room (SQLite) and `SharedPreferences`.
- **Backups:** When you export a JSON backup, the file is saved only to the destination you select via the Android system file picker.
- **App Milestones:** Basic local usage milestones (such as setup completion, first workout saved, and active days visited) are stored locally on your device and are not sent to external servers.

## Optional Cloud Clubs & Google Sign-In

- **Google Sign-In:** If you open the **Clubs** tab and choose to sign in with Google, Firebase Authentication verifies your Google identity (`uid`, display name) so you can create clubs, join via a 6-character invite code, and appear on club leaderboards.
- **Cloud Firestore Leaderboard Data:** Only your club display name and aggregate fitness counters (`weeklyPushUps`, `totalPushUps`, `streakDays`, `personalBest`) are synced to Cloud Firestore for clubs you join or create. Individual workout timestamps or local schedules are not uploaded.
- **Signing Out / Leaving Clubs:** You can leave any club, delete clubs you created, or sign out at any time from the **Clubs** tab.

## Permissions & Sensors

- **Camera (`CAMERA`):** Requested only when you explicitly turn on **Camera Mode** during an active workout to detect downward chest movement using on-device luminance analysis. Frames are processed purely in memory in real time and are never recorded, saved, or transmitted anywhere.
- **Internet (`INTERNET`):** Used only when you voluntarily use optional **Clubs** (Google Sign-In and Cloud Firestore leaderboards).
- **Notifications (`POST_NOTIFICATIONS`):** Requested only when you turn on Daily Training Reminders in the Plan tab so the app can alert you on scheduled workout days.
- **Boot Completed (`RECEIVE_BOOT_COMPLETED`):** Used only to restore your scheduled workout reminders after your device restarts.
- **Vibration (`VIBRATE`):** Used for optional haptic feedback during rep counting and rest countdowns.
- **Proximity Sensor:** Used only during an active workout when you explicitly enable Hands-Free Sensor mode to count push-up reps. Sensor readings are processed in memory and never stored or transmitted.

## Sharing

Progress cards, friend challenges, and club invite codes are strictly optional and only shared when you tap Share and choose an app via the Android system share sheet.

## Data Deletion

You can clear all logged workouts at any time from the History tab (**Clear History**), delete clubs you own or leave clubs in the **Clubs** tab, or delete all local app data by uninstalling Pushup Club. To request deletion of cloud club records associated with your account, email [elevizion876@gmail.com](mailto:elevizion876@gmail.com).

## Contact

If you have questions about this privacy policy or need support, email [elevizion876@gmail.com](mailto:elevizion876@gmail.com).
