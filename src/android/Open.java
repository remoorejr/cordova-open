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

        // Try Android's built-in utility first
        String extension = MimeTypeMap.getFileExtensionFromUrl(path);
        
        // Fallback: Manually extract extension if Android's utility fails
        if (extension == null || extension.isEmpty()) {
            int i = path.lastIndexOf('.');
            if (i > 0) {
                extension = path.substring(i + 1);
            }
        }

        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            mimeType = mime.getMimeTypeFromExtension(extension.toLowerCase());
        }

        // Check your Logcat for this output! 
        // If it prints "*/*" or "null" for a PDF, the viewer will be blank.
        System.out.println("Resolved Mime type: " + mimeType);

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
                    // Decode URL-encoded characters (like %20 for spaces)
                    filePath = java.net.URLDecoder.decode(filePath, "UTF-8"); 
                }

                File file = new File(filePath);
                if (!file.exists()) {
                    callbackContext.error("File not found at: " + filePath);
                    return;
                }

                String mime = getMimeType(path);
                if (mime == null) {
                    mime = "*/*"; // Fallback to ensure the Intent resolves even if mime is unrecognized
                }
                
                Intent fileIntent = new Intent(Intent.ACTION_VIEW);
                Context context = cordova.getActivity().getApplicationContext();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // API 24+
                    // Check for modern Cordova file provider authority first
                    String authority = context.getPackageName() + ".provider";
                    String cdvFileAuthority = context.getPackageName() + ".cdv.core.file.provider";
                    
                    // If the modern cordova-plugin-file provider exists, use it to prevent SecurityExceptions
                    if (context.getPackageManager().resolveContentProvider(cdvFileAuthority, 0) != null) {
                        authority = cdvFileAuthority;
                    }

                    Uri contentUri = FileProvider.getUriForFile(context, authority, file);
                    
                    fileIntent.setDataAndTypeAndNormalize(contentUri, mime);
                    fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    fileIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION); 

                    // Explicitly grant URI permission to all resolving packages
                    java.util.List<android.content.pm.ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(fileIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
                    for (android.content.pm.ResolveInfo resolveInfo : resInfoList) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        context.grantUriPermission(packageName, contentUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                }
                

                // FIX 2: Do NOT use FLAG_ACTIVITY_NEW_TASK when starting from an Activity context.
                // It can sever the temporary URI permissions on newer Android builds.
                // fileIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 

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

}
