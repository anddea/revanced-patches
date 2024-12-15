package app.revanced.extension.music.patches.utils;

@SuppressWarnings("unused")
public class DrawableColorPatch {
    private static final int[] DARK_VALUES = {
            -14606047 // comments box background
    };

    public static int getLithoColor(int originalValue) {
        if (anyEquals(originalValue, DARK_VALUES))
            return -16777215;

        return originalValue;
    }

    private static boolean anyEquals(int value, int... of) {
        for (int v : of) if (value == v) return true;
        return false;
    }
}


