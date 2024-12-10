package app.revanced.extension.youtube.patches.utils;

import app.revanced.extension.youtube.shared.BottomSheetState;

@SuppressWarnings("unused")
public class BottomSheetHookPatch {
    /**
     * Injection point.
     */
    public static void onAttachedToWindow() {
        BottomSheetState.set(BottomSheetState.OPEN);
    }

    /**
     * Injection point.
     */
    public static void onDetachedFromWindow() {
        BottomSheetState.set(BottomSheetState.CLOSED);
    }
}

