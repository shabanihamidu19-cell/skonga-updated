# Categorized Android notifications (one-time native step)

## Why

Android groups notifications into **channels** that show up separately
under *Settings ▸ Apps ▸ SKONGA AI ▸ Notifications*, each with its own
on/off switch and sound. Without this, every notification lands in one
generic "Default" bucket. The app's JS already defines the channels it
wants (`NOTIFY_CHANNELS` in `www/skonga_v0_8.html`) — 🤖 AI Replies,
⬇ Downloads, 📝 Notes, 📚 Study, 🎯 Daily Reminder, 🔥 Updates — and
will create them automatically at runtime **once the native Local
Notifications plugin is installed**. This doc is that install step.

## Steps

### 1. Install the plugin
```bash
cd ~/skonga-app
npm install @capacitor/local-notifications
npx cap sync android
```
(`@capacitor/local-notifications` is already listed in
`package.json` — this just needs `npm install` to actually fetch it,
and `cap sync` to wire it into the native `android/` project.)

### 2. Android 13+ runtime permission
Android 13 (API 33) requires the user to explicitly grant a
notification permission at runtime — Capacitor's Local Notifications
plugin handles the request for you; no manual manifest edits needed
beyond what `cap sync` already adds automatically
(`POST_NOTIFICATIONS` permission in `AndroidManifest.xml`).

### 3. That's it
The app calls `LocalNotifications.createChannel(...)` for each of the
six channels the first time notifications are turned on in Settings
(or automatically at startup if they were already enabled in a
previous session). No further native code is required — this plugin,
unlike file downloads, ships a full JS API that covers channel
creation out of the box.

### 4. Rebuild
```bash
git add package.json package-lock.json android/
git commit -m "Add categorized Android notification channels"
git push
```

## Result

Once a user gets their first "AI Replies" notification, Android will
have registered a **🤖 AI Replies** channel they can individually mute,
change the sound for, or turn off — without affecting Downloads,
Notes, or Daily Reminder notifications.

## Notes
- Push notifications via Firebase (for while the app is fully closed)
  are a separate, optional step — see `CAPACITOR_SETUP.md`. Local
  notifications (this doc) work whenever the app is backgrounded, not
  force-closed.
- If you don't run `npx cap sync android` after installing the
  plugin, `window.Capacitor.Plugins.LocalNotifications` won't exist
  and the app silently falls back to the plain web `Notification` API
  (single channel) — nothing crashes either way.
