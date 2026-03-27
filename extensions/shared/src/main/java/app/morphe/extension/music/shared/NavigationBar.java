package app.morphe.extension.music.shared;

@SuppressWarnings("unused")
public final class NavigationBar {
    private static volatile int lastIndex = 0;

    /**
     * Injection point.
     */
    public static void navigationTabSelected(int index, boolean isSelected) {
        if (isSelected) {
            lastIndex = index;
        }
    }

    public static int getNavigationTabIndex() {
        return lastIndex;
    }
}
