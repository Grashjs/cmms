package com.atlas.offline;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

/** Uses Expo's existing, non-exported FileProvider; no public file copies. */
public final class AtlasOfflineStorageModule extends ReactContextBaseJavaModule {
  private final Set<Uri> grantedUris = new HashSet<>();

  public AtlasOfflineStorageModule(ReactApplicationContext context) {
    super(context);
  }

  @Override public String getName() { return "AtlasOfflineStorage"; }

  @ReactMethod
  public void openDocument(String value, String name, String mimeType, Promise promise) {
    getReactApplicationContext().runOnUiQueueThread(() -> {
      try {
        Uri source = Uri.parse(value);
        if (!"file".equals(source.getScheme()) || source.getPath() == null)
          throw new IllegalArgumentException("A private file is required");
        File root = new File(getReactApplicationContext().getFilesDir(), "atlas-offline").getCanonicalFile();
        File file = new File(source.getPath()).getCanonicalFile();
        if (!file.getPath().startsWith(root.getPath() + File.separator) || !file.isFile())
          throw new IllegalArgumentException("File is outside private offline storage");
        Uri uri = FileProvider.getUriForFile(getReactApplicationContext(),
          getReactApplicationContext().getPackageName() + ".FileSystemFileProvider", file);
        Intent view = new Intent(Intent.ACTION_VIEW)
          .setDataAndType(uri, mimeType)
          .setClipData(ClipData.newRawUri(name, uri))
          .putExtra(Intent.EXTRA_TITLE, name)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        getReactApplicationContext().startActivity(view);
        grantedUris.add(uri);
        promise.resolve(null);
      } catch (ActivityNotFoundException error) {
        promise.reject("offline.file.viewer_unavailable", "No document viewer is available", error);
      } catch (Exception error) {
        promise.reject("offline.file.open", "Unable to open the private document", error);
      }
    });
  }

  @ReactMethod
  public void closeDocument(Promise promise) {
    getReactApplicationContext().runOnUiQueueThread(() -> {
      for (Uri uri : grantedUris)
        getReactApplicationContext().revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
      grantedUris.clear();
      promise.resolve(null);
    });
  }
}
