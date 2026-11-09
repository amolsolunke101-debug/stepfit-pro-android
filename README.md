# StepFit PRO

StepFit PRO is an Android step counter with offline step tracking, optional account sync, walking route tracking, activity history, and community features.

## Build

The GitHub Actions workflow builds a debug APK on pushes to `main` and uploads it as the `StepFit-PRO-APK` artifact.

- Open the repository's **Actions** tab.
- Select the latest successful **Build StepFit PRO APK** run.
- Download the `StepFit-PRO-APK` artifact.

## Firebase setup

The app uses Firebase Authentication, Cloud Firestore, and Cloud Storage. Configure the Android app in Firebase and keep the matching `google-services.json` in `app/`. Do not commit service-account credentials or private keys.

### Deploy security rules

Install the Firebase CLI on a trusted computer and sign in to the Firebase account that owns this project. From the repository root, deploy the rules:

```sh
firebase deploy --only firestore:rules,storage
```

Confirm the selected Firebase project is the intended StepFit PRO project before deploying. The repository files `firestore.rules` and `storage.rules` are source files; a GitHub commit does **not** deploy them automatically. Review the rules in the Firebase Console after deployment.

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

**Important:** The admin screen checks the custom claim, and Firestore/Storage rules enforce access on cloud data. Assign admin access only to a trusted owner. Blocking a user prevents access to protected cloud data through the rules; it does not delete the Firebase Authentication account. The offline step counter remains a local device feature.

## Free-plan and privacy notes

- Step counting works locally and does not require an account.
- Cloud sign-in, profile sync, community, and photo upload require internet and depend on Firebase quotas and current Firebase plan limits.
- No SMS OTP flow is required by the current sign-in design.
- Do not describe cloud usage as unlimited or guaranteed free; check the Firebase Console for current usage and plan terms.
- Profile images and community data are account-linked. Review and deploy the security rules before inviting public users.

## Current limitations to verify before public release

- Firebase security rules must be deployed to the intended Firebase project and verified there. GitHub Actions security-rule tests passing do not mean the rules have been deployed.
- The owner must securely assign the admin custom claim before the Admin Panel can be used.
- GPS route history remains local by default. A separate, unchecked-by-default consent option enables private backup of a completed route to the signed-in user's own Firestore `users/{uid}/routes` collection. The Route Tracking screen includes a private Cloud History view and cloud-route deletion. Cloud routes are intended to be readable/deletable only by their owner under `firestore.rules`; they are not shared with friends or clubs.
- Before using cloud route backup/history, deploy the repository's Firestore rules to the intended Firebase project. GitHub Actions validates the rules and builds the app, but does not deploy Firebase rules. Cloud route backup requires sign-in and internet; local GPS tracking/history remain separate.
- Keep GPS cloud backup disabled unless the user explicitly opts in. Precise location history is sensitive; explain the privacy implications before enabling it.
- Challenge participation and basic personal progress are present; a verified, multi-user challenge leaderboard with server-trusted scoring is still future work.
- A successful GitHub build verifies compilation only. Device behavior, sensor accuracy, GPS behavior, account flows, and Android-version compatibility still require real-device testing.
