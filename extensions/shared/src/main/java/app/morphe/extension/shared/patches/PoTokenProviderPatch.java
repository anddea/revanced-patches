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

import static app.morphe.extension.shared.utils.StringRef.str;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import app.morphe.extension.shared.settings.LongSetting;
import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.settings.SharedYouTubeSettings;
import app.morphe.extension.shared.spoof.SabrResponseReader;
import app.morphe.extension.shared.spoof.SpoofVideoStreamsPatch;
import app.morphe.extension.shared.ui.ReminderDialog;
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
     * The status YouTube sends once it no longer accepts the token of the app.
     */
    private static final int ATTESTATION_REQUIRED = 3;
    /**
     * YouTube Music sends no status, its server only stops sending media and asks to retry later.
     * Normal playback requests media only when it needs it, so every response has some.
     */
    private static final int RESPONSES_WITHOUT_MEDIA_LIMIT = 5;

    /**
     * Readers of the SABR responses, by the native callbacks of their request.
     */
    private static final Map<Object, SabrResponseReader> sabrResponseReaders =
            Collections.synchronizedMap(new WeakHashMap<>());
    @Nullable
    private static SabrResponseReader lastSabrResponseReader;
    private static int responsesWithoutMedia;
    @Nullable
    private static volatile Boolean providerInUse;

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

    /**
     * Settings that change this restart the app, so it is checked only once.
     */
    private static boolean isProviderInUse() {
        Boolean inUse = providerInUse;
        if (inUse == null) {
            inUse = SharedYouTubeSettings.POTOKEN_PROVIDER.get()
                    && SpoofVideoStreamsPatch.isPatchIncluded()
                    && isPoTokenProviderAvailable();
            providerInUse = inUse;
        }
        return inUse;
    }

    /**
     * Injection point.
     * Called off the main thread when a media request starts.
     */
    public static void onMediaRequest(MediaHttpRequest request, Object callbacks) {
        try {
            if (!isProviderInUse()) {
                return;
            }
            String url = request.patch_getUrl();
            // Live streams can have responses without media while the stream waits for new segments.
            if (url != null && url.contains("/videoplayback") && url.contains("sabr=1")
                    && !url.contains("yt_live_broadcast") && !url.contains("yt_premiere_broadcast")) {
                onSabrRequest(callbacks);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "onMediaRequest failure", ex);
        }
    }

    /**
     * A new request means the previous response is complete.
     */
    private static synchronized void onSabrRequest(Object callbacks) {
        SabrResponseReader previous = lastSabrResponseReader;
        if (previous != null) {
            if (previous.sawMedia()) {
                responsesWithoutMedia = 0;
            } else if (++responsesWithoutMedia >= RESPONSES_WITHOUT_MEDIA_LIMIT) {
                responsesWithoutMedia = 0;
                onTokenRejected("media responses no longer contain media");
            }
        }
        SabrResponseReader reader = new SabrResponseReader();
        sabrResponseReaders.put(callbacks, reader);
        lastSabrResponseReader = reader;
    }

    /**
     * Injection point.
     * Called with each chunk of a media response, before native code reads it.
     */
    public static void onMediaResponseChunk(Object callbacks, @Nullable ByteBuffer chunk) {
        if (chunk == null) {
            return;
        }
        try {
            if (sabrResponseReaders.isEmpty()) {
                return;
            }
            SabrResponseReader reader = sabrResponseReaders.get(callbacks);
            if (reader == null) {
                return;
            }
            if (reader.read(chunk) == ATTESTATION_REQUIRED) {
                sabrResponseReaders.remove(callbacks);
                onTokenRejected("attestation is required");
            }
        } catch (Exception ex) {
            sabrResponseReaders.remove(callbacks);
            Logger.printInfo(() -> "Could not read media response", ex);
        }
    }

    /**
     * Playback stops once the buffered media runs out, so offer to turn on 'Spoof video streams'.
     */
    private static synchronized void onTokenRejected(String reason) {
        Logger.printDebug(() -> "YouTube no longer accepts the token made by the helper: " + reason);
        // Log only until the check stops firing while the token still works.
        if (!SharedYouTubeSettings.POTOKEN_PROVIDER_EXPIRED_DIALOG.get()) {
            return;
        }
        LongSetting lastShown = SharedYouTubeSettings.POTOKEN_PROVIDER_EXPIRED_DIALOG_LAST_SHOWN;
        Activity activity = Utils.getActivity();
        if (activity == null || !ReminderDialog.isDue(lastShown)) {
            return;
        }
        ReminderDialog.show(
                activity,
                lastShown,
                str("morphe_potoken_provider_expired_dialog_title"),
                str("morphe_potoken_provider_expired_dialog_message"),
                str("morphe_potoken_provider_expired_dialog_turn_on"),
                () -> {
                    SharedYouTubeSettings.SPOOF_VIDEO_STREAMS.save(true);
                    Utils.restartApp(activity);
                }
        );
    }
}
