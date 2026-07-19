# Signed release APK (one-time setup)

The CI workflow now has a `assembleRelease` step, but it's gated on 4
GitHub secrets — it's automatically **skipped** until you add them, so
your existing debug-APK builds keep working exactly as before.

Why you'd want this: a **debug** APK (what you've been building so
far) is fine for installing on your own phone via `termux-open`, but
it's not accepted for Play Store, and Android treats every debug
build as "not trusted" the same way. A **release** APK, signed with
your own keystore, is what you'd actually distribute or publish.

## 1. Generate a keystore (do this once, keep it forever)

You need a JDK installed somewhere to run `keytool` — easiest is to
do this step on the GitHub Actions runner itself via a throwaway
workflow, or if you have any machine with Java. From Termux, `keytool`
ships with the JDK, which Termux's `openjdk-17` package includes:

```bash
pkg install openjdk-17 -y
keytool -genkeypair -v \
  -keystore skonga-release.keystore \
  -alias skonga \
  -keyalg RSA -keysize 2048 -validity 10000
```

It'll ask for a keystore password, your name/org details, and a key
password (can be the same as the keystore password). **Write both
passwords down somewhere safe — if you lose them, you cannot update
this app under the same signature ever again (Play Store enforces
this permanently).**

This creates `skonga-release.keystore` — **do not commit this file to
git**, it's already covered by `.gitignore`'s `*.keystore` pattern (add
it if it isn't there).

## 2. Base64-encode it for GitHub Secrets

```bash
base64 -w 0 skonga-release.keystore > keystore-base64.txt
cat keystore-base64.txt
```
Copy the entire output (one long line, no spaces).

## 3. Add 4 repository secrets

On github.com: your repo → **Settings** → **Secrets and variables** →
**Actions** → **New repository secret**. Add all four:

| Secret name                  | Value                                  |
|-------------------------------|-----------------------------------------|
| `ANDROID_KEYSTORE_BASE64`     | the long base64 string from step 2     |
| `ANDROID_KEYSTORE_PASSWORD`   | the keystore password you set          |
| `ANDROID_KEY_ALIAS`           | `skonga` (or whatever alias you used)  |
| `ANDROID_KEY_PASSWORD`        | the key password you set               |

## 4. Push — CI does the rest

Next push to `main` (or manual run from the Actions tab), the
workflow will:
- still build the debug APK as before (`skonga-ai-debug-apk`)
- **also** build a signed release APK (`skonga-ai-release-apk`), if
  and only if the secrets above are present

Download it the same way as the debug one: Actions tab → the run →
Artifacts.

## If the release build fails with a "signing config" error

The Android Gradle Plugin's injected signing properties
(`-Pandroid.injected.signing.*`, used in the workflow) work
automatically for a stock `cap add android` project. If your
`android/app/build.gradle` has been hand-edited to define its own
`release` `signingConfig` block, that will take priority instead and
the injected properties will be ignored — in that case, either remove
the custom `signingConfig` from `build.gradle` and let CI handle it
as above, or wire the same 4 values into your existing
`signingConfig` block using `System.getenv(...)`.

## Play Store publishing (separate, optional step)

A signed release APK is a prerequisite for the Play Store, but not
the whole thing — you'd also need:
- a one-time $25 Google Play Console developer account
- an **AAB** (Android App Bundle) rather than APK for new app
  submissions — swap `assembleRelease` for `bundleRelease` in the
  workflow, output path becomes
  `android/app/build/outputs/bundle/release/app-release.aab`
- store listing assets (screenshots, descriptions, privacy policy
  URL — see the permission-rationale note in the app itself, Play
  Store review looks for that when camera/mic permissions are used)

None of that is required just to have a properly signed APK for
direct distribution/testing, which is what this step alone gives you.
