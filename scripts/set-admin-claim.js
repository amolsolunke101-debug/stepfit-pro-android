/**
 * Assign the StepFit PRO admin custom claim to one Firebase Auth user.
 *
 * SECURITY:
 * - Run only on a trusted computer.
 * - Keep the service-account JSON outside this repository.
 * - Never put service-account credentials in the APK, GitHub, or chat.
 *
 * Usage:
 *   GOOGLE_APPLICATION_CREDENTIALS="/secure/path/service-account.json" node scripts/set-admin-claim.js FIREBASE_AUTH_UID
 */
const admin = require("firebase-admin");

const uid = process.argv[2];
const revoke = process.argv[3] === "--revoke";
if (!uid) {
  console.error("Usage: node scripts/set-admin-claim.js FIREBASE_AUTH_UID [--revoke]");
  process.exit(1);
}
if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
  console.error("Set GOOGLE_APPLICATION_CREDENTIALS to a secure service-account JSON file path.");
  process.exit(1);
}

admin.initializeApp({ credential: admin.credential.applicationDefault() });

(async () => {
  try {
    const user = await admin.auth().getUser(uid);
    const existing = user.customClaims || {};
    const claims = { ...existing };
    if (revoke) delete claims.admin;
    else claims.admin = true;
    await admin.auth().setCustomUserClaims(uid, claims);
    console.log(revoke ? "Admin claim revoked from:" : "Admin claim assigned to:", uid);
    console.log("The user must sign out and sign in again (or refresh the ID token) for the claim to appear.");
  } catch (error) {
    console.error("Could not assign admin claim:", error.message);
    process.exitCode = 1;
  }
})();
