# Admin authorization setup

The Android client does not contain an administrator PIN or grant admin privileges from a username, email, Room row, or BuildConfig value. Privileged screens and Firestore writes require a verified email, the Firebase ID-token custom claim `admin: true`, and an SMS MFA sign-in.

1. Deploy the project rules with Firebase CLI: `firebase deploy --only firestore:rules --project <firebase-project-id>`.
2. From a trusted server or one-time Admin SDK environment, assign the `admin` custom claim to the verified administrator account's Firebase UID. Preserve any existing custom claims when updating them.
3. Configure Firebase Authentication SMS multi-factor authentication for the verified administrator account. The Android sign-in flow currently supports SMS phone factors; Admin UI and Firestore rules require an MFA-authenticated sign-in as well as the custom claim.
4. Do not put service-account credentials or Admin SDK scripts containing credentials in the Android project or `.env` file.

The trusted provisioning environment must also create the administrator's `users/{uid}` profile with `uid`, `email`, `username`, `fullName`, `authProvider`, `createdAt`, `lastLoginAt`, `activitySummary`, and `isRemoved: false`. Assign the claim with the Admin SDK using the real Firebase UID and preserving existing claims, for example:

```js
const account = await getAuth().getUser(adminUid);
await getAuth().setCustomUserClaims(adminUid, {
  ...account.customClaims,
  admin: true,
});
```

Never run this provisioning code in the Android app or expose an Admin SDK service-account key.

User removal is recorded as a Firestore status and blocks subsequent sign-ins and access to protected Firestore collections. Disabling/deleting a Firebase Authentication account and revoking already-issued tokens requires a trusted Cloud Function or Admin SDK service; it cannot safely be performed by this Android client.
