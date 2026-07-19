# SKONGA AI — Building a Real Android App (Phone-Only, No PC Needed)

This turns your existing `index.html` into an installable Android app
(`.apk`) that opens like a real app — no browser bar, no "running in
Chrome" banner, works offline for the UI (AI replies still need
internet, same as ChatGPT/Claude/Grok apps).

Since you don't have a PC, we use **Capacitor** (wraps your existing
web app into a native shell) + **GitHub Actions** (builds the APK on
GitHub's free cloud servers — Termux never has to run Android Studio
or the Android SDK itself).

---

## One-time setup

### 1. Install prerequisites in Termux
```bash
pkg update -y && pkg upgrade -y
pkg install nodejs-lts git -y
node -v
```

### 2. Create a free GitHub account (if you don't have one)
Open `https://github.com` in your phone browser → Sign up.

### 3. Create a new empty repository
On github.com (phone browser): tap **+** → **New repository** → name it
`skonga-app` → keep it **Private** or Public, your choice → **do NOT**
initialize with a README → Create repository.

### 4. Create a GitHub Personal Access Token (needed to `git push` from Termux)
- On github.com: tap your profile picture → **Settings** → scroll down to
  **Developer settings** → **Personal access tokens** → **Tokens (classic)**
  → **Generate new token (classic)**
- Give it a name like `termux`, check the **repo** checkbox, generate it
- **Copy the token immediately** (starts with `ghp_...`) — you won't see it again.
  Save it somewhere safe (e.g. a note) — you'll paste it as your password
  when pushing from Termux.

---

## Set up the project in Termux

### 5. Unzip the project I gave you
```bash
cd ~
unzip /sdcard/Download/skonga-app.zip -d skonga-app
cd skonga-app
```

### 6. Install Capacitor packages (this needs your Termux internet, not mine)
```bash
npm install
```

### 7. Add the native Android project
```bash
npx cap add android
```
This creates an `android/` folder (a full native Android project) —
you don't need to touch it directly, GitHub's servers will build it.

### 8. Connect this folder to your GitHub repo
```bash
git init
git add .
git commit -m "Initial SKONGA app"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/skonga-app.git
git push -u origin main
```
When it asks for a username: type your GitHub username.
When it asks for a password: **paste the `ghp_...` token** you saved earlier (not your real password).

---

## Let GitHub build your APK

### 9. Watch the build
On github.com, open your `skonga-app` repo → tap the **Actions** tab.
You'll see a workflow run start automatically ("Build Android APK").
It takes about 3-5 minutes.

### 10. Download the APK
Once it shows a green checkmark ✅, tap into that run → scroll down to
**Artifacts** → tap `skonga-ai-debug-apk` to download a `.zip`.

### 11. Install it on your phone
```bash
cd ~/storage/downloads   # or wherever the zip downloaded
unzip skonga-ai-debug-apk.zip
termux-open app-debug.apk
```
Android will ask to confirm installing from an unknown source the
first time — allow it (Settings → apps → this file manager/browser →
"Allow from this source"). Then it installs like any normal app, with
its own icon on your home screen.

---

## What works right away vs later

**Works immediately:**
- The app opens instantly with its own icon, no browser UI at all
- **A custom SKONGA app icon and splash screen are already included** (purple gradient with the SKONGA layers logo) — GitHub Actions generates all the required Android icon sizes automatically from `resources/icon.png`, `resources/icon-foreground.png`, `resources/icon-background.png`, and `resources/splash.png` during the build, no extra steps needed
- All your existing screens (Chat, Notes, Scan, Settings, Calculator) work exactly as before
- AI replies work whenever the phone has internet (same as any AI app)

**Want to change the icon later?**
Just replace the PNG files in `resources/` with your own artwork (keep the same filenames and roughly the same sizes: 1024×1024 for the icons, 2732×2732 for splash.png), then `git add`, `git commit`, `git push` — GitHub Actions regenerates everything automatically on the next build.

**Needs a bit more wiring later (optional, ask me when ready):**
- **Voice input** — the Web Speech API used at first doesn't actually work inside Android WebView (a platform restriction, not a bug in the code) — see `VOICE-INPUT-FIX.md` for the one-time native plugin install that makes it work for real.
- **Real file downloads** (code blocks, images, notes, AI replies) — plain Android WebView can't do this on its own; see `ANDROID-DOWNLOAD-BRIDGE.md` for a one-time, ~10-line native patch. Until you do this step, download buttons will appear to do nothing on a real device (they work fine when testing the HTML in a desktop browser).
- **Categorized notifications** (separate AI Replies / Downloads / Notes / Study / Reminders / Updates channels in Android Settings) — see `ANDROID-NOTIFICATION-CHANNELS.md`, a one-time `npm install` + `cap sync`.
- Real push notifications (currently your code has placeholders for this — Capacitor's push-notifications plugin needs a Firebase project to fully connect)
- Native camera via Capacitor's Camera plugin (right now Scan uses the web camera API, which still works fine inside Capacitor, but the native plugin is smoother)
- Publishing to the Play Store (needs a signed **release** build + a one-time $25 Google Play developer account — a separate step from what we did here, which only makes a debug/testing APK)

---

## Updating the app later

Whenever you change `www/index.html` (or anything in the SKONGA frontend):
```bash
cd ~/skonga-app
cp /path/to/updated/index.html www/index.html
git add .
git commit -m "Update SKONGA UI"
git push
```
GitHub Actions rebuilds automatically — just download the new APK from
the Actions tab again and reinstall (Android will update it in place,
your data stays since it's the same app ID).
