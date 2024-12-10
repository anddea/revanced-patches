package app.revanced.extension.shared.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class IntentUtils extends Utils {

    public static void launchExternalDownloader(@NonNull String content, @NonNull String downloaderPackageName) {
        Intent intent = new Intent("android.intent.action.SEND");
        intent.setType("text/plain");
        intent.setPackage(downloaderPackageName);
        intent.putExtra("android.intent.extra.TEXT", content);
        launchIntent(intent);
    }

    private static void launchIntent(@NonNull Intent intent) {
        // If possible, use the main activity as the context.
        // Otherwise fall back on using the application context.
        Context mContext = getActivity();
        if (mContext == null) {
            // Utils context is the application context, and not an activity context.
            mContext = context;
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        mContext.startActivity(intent);
    }

    public static void launchView(@NonNull String content) {
        launchView(content, null);
    }

    public static void launchView(@NonNull String content, @Nullable String packageName) {
        Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(content));
        if (packageName != null) {
            intent.setPackage(packageName);
        }
        launchIntent(intent);
    }

}
