# SIMSIM CALL

Private Android messaging and one-to-one encrypted audio calling.

## Current architecture

- Android application ID: `com.simsim.call`
- Firebase project: `simsim-call-52f31`
- Firebase Authentication + Cloud Firestore for accounts, private user codes, and encrypted message envelopes.
- Messages use a fresh AES-256-GCM key per message; that key is RSA-OAEP wrapped separately for the sender and recipient. Private RSA keys stay in Android Keystore.
- Audio uses LiveKit Android with LiveKit media E2EE. The call secret is encrypted for both participants in Firestore and is not sent to the token service.
- The Node server verifies Firebase ID tokens and accepted-call membership before issuing LiveKit room tokens.

## First-time Firebase setup

1. In Firebase Authentication, enable Email/Password.
2. Create your own administrator account in Authentication and verify its UID.
3. In Firestore, create document `admins/<YOUR_AUTH_UID>` with exactly `{ "role": "admin" }`.
4. From the repository root, deploy the rules:
   `firebase deploy --only firestore:rules`
5. The admin signs in through the app's Admin entry and creates one-time invitation codes.

New member records can only be created after the corresponding invitation has been atomically marked as used by that same Firebase UID.

## Render setup

See [server/README.md](server/README.md). The Render service must use root directory `server` and set its private environment variables. Never commit service-account JSON, LiveKit API secrets, or real environment files.

## Android build

Use JDK 17, Android SDK 35, and Gradle 8.7. Set the deployed Render HTTPS base URL as a Gradle project property:

`gradle --no-daemon :app:assembleDebug -PSIMSIM_SERVER_URL=https://YOUR-RENDER-SERVICE.onrender.com`

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

The CI workflow builds the debug APK on pushes and pull requests.

## Important security and test status

This branch is a development candidate, not a production security certification. Before distributing it, run the Android build and test registration, one-time invitation rejection/reuse, code lookup, encrypted message send/receive, and two-device audio calls. Verify key fingerprints out of band before trusting a contact. If app data is cleared or the app is reinstalled, the Android Keystore private key may be lost and old messages/call keys may no longer be decryptable. A reviewed key-recovery and forward-secrecy protocol is required before claiming Signal-level security.
