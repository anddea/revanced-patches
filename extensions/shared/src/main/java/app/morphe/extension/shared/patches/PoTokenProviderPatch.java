/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2618
 *
 * Portions of this file are modified by anddea:
 * Copyright (C) 2026 anddea
 * https://github.com/anddea/revanced-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.shared.patches;

import android.net.Uri;
import android.os.Bundle;

import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.settings.SharedYouTubeSettings;
import app.morphe.extension.shared.spoof.SpoofVideoStreamsPatch;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public class PoTokenProviderPatch {
    public static final class PoTokenProviderAvailability implements Setting.Availability {
        @Override
        public boolean isAvailable() {
            return isPoTokenProviderAvailable();
        }
    }

    private static final String PO_TOKEN_SERVICE_SUFFIX = ".potokens.service.START";
    public static final String LOCAL_PO_TOKEN_SERVICE_ACTION = "app.morphe.extension.potokens.service.START";

    /** The authority is derived from the final application package in the patched manifest. */
    public static final String LOCAL_PO_TOKEN_PROVIDER_AUTHORITY_SUFFIX = ".morphe.potoken.chimera";

    private static volatile Uri cachedLocalAuthority;

    /**
     * Injection point.
     */
    public static Uri overrideAuthorities(String serviceAction, Uri uri) {
        if (useBuiltInPoTokenProvider(serviceAction)) {
            Uri localAuthority = getLocalProviderAuthority();
            Logger.printInfo(() -> "PoTokenProvider: overrideAuthorities: " + uri + " -> " + localAuthority);
            return localAuthority;
        }
        return uri;
    }

    /**
     * Injection point for legacy GMS client (YouTube <= 19.49).
     */
    public static Uri overrideAuthorities(Uri uri, Bundle bundle) {
        String serviceAction = (bundle != null) ? bundle.getString("serviceActionBundleKey") : null;
        return overrideAuthorities(serviceAction, uri);
    }

    /**
     * Injection point.
     */
    public static String overrideServiceAction(String serviceAction) {
        if (useBuiltInPoTokenProvider(serviceAction)) {
            Logger.printInfo(() -> "PoTokenProvider: overrideServiceAction: " + serviceAction + " -> " + LOCAL_PO_TOKEN_SERVICE_ACTION);
            return LOCAL_PO_TOKEN_SERVICE_ACTION;
        }
        return serviceAction;
    }

    private static Uri getLocalProviderAuthority() {
        if (cachedLocalAuthority == null) {
            synchronized (PoTokenProviderPatch.class) {
                if (cachedLocalAuthority == null) {
                    var context = Utils.getContext();
                    String pkg = (context != null) ? context.getPackageName() : "com.google.android.youtube";
                    cachedLocalAuthority = new Uri.Builder()
                            .scheme("content")
                            .authority(pkg + LOCAL_PO_TOKEN_PROVIDER_AUTHORITY_SUFFIX)
                            .build();
                }
            }
        }
        return cachedLocalAuthority;
    }

    private static boolean useBuiltInPoTokenProvider(String serviceAction) {
        return serviceAction != null
                && serviceAction.endsWith(PO_TOKEN_SERVICE_SUFFIX)
                && SharedYouTubeSettings.POTOKEN_PROVIDER.get()
                && isPoTokenProviderAvailable();
    }

    private static boolean isPoTokenProviderAvailable() {
        // To minimize confusion, it works only when 'Spoof video streams' is turned off.
        return !SpoofVideoStreamsPatch.isPatchIncluded() || !SharedYouTubeSettings.SPOOF_VIDEO_STREAMS.get();
    }
}
