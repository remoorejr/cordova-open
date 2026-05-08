package com.disusered;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.net.Uri;
import android.content.Intent;
import androidx.core.content.FileProvider;
import android.webkit.MimeTypeMap;
import android.content.ActivityNotFoundException;
import android.os.Build;

import java.io.File;

/**
 * This class starts an activity for an intent to view files
 */
public class Open extends CordovaPlugin {

    public static final String OPEN_ACTION = "open";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals(OPEN_ACTION)) {
            String path = args.getString(0);
            this.chooseIntent(path, callbackContext);
            return true;
        }
        return false;
    }

    /**
     * Returns the MIME type of the file.
     *
     * @param path
     * @return
     */
    private static String getMimeType(String path) {
        String mimeType = null;

        String extension = MimeTypeMap.getFileExtensionFromUrl(path);
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            mimeType = mime.getMimeTypeFromExtension(extension.toLowerCase());
        }

        System.out.println("Mime type: " + mimeType);

        return mimeType;
    }

    /**
     * Creates an intent for the data of mime type
     *
     * @param path
     * @param callbackContext
     */
    private void chooseIntent(String path, CallbackContext callbackContext) {
    if (path != null && path.length() > 0) {
        try {
            // Clean up the path if it starts with file://
            String filePath = path;
            if (filePath.startsWith("file://")) {
                filePath = filePath.substring(7);
            }

            File file = new File(filePath);
            if (!file.exists()) {
                callbackContext.error("File not found at: " + filePath);
                return;
            }

            String mime = getMimeType(path);
            Intent fileIntent = new Intent(Intent.ACTION_VIEW);
            Context context = cordova.getActivity().getApplicationContext();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // API 24+
                // Ensure your package name matches the one in plugin.xml/AndroidManifest
                String authority = context.getPackageName() + ".provider";
                Uri contentUri = FileProvider.getUriForFile(context, authority, file);
                
                fileIntent.setDataAndTypeAndNormalize(contentUri, mime);
                fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                fileIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION); // Optional but helpful
            } else {
                fileIntent.setDataAndTypeAndNormalize(Uri.fromFile(file), mime);
            }

            // Always use NEW_TASK when starting activity from Application Context
            fileIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Temporary test to see if MIME detection is the culprit
            fileIntent.setDataAndTypeAndNormalize(contentUri, "application/pdf");

            cordova.getActivity().startActivity(fileIntent);
            callbackContext.success();

        } catch (ActivityNotFoundException e) {
            callbackContext.error(1); // No handler found
        } catch (Exception e) {
            e.printStackTrace();
            callbackContext.error(0); // Undefined error
        }
    } else {
        callbackContext.error(2);
    }

}
