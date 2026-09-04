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

package app.morphe.extension.shared.settings.preference;

import static app.morphe.extension.shared.utils.StringRef.str;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import app.morphe.extension.shared.settings.SharedYouTubeSettings;
import app.morphe.extension.shared.spoof.SpoofVideoStreamsPatch;
import app.morphe.extension.shared.utils.Logger;

@SuppressWarnings({"deprecation", "unused"})
public class PoTokenProviderPreference extends SwitchPreference {

    /** Callback when the app is resumed, so the switch reflects the current stream setting. */
    private final Application.ActivityLifecycleCallbacks ACTIVITY_LIFECYCLE_CALLBACKS
            = new Application.ActivityLifecycleCallbacks() {

        public void onActivityResumed(@NonNull Activity activity) {
            Logger.printDebug(() -> "onActivityResumed");
            updateUI();
        }

        public void onActivityCreated(@NonNull Activity a, @Nullable Bundle b) {}
        public void onActivityStarted(@NonNull Activity a) {}
        public void onActivityPaused(@NonNull Activity a) {}
        public void onActivityStopped(@NonNull Activity a) {}
        public void onActivitySaveInstanceState(@NonNull Activity a, @NonNull Bundle b) {}
        public void onActivityDestroyed(@NonNull Activity a) {}
    };

    private void registerApplicationOnResumeCallback() {
        SpoofVideoStreamsPatch.getApplication().registerActivityLifecycleCallbacks(
                ACTIVITY_LIFECYCLE_CALLBACKS
        );
    }

    private void unregisterApplicationOnResumeCallback() {
        SpoofVideoStreamsPatch.getApplication().unregisterActivityLifecycleCallbacks(
                ACTIVITY_LIFECYCLE_CALLBACKS
        );
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        registerApplicationOnResumeCallback();
    }

    @Override
    protected void onPrepareForRemoval() {
        super.onPrepareForRemoval();
        unregisterApplicationOnResumeCallback();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        updateUI(); // Needed to update summary after Spoof stream is disabled but not restarted.
    }

    private void updateUI() {
        final boolean available = SharedYouTubeSettings.POTOKEN_PROVIDER.isAvailable();
        super.setEnabled(available);

        String summaryKey = available
                    ? "morphe_potoken_provider_summary"
                    : "morphe_potoken_provider_unavailable_summary";
        setSummary(str(summaryKey));
    }

    public PoTokenProviderPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public PoTokenProviderPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PoTokenProviderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PoTokenProviderPreference(Context context) {
        super(context);
    }
}
