package app.morphe.extension.music.sponsorblock.objects;

import static app.morphe.extension.shared.utils.StringRef.sf;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

import app.morphe.extension.shared.utils.StringRef;

public enum CategoryBehaviour {
    SKIP_AUTOMATICALLY("skip", 2, true, sf("revanced_sb_skip_automatically")),
    // ignored categories are not exported to json, and ignore is the default behavior when importing
    IGNORE("ignore", -1, false, sf("revanced_sb_skip_ignore"));

    /**
     * ReVanced specific value.
     */
    @NonNull
    public final String reVancedKeyValue;
    /**
     * Desktop specific value.
     */
    public final int desktopKeyValue;
    /**
     * If the segment should skip automatically
     */
    public final boolean skipAutomatically;
    @NonNull
    public final StringRef description;

    CategoryBehaviour(String reVancedKeyValue, int desktopKeyValue, boolean skipAutomatically, StringRef description) {
        this.reVancedKeyValue = Objects.requireNonNull(reVancedKeyValue);
        this.desktopKeyValue = desktopKeyValue;
        this.skipAutomatically = skipAutomatically;
        this.description = Objects.requireNonNull(description);
    }

    @Nullable
    public static CategoryBehaviour byReVancedKeyValue(@NonNull String keyValue) {
        for (CategoryBehaviour behaviour : values()) {
            if (behaviour.reVancedKeyValue.equals(keyValue)) {
                return behaviour;
            }
        }
        return null;
    }
}
