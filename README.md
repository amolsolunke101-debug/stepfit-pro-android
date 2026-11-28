# StepFit PRO

StepFit PRO is an Android step counter with offline step tracking, optional account sync, walking route tracking, activity history, and community features.

## Build

The GitHub Actions workflow runs Android lint, builds a debug APK, and compiles an unsigned release APK on pushes to `main` or `fix/**` branches and on pull requests targeting `main`. It uploads both files as the `StepFit-PRO-APKs` artifact.

- Open the repository's **Actions** tab.
- Select the latest successful **Build StepFit PRO APK** run.
- Download the `StepFit-PRO-APKs` artifact.
- Use the debug APK for device testing. The release APK is unsigned and is not ready for public distribution or app-store installation until a protected release signing key is configured.

## Firebase setup

The app uses Firebase Authentication and Cloud Firestore. It does not use Firebase Cloud Storage, so profile-photo selection stays local to the device. Configure the Android app in Firebase and keep the matching `google-services.json` in `app/`. Do not commit service-account credentials or private keys.

### Deploy security rules

Install the Firebase CLI on a trusted computer and sign in to the Firebase account that owns this project. This repository includes a `firebase.json` that maps the Firestore rules file. From the repository root, replace the placeholder with the exact Firebase project ID and deploy only the Firestore rules:

```sh
firebase deploy --only firestore:rules --project YOUR_FIREBASE_PROJECT_ID
```

Do not run this command against a production project until its owner has explicitly approved the deployment. The current Firestore rules are still under security review; do not use them for a public rollout until the full ruleset has been reviewed and its emulator tests have passed. The `firestore.rules` file is used by this app; `storage.rules` is retained as an unused template and is not deployed by this command. A GitHub commit does **not** deploy security rules automatically. Review the Firestore rules in the Firebase Console after an authorized deployment.

### Grant or revoke the protected admin claim

Admin access is controlled by a Firebase Authentication **custom claim** named `admin`. It is not controlled by an email address, a screen setting, or a value users can set in their own profile.

1. In Firebase Console, open Authentication and identify the intended administrator account.
2. Copy that account's Firebase Authentication UID.
3. In Google Cloud Console, create a service-account key with only the permissions needed to administer Firebase Authentication. Store the JSON file outside this repository in a protected location.
4. On a trusted computer with Node.js installed, open a terminal in the repository root and install the Admin SDK locally without saving dependency files:

```sh
npm install --no-save --no-package-lock firebase-admin
```

5. From the same repository root, run:

```sh
GOOGLE_APPLICATION_CREDENTIALS="/secure/path/service-account.json" node scripts/set-admin-claim.js FIREBASE_AUTH_UID
```

To revoke the admin claim:

```sh
GOOGLE_APPLICATION_CREDENTIALS="/secure/path/service-account.json" node scripts/set-admin-claim.js FIREBASE_AUTH_UID --revoke
```

Replace the example path and UID with the actual values. Never send the service-account JSON file to anyone or upload it to GitHub. The administrator must refresh their Firebase ID token (sign out and sign in again is the simplest option) before the app sees the new claim.

**Important:** The admin screen checks the custom claim, and Firestore rules enforce access to cloud data. Assign admin access only to a trusted owner. Blocking a user prevents access to protected cloud data through the rules; it does not delete the Firebase Authentication account. The offline step counter remains a local device feature.

## Free-plan and privacy notes

- Step counting works locally and does not require an account.
- Email/Google sign-in, profile sync, and community features require internet and use Firebase Authentication/Firestore quotas; they are not unlimited.
- Profile-photo selection is saved on the device only. This build intentionally does not upload photos to Firebase Cloud Storage.
- No SMS OTP flow is required by the current sign-in design.
- Do not describe cloud usage as unlimited or guaranteed free; check the Firebase Console for current usage and plan terms.
- Review and deploy Firestore security rules before inviting public users.

## Current limitations to verify before public release

- Firestore security rules must be deployed and tested in the Firebase project; Cloud Storage is not used by this build.
- The owner must securely assign the admin custom claim before the Admin Panel can be used.
- Route history is currently stored locally on the device; it is not a cloud-synced route archive.
- Challenge participation and basic personal progress are present; a verified, multi-user challenge leaderboard with server-trusted scoring is still future work.
- A successful GitHub build verifies compilation only. Device behavior, sensor accuracy, GPS behavior, account flows, and Android-version compatibility still require real-device testing.
