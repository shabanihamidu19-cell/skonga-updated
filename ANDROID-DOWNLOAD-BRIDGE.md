# Making downloads actually work in the APK (one-time native step)

## Why this is needed

Plain Android `WebView` (which is what Capacitor uses to show `www/`)
does **not** reliably support the standard web download trick
(`<a href="blob:..." download>` + `.click()`). Chrome/desktop browsers
handle it fine; WebView usually just... does nothing. That's why every
download button (code blocks, generated images, notes, AI replies)
looked like a "demo" that didn't save anything.

The app's JS (`saveFileForUser()` in `www/skonga_v0_8.html`) already
looks for a native bridge called `window.Android.saveBase64File(...)`
and uses it automatically when present, falling back to the normal
web method otherwise. **This file is the native half of that bridge** —
a few lines of Java added once to `MainActivity.java`.

You only need to do this once. It's a small, permanent edit to the
native Android project, so it survives every future `git push` / CI
rebuild — you don't have to repeat it per release.

---

## Steps

### 1. Generate the native Android project (if you haven't already)
```bash
cd ~/skonga-app
npx cap add android
```
This creates the `android/` folder — commit it to git, it's meant to
be a real part of the repo (the CI workflow only *syncs* it, it
doesn't create it from scratch).

### 2. Open `MainActivity.java`
It lives at:
```
android/app/src/main/java/tz/co/kclplatform/skonga/MainActivity.java
```
(the package path matches `appId` in `capacitor.config.json`).

### 3. Replace its contents with this

```java
package tz.co.kclplatform.skonga;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import com.getcapacitor.BridgeActivity;

import java.io.OutputStream;

public class MainActivity extends BridgeActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Expose window.Android.* to the web page running in the WebView
    WebView webView = this.bridge.getWebView();
    webView.addJavascriptInterface(new AndroidBridge(this), "Android");
  }

  /** JS-callable bridge. Every public method here is reachable from
   *  JS as window.Android.methodName(...). Keep method signatures in
   *  sync with what www/skonga_v0_8.html actually calls. */
  public static class AndroidBridge {
    private final Context context;

    AndroidBridge(Context context) {
      this.context = context;
    }

    /**
     * Called from JS as:
     *   window.Android.saveBase64File(base64, filename, mimeType)
     * Decodes the base64 payload and writes it straight to the
     * public Downloads folder via MediaStore (works on Android 10+
     * without needing WRITE_EXTERNAL_STORAGE at all).
     */
    @JavascriptInterface
    public void saveBase64File(String base64Data, String filename, String mimeType) {
      try {
        byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
        values.put(MediaStore.MediaColumns.MIME_TYPE,
            (mimeType == null || mimeType.isEmpty()) ? "application/octet-stream" : mimeType);

        Uri collection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
          collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        } else {
          // Pre-Android 10 fallback: legacy public Downloads path
          collection = MediaStore.Files.getContentUri("external");
        }

        Uri itemUri = context.getContentResolver().insert(collection, values);
        if (itemUri == null) throw new RuntimeException("MediaStore insert failed");

        try (OutputStream out = context.getContentResolver().openOutputStream(itemUri)) {
          if (out == null) throw new RuntimeException("Could not open output stream");
          out.write(bytes);
          out.flush();
        }

        runOnUiThreadSafe(() ->
            Toast.makeText(context, filename + " saved to Downloads", Toast.LENGTH_SHORT).show());

      } catch (Exception e) {
        runOnUiThreadSafe(() ->
            Toast.makeText(context, "Could not save " + filename, Toast.LENGTH_SHORT).show());
      }
    }

    private void runOnUiThreadSafe(Runnable r) {
      if (context instanceof android.app.Activity) {
        ((android.app.Activity) context).runOnUiThread(r);
      }
    }
  }
}
```

### 4. Rebuild
```bash
git add android/app/src/main/java/tz/co/kclplatform/skonga/MainActivity.java
git commit -m "Add native download bridge for WebView"
git push
```
GitHub Actions will pick it up on the next run. From then on, every
download button in the app (code blocks, images, notes, AI replies)
writes real files straight to the phone's **Downloads** folder,
findable in any file manager.

### Notes
- If your `appId` in `capacitor.config.json` is ever changed, the
  Java package path (`tz/co/kclplatform/skonga/...`) needs to change
  to match.
- No extra Android permission is required for this — `MediaStore` on
  Android 10+ handles scoped storage automatically.
- If `window.Android` isn't present (e.g. testing the HTML in a
  regular browser), the app automatically falls back to the standard
  blob-URL download, so nothing breaks during development.
