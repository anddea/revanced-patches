package app.revanced.extension.youtube.patches.utils;

import app.revanced.extension.shared.utils.ResourceUtils;

@SuppressWarnings("unused")
public class DrawableColorPatch {
    private static final int[] WHITE_VALUES = {
            -1,         // comments chip background
            -394759,    // music related results panel background
            -83886081   // video chapters list background
    };

    private static final int[] DARK_VALUES = {
            -14145496,  // drawer content view background
            -14606047,  // comments chip background
            -15198184,  // music related results panel background
            -15790321,  // comments chip background (new layout)
            -98492127   // video chapters list background
    };

    // background colors
    private static int whiteColor = 0;
    private static int blackColor = 0;

    public static int getLithoColor(int originalValue) {
        if (anyEquals(originalValue, DARK_VALUES)) {
            return getBlackColor();
        } else if (anyEquals(originalValue, WHITE_VALUES)) {
            return getWhiteColor();
        }
        return originalValue;
    }

    private static int getBlackColor() {
        if (blackColor == 0) blackColor = ResourceUtils.getColor("yt_black1");
        return blackColor;
    }

    private static int getWhiteColor() {
        if (whiteColor == 0) whiteColor = ResourceUtils.getColor("yt_white1");
        return whiteColor;
    }

    private static boolean anyEquals(int value, int... of) {
        for (int v : of) if (value == v) return true;
        return false;
    }
}


