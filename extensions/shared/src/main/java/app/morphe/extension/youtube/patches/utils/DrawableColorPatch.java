package app.morphe.extension.youtube.patches.utils;

import org.apache.commons.lang3.ArrayUtils;

import app.morphe.extension.shared.utils.ResourceUtils;

@SuppressWarnings("unused")
public class DrawableColorPatch {
    private static final int[] DARK_COLORS = {
            0xFF282828, // drawer content view background
            0xFF212121, // comments chip background
            0xFF181818, // music related results panel background
            0xFF0F0F0F, // comments chip background (new layout)
            0xFA212121, // video chapters list background
    };

    private static final int[] LIGHT_COLORS = {
            -1,         // comments chip background
            0xFFF9F9F9, // music related results panel background
            0xFAFFFFFF, // video chapters list background
    };

    // background colors
    private static int whiteColor = 0;
    private static int blackColor = 0;

    public static int getLithoColor(int colorValue) {
        if (ArrayUtils.contains(DARK_COLORS, colorValue)) {
            return getBlackColor();
        } else if (ArrayUtils.contains(LIGHT_COLORS, colorValue)) {
            return getWhiteColor();
        }
        return colorValue;
    }

    private static int getBlackColor() {
        if (blackColor == 0) blackColor = ResourceUtils.getColor("yt_black1");
        return blackColor;
    }

    private static int getWhiteColor() {
        if (whiteColor == 0) whiteColor = ResourceUtils.getColor("yt_white1");
        return whiteColor;
    }
}


