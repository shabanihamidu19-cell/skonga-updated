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
   *  sync with what www/index.html actually calls. */
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
