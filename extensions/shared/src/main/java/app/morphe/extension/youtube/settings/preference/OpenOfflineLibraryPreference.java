/*
 * Copyright (C) 2026 lavinhoque33
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Licensed under the GNU General Public License v3.0.
 * Written by lavinhoque33, 2026-10-10.
 */

package app.morphe.extension.youtube.settings.preference;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import app.morphe.extension.youtube.patches.general.offline.OfflineScreens;

/**
 * Opens the offline library, so it is reachable without replacing YouTube's Downloads page.
 */
@SuppressWarnings({"unused", "deprecation"})
public class OpenOfflineLibraryPreference extends Preference {
    {
        setOnPreferenceClickListener(pref -> {
            OfflineScreens.openLibrary(getContext());
            return true;
        });
    }

    public OpenOfflineLibraryPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public OpenOfflineLibraryPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public OpenOfflineLibraryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OpenOfflineLibraryPreference(Context context) {
        super(context);
    }
}
