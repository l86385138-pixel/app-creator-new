# Gayan Ganga Student Desktop

Windows installable student app.

## Firebase setup
1. Firebase Console → Project settings → Your apps.
2. Create/register a **Web app** for the same Firebase project.
3. Copy its Firebase web configuration into `desktop/firebase-config.js`.
4. Run `npm install` and `npm start`.
5. Build Windows installer with `npm run dist`.

The Android `google-services.json` is not the web configuration and should not be pasted into firebase-config.js.
