# Play Console: Data safety answers (draft for v0.1)

Re-check before every release that adds a library or a network call.

## Does the app collect or share user data?
**Collects: yes. Shares: no.**

| Data type | Collected | Shared | Why | Optional? | Notes |
|---|---|---|---|---|---|
| Name, email (owner's Google account) | Yes | No | Account management (Firebase Auth) | Required to sign in | Stored by Firebase Auth |
| User IDs | Yes | No | Account management, fraud prevention | Required | Firebase uid |
| App interactions, crash logs, diagnostics | Yes | No | Analytics, app stability (Firebase Analytics, Crashlytics) | Required | No student data in logs |
| Device or other IDs | Yes | No | Fraud prevention / security (sign-in abuse protection) | Required | |

**Not collected by LibraryUp** (stays on the device, or in the owner's own Google Drive when Drive backup is on):
students' names, phone numbers, addresses, photos, ID proofs, payments, attendance.
Play's definition of "collected" is data sent off the device to the developer, so these are not declared as collected. Drive backup goes to the user's own Drive account via the `drive.file` scope, not to us.

## Security practices
- Data is encrypted in transit: **Yes** (HTTPS to Firebase/Google APIs).
- Users can request data deletion: **Yes** (Settings → Security → delete account/data; email support).
- Committed to Play Families policy: No (not a children's app).
- Independent security review: No.

## When this must change
- **AdMob / ads** (planned): declare Device or other IDs → shared with advertising partners, purpose Advertising.
- **Multi-device sync** (planned): student data would then be collected (stored on our servers) → declare
  personal info, photos, financial info (payment records) as collected, encrypted in transit and at rest.
- **Play Billing**: purchase history collected by Google Play, not by us.
