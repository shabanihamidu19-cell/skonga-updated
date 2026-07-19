# Fixing voice input for real (one-time native step)

## Why it never actually worked

The app's JS used the **Web Speech API**
(`SpeechRecognition`/`webkitSpeechRecognition`) — the same API that
works great in Chrome on desktop or a normal Android Chrome tab. The
problem: **it doesn't work inside an Android WebView**, which is
exactly what every Capacitor app is built on. The objects exist,
`recognition.start()` doesn't throw, mic permission can even be
granted — and it still silently fails or throws a `network` error the
moment it actually tries to listen. This is a long-standing Android
platform restriction (Chrome's speech backend is only authorized for
the standalone Chrome app, not for apps that embed WebView), not
something fixable from JS alone. That's why voice input looked fully
wired up in the code but never worked on your phone.

## The fix

The app's JS (`www/index.html`) now checks for a native plugin —
`@capacitor-community/speech-recognition` — and uses that instead
whenever it's running as the installed app. This plugin talks
directly to Android's built-in `SpeechRecognizer`, bypassing WebView's
restriction entirely, and works fully offline on most devices. The
old Web Speech API path is kept only as a fallback for when you
open this same HTML file in a regular desktop browser to test UI
changes quickly — it's genuinely fine there, just never in the APK.

You only need to install the plugin once — the JS already does the
rest automatically.

## Steps

### 1. Install the plugin
```bash
cd ~/skonga-app
npm install @capacitor-community/speech-recognition
npx cap sync android
```
(It's already listed in `package.json` — this just fetches it and
wires it into the native `android/` project.)

### 2. Nothing else to configure
`cap sync` automatically adds the `RECORD_AUDIO` permission to
`AndroidManifest.xml` — no manual native edits needed, unlike the
download bridge or notification channels.

### 3. Rebuild
```bash
git add package.json package-lock.json android/
git commit -m "Fix voice input with native speech recognition plugin"
git push
```

## How to tell it's actually using the native path

Open the app, go to Settings, and check the console/logs aren't
necessary — simplest test: tap the mic button and speak. If it
transcribes your speech into the input box, it's working. If nothing
happens or you get a "Voice input isn't available here" toast, it
means `npx cap sync android` wasn't run after installing the plugin
(the native bridge isn't wired in yet), or you're testing the raw
HTML in a browser instead of the built APK (expected — that toast is
the browser-fallback path telling you exactly that).

## Notes
- No extra permission-rationale changes needed — the existing
  "why we need your microphone" screen (Settings-adjacent, shown
  before any mic prompt) already covers this path too.
- If you ever add iOS, this same plugin also wraps `SFSpeechRecognizer`
  there, so the JS code doesn't need platform-specific branches beyond
  the native-vs-web check that's already there.
