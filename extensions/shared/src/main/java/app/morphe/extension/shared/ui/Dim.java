package app.morphe.extension.shared.ui;

import app.morphe.extension.shared.utils.Utils;

public final class Dim {
    public static final int dp4 = Utils.dipToPixels(4);
    public static final int dp6 = Utils.dipToPixels(6);
    public static final int dp8 = Utils.dipToPixels(8);
    public static final int dp12 = Utils.dipToPixels(12);
    public static final int dp16 = Utils.dipToPixels(16);
    public static final int dp20 = Utils.dipToPixels(20);
    public static final int dp24 = Utils.dipToPixels(24);
    public static final int dp32 = Utils.dipToPixels(32);
    public static final int dp36 = Utils.dipToPixels(36);
    public static final int dp40 = Utils.dipToPixels(40);
    public static final int dp48 = Utils.dipToPixels(48);

    public static android.util.DisplayMetrics getMetrics() {
        return Utils.getResources().getDisplayMetrics();
    }

    public static int dp(int value) {
        return Utils.dipToPixels(value);
    }
}
