# My Daily Manager Premium V3 — Android Offline-first

## Added in V3
- PIN lock (4–8 digits, salted SHA-256 hash)
- Device biometric / platform authenticator unlock via WebAuthn (Android/browser decides whether fingerprint or another device verification method is shown)
- Monthly income and expense totals
- Print-ready advanced report; choose “Save as PDF” in Android print dialog
- Calendar events with notification reminders while the app is open
- File/image attachments (up to 12 MB each), stored locally and included in JSON backup
- Expanded JSON backup and restore for all modules

## Android installation
1. Host `index.html`, `manifest.webmanifest`, and `sw.js` on an HTTPS website.
2. Open the site in Android Chrome while online and choose Install app / Add to Home screen.
3. Open once online so the app shell is cached for offline use.

## Security and limitations
- PIN hash is stored locally; this is a basic app lock, not full device-grade encryption.
- Biometric login requires HTTPS and WebAuthn platform authenticator support. Android/browser may offer fingerprint, face, or device screen-lock verification; the app cannot force fingerprint specifically.
- Browser notifications require permission and reminders are checked while the app is open. Reliable background reminders after the app is fully closed require a native Android app or a push-notification server.
- PDF export uses the device print dialog; choose Save as PDF.
- Attachments are limited to 12 MB each to avoid filling browser storage.
- Exported JSON backup is not encrypted. Keep it private.
- Clearing browser/site storage can erase local data. Back up regularly.
