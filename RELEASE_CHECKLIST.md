# Release checks

Record the release version, tester, date, device/Android versions, and results before publishing. These checks have not been completed by adding this document.

## Firebase and app identity

- Confirm who owns and pays for the production Firebase project, its project ID, and the intended Firestore database. Match `google-services.json`, `firebase.json`, and `firebase_applet_config.xml`.
- Confirm the permanent Play package ID before first publication. The current ID is `com.aistudio.pushuptracker.fylsqb`; changing an already published ID creates a different app. Visible branding and metadata currently use Pushup Club.
- Enable Google Sign-In and register debug, upload, and Play app signing SHA-1/SHA-256 certificates as appropriate. Download updated config. Test an install delivered through Play internal testing.
- Run emulator tests first. Deploy rules explicitly to the confirmed project with `firebase deploy --only firestore:rules --project YOUR_PROJECT_ID`; inspect the target database in Firebase Console afterward. Emulator success does not prove production deployment.
- Verify two real accounts can create/join/leave clubs, sync only their own stats, and delete their cloud accounts. Confirm unauthorized access is rejected.
- App Check is not currently integrated in the Android dependencies. Do not enable enforcement until a supported provider is implemented and tested with Play installs.
- Build the signed release bundle, upload to Play internal testing, and verify Google Sign-In and clubs from that build.

## First workout and counting

- On a small screen with large font settings, complete onboarding, reach Start Session, count using taps, save, and verify history and weekly progress. Repeat offline, restart the app, and verify no reps are lost.
- Test camera and proximity modes on several physical phones, including a phone without a proximity sensor. Record manually observed reps, detected reps, missed reps, and false positives for each trial.
- Repeat camera trials in bright, dim, uneven, and changing light, with different floor/camera positions and clothing. Include pauses at the bottom, rest intervals, interrupted sessions, and denied camera permission.
- Keep camera counting described as experimental until measured accuracy supports a stronger claim. Verify tap counting remains readily available when automatic counting fails.
- Test reminders, backup/restore, and widget quick start on physical devices.

## Store launch

- Capture real screenshots of onboarding, starting/saving a workout, and progress from the release build.
- Publish the privacy policy at a stable public HTTPS URL and place it in Play Console and the store listing.
- Add the actual public install URL once published. Use Play Console listing views/conversions and privacy-appropriate first-workout measurement to evaluate the funnel; avoid inferring install problems without data.
