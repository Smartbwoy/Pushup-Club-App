# Privacy Policy — Pushup Club

**Effective Date:** November 14, 2026  
**Contact:** [elevizion876@gmail.com](mailto:elevizion876@gmail.com)

## Overview

Pushup Club works offline by default for all solo workout tracking, 30-day challenges, schedules, and reminders without requiring an account. Signing in with Google is strictly optional and only used if you choose to join or create a Pushup Club leaderboard.

## Data Stored on Your Device

- **Workout, Challenge & Schedule Data:** Logged push-up sessions, set breakdowns, weekly goals, training days, 30-day challenge progress, adaptive multipliers, and achievement progress are stored locally on your device using Android Room (SQLite) and `SharedPreferences`. Android cloud Auto Backup (`allowBackup`) is configured to exclude your local Room database and preference files so local workout history stays under your control.
- **Backups:** When you export a JSON backup, the file is saved only to the destination you select via the Android system file picker. Before restoring a backup file, the app shows a confirmation prompt and writes a local pre-restore safety snapshot inside app storage (`pre_restore_safety_backup.json`).
- **App Milestones:** Basic local usage milestones (such as setup completion, first workout saved, and active days visited) are stored locally on your device and are not sent to external servers.

## Optional Cloud Clubs & Google Sign-In

- **Google Sign-In:** If you open the **Clubs** tab and choose to sign in with Google, Firebase Authentication verifies your Google `uid` so you can create clubs, join via a 6-character invite code or `pushupclub://join` invite link, and appear on club leaderboards.
- **Privacy-Safe Club Nickname:** By default, club leaderboards display an anonymized nickname (e.g., `Athlete-1234`) rather than your Google full name or email address. You can set or change your custom club nickname at any time on the **Clubs** tab.
- **Cloud Firestore Leaderboard Data:** Only your chosen club nickname and aggregate fitness counters (`weeklyPushUps`, `totalPushUps`, `streakDays`, `personalBest`) are synced to Cloud Firestore for clubs you join or create. Individual workout timestamps or local schedules are not uploaded.
- **Automatic Leaderboard Sync:** When signed in to Clubs, your aggregate club metrics sync automatically after saved workouts/edits and once daily in the background via Android `WorkManager` so your rolling 7-day club total stays accurate even on rest days.
- **Signing Out / Leaving Clubs:** You can leave any club (automatically transferring ownership if you are the owner and other members remain, or deleting an empty club), delete clubs you created, or sign out at any time from the **Clubs** tab.

## Permissions & Sensors

- **Camera (`CAMERA`):** Requested only when you explicitly turn on **Camera Mode** during an active workout to detect downward chest movement using on-device luminance analysis. Frames are processed purely in memory in real time and are never recorded, saved, or transmitted anywhere.
- **Internet (`INTERNET`):** Used only when you voluntarily use optional **Clubs** (Google Sign-In and Cloud Firestore leaderboards).
- **Notifications (`POST_NOTIFICATIONS`):** Requested only when you turn on Daily Training Reminders in onboarding or the Plan & Settings tab so the app can alert you on scheduled workout days (skipped automatically if you already logged reps today).
- **Boot & Time Updates (`RECEIVE_BOOT_COMPLETED`):** Used only to restore your scheduled workout reminders after your device restarts, updates, or changes time zones.
- **Vibration (`VIBRATE`):** Used for optional haptic feedback during rep counting and rest countdowns.
- **Proximity Sensor:** Used only during an active workout when you explicitly enable Proximity Sensor mode to count push-up reps. Sensor readings are processed in memory and never stored or transmitted.

## Sharing

Progress cards, friend challenges, and club invite links (`pushupclub://join`) are strictly optional and only shared when you tap Share and choose an app via the Android system share sheet.

## Data & Account Deletion

- **In-App Cloud Account Deletion:** While signed in on the **Clubs** tab, tap **Delete Cloud Account & Club Data** to remove your entries from all joined clubs (transferring ownership or deleting empty clubs), delete any stored cloud user profile record, and permanently delete your Firebase Authentication account while keeping your on-device workout history intact.
- **Local Data Deletion:** Clear all logged workouts at any time from the History tab (**Clear History**), or delete all local app data by uninstalling Pushup Club.
- **Web / Email Deletion Request:** If you no longer have access to the app, you can also request deletion of cloud club records associated with your account by emailing [elevizion876@gmail.com](mailto:elevizion876@gmail.com).

## Contact

If you have questions about this privacy policy or need support, email [elevizion876@gmail.com](mailto:elevizion876@gmail.com).
