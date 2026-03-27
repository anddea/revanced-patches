package app.morphe.extension.youtube.patches.general;

import static app.morphe.extension.shared.utils.Utils.hideViewUnderCondition;
import static app.morphe.extension.youtube.shared.NavigationBar.NavigationButton;

import android.content.Context;
import android.content.Intent;
import android.text.Spanned;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.BooleanUtils;

import java.lang.ref.WeakReference;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.NavigationBar;
import app.morphe.extension.youtube.shared.RootView;
import app.morphe.extension.youtube.utils.ExtendedUtils;

@SuppressWarnings("unused")
public final class NavigationButtonsPatch {

    private static final boolean ENABLE_NARROW_NAVIGATION_BUTTONS
            = Settings.ENABLE_NARROW_NAVIGATION_BUTTONS.get();

    private static final boolean ENABLE_TRANSLUCENT_NAVIGATION_BAR
            = Settings.ENABLE_TRANSLUCENT_NAVIGATION_BAR.get();

    private static final boolean HIDE_NAVIGATION_LABEL
            = Settings.HIDE_NAVIGATION_LABEL.get();

    private static final boolean HIDE_NAVIGATION_BAR
            = Settings.HIDE_NAVIGATION_BAR.get();

    private static final boolean SWITCH_CREATE_WITH_NOTIFICATIONS_BUTTON
            = Settings.SWITCH_CREATE_WITH_NOTIFICATIONS_BUTTON.get();

    private static final boolean REPLACE_NAVIGATION_BUTTON
            = Settings.REPLACE_NAVIGATION_BUTTON.get();
    private static final NavigationButton REPLACE_NAVIGATION_BUTTON_TARGET
            = Settings.REPLACE_NAVIGATION_BUTTON_TARGET.get();
    private static final List<String> REPLACE_NAVIGATION_BUTTON_TARGET_ENUM_NAMES
            = REPLACE_NAVIGATION_BUTTON_TARGET.ytEnumNames;

    private static final boolean SET_CAIRO_NOTIFICATION_FILLED_ICON
            = !REPLACE_NAVIGATION_BUTTON || REPLACE_NAVIGATION_BUTTON_TARGET != NavigationButton.NOTIFICATIONS;

    private static Map<NavigationButton, Boolean> shouldHideMap;

    private static volatile WeakReference<TextView> searchQueryRef = new WeakReference<>(null);

    private static View.OnClickListener openSearchBar;

    private static final View.OnClickListener openSearchBarOnClickListener = v -> {
        if (RootView.isSearchBarActive() && searchQueryRef.get() != null) {
            searchQueryRef.get().callOnClick();
        } else if (openSearchBar != null) {
            openSearchBar.onClick(v);
        } else {
            Context context = v.getContext();
            Intent intent = new Intent();
            intent.setAction("com.google.android.youtube.action.open.search");
            intent.setPackage(context.getPackageName());
            context.startActivity(intent);
        }
    };

    private static int emptyContentCountId = -1;
    private static int emptyContentDotId = -1;
    private static int libraryCairoId = -1;


    /**
     * Injection point.
     */
    public static boolean enableNarrowNavigationButton(boolean original) {
        return ENABLE_NARROW_NAVIGATION_BUTTONS || original;
    }

    /**
     * Injection point.
     */
    public static boolean enableTranslucentNavigationBar() {
        return ENABLE_TRANSLUCENT_NAVIGATION_BAR;
    }

    /**
     * Injection point.
     *
     * @noinspection ALL
     */
    public static void setCairoNotificationFilledIcon(EnumMap enumMap, Enum tabActivityCairo) {
        if (SET_CAIRO_NOTIFICATION_FILLED_ICON) {
            final int fillBellCairoBlack = ResourceUtils.getDrawableIdentifier("yt_fill_bell_cairo_black_24");
            if (fillBellCairoBlack != 0) {
                // It's very unlikely, but Google might fix this issue someday.
                // If so, [fillBellCairoBlack] might already be in enumMap.
                // That's why 'EnumMap.putIfAbsent()' is used instead of 'EnumMap.put()'.
                enumMap.putIfAbsent(tabActivityCairo, Integer.valueOf(fillBellCairoBlack));
            }
        }
    }

    private static int getEmptyContentCountId() {
        if (emptyContentCountId == -1) {
            emptyContentCountId = ResourceUtils.getIdIdentifier("empty_content_count");
        }
        return emptyContentCountId;
    }

    /**
     * Injection point.
     */
    public static View getContentCountId(View view, int original) {
        if (REPLACE_NAVIGATION_BUTTON && shouldReplace()) {
            int id = getEmptyContentCountId();
            if (id != 0) {
                View emptyView = view.findViewById(id);
                if (emptyView != null) {
                    return emptyView;
                }
            }
        }

        return view.findViewById(original);
    }

    private static int getEmptyContentDotId() {
        if (emptyContentDotId == -1) {
            emptyContentDotId = ResourceUtils.getIdIdentifier("empty_content_dot");
        }
        return emptyContentDotId;
    }

    /**
     * Injection point.
     */
    public static View getContentDotId(View view, int original) {
        if (REPLACE_NAVIGATION_BUTTON && shouldReplace()) {
            int id = getEmptyContentDotId();
            if (id != 0) {
                View emptyView = view.findViewById(id);
                if (emptyView != null) {
                    return emptyView;
                }
            }
        }

        return view.findViewById(original);
    }

    private static int getLibraryCairoId() {
        if (libraryCairoId == -1) {
            libraryCairoId = ResourceUtils.getIdIdentifier("yt_outline_library_cairo_black_24");
        }
        return libraryCairoId;
    }

    /**
     * Injection point.
     */
    public static int getLibraryDrawableId(int original) {
        if (ExtendedUtils.IS_19_26_OR_GREATER &&
                !ExtendedUtils.isSpoofingToLessThan("19.27.00")) {
            int libraryCairoId = getLibraryCairoId();
            if (libraryCairoId != 0) {
                return libraryCairoId;
            }
        }
        return original;
    }

    /**
     * Toolbar buttons (including the YouTube logo) and navigation bar buttons depend on the
     * '<a href="https://www.youtube.com/youtubei/v1/guide">'/guide' endpoint</a>' requests.
     * <p>
     * Therefore, the patch works if the 'osName' value is spoofed only in '/guide' endpoint requests.
     *
     * @return osName.
     */
    public static String getOSName() {
        return SWITCH_CREATE_WITH_NOTIFICATIONS_BUTTON
                ? "Android Automotive"
                // If the setting is off, it should return the original osName (override).
                // Otherwise, there may be interference with the 'Hide ads' patch.
                : ExtendedUtils.getOSName();
    }

    /**
     * Injection point.
     */
    public static void setSearchBarOnClickListener(View.OnClickListener listener) {
        if (REPLACE_NAVIGATION_BUTTON) {
            openSearchBar = listener;
        }
    }

    private static boolean shouldReplace() {
        return shouldReplace(NavigationBar.getLastAppNavigationEnum());
    }

    private static boolean shouldReplace(@Nullable Enum<?> navigationEnum) {
        return navigationEnum != null && shouldReplace(navigationEnum.name());
    }

    private static boolean shouldReplace(String lastEnumName) {
        return lastEnumName != null && REPLACE_NAVIGATION_BUTTON_TARGET_ENUM_NAMES.contains(lastEnumName);
    }

    /**
     * Injection point.
     */
    @Nullable
    public static Enum<?> changeIconType(@Nullable Enum<?> original) {
        if (REPLACE_NAVIGATION_BUTTON && shouldReplace(original)) {
            String enumName = original.name();
            return original.name().endsWith("CAIRO")
                    ? YouTubeIcon.searchCairo
                    : YouTubeIcon.search;
        }

        return original;
    }

    /**
     * Injection point.
     */
    public static Spanned changeSpanned(Spanned original) {
        if (REPLACE_NAVIGATION_BUTTON &&
                // If the navigation bar label is hidden, there is no need to replace Spanned.
                !HIDE_NAVIGATION_LABEL &&
                shouldReplace()
        ) {
            String lastYTNavigationEnumName = NavigationBar.getLastAppNavigationEnum();
            return Utils.newSpanUsingStylingOfAnotherSpan(original, ResourceUtils.getString("menu_search"));
        }
        return original;
    }

    /**
     * Injection point.
     */
    public static void searchQueryViewLoaded(TextView searchQuery) {
        if (REPLACE_NAVIGATION_BUTTON) {
            searchQueryRef = new WeakReference<>(searchQuery);
        }
    }

    private static Map<NavigationButton, Boolean> getHideMap() {
        if (shouldHideMap == null || shouldHideMap.isEmpty()) {
            shouldHideMap = new EnumMap<>(NavigationButton.class) {
                {
                    put(NavigationButton.HOME, Settings.HIDE_NAVIGATION_HOME_BUTTON.get());
                    put(NavigationButton.SHORTS, Settings.HIDE_NAVIGATION_SHORTS_BUTTON.get());
                    put(NavigationButton.SUBSCRIPTIONS, Settings.HIDE_NAVIGATION_SUBSCRIPTIONS_BUTTON.get());
                    put(NavigationButton.CREATE, Settings.HIDE_NAVIGATION_CREATE_BUTTON.get());
                    put(NavigationButton.NOTIFICATIONS, Settings.HIDE_NAVIGATION_NOTIFICATIONS_BUTTON.get());
                    put(NavigationButton.LIBRARY, Settings.HIDE_NAVIGATION_LIBRARY_BUTTON.get());
                }
            };
        }
        return shouldHideMap;
    }

    /**
     * Injection point.
     */
    public static void navigationTabCreated(NavigationButton button, View tabView) {
        if (REPLACE_NAVIGATION_BUTTON && button == REPLACE_NAVIGATION_BUTTON_TARGET) {
            tabView.setOnClickListener(openSearchBarOnClickListener);
            Utils.runOnMainThread(() -> tabView.setOnClickListener(openSearchBarOnClickListener));

            return;
        }

        if (BooleanUtils.isTrue(getHideMap().get(button))) {
            tabView.setVisibility(View.GONE);
        }
    }

    /**
     * Injection point.
     */
    public static void hideNavigationLabel(TextView view) {
        hideViewUnderCondition(HIDE_NAVIGATION_LABEL, view);
    }

    /**
     * Injection point.
     */
    public static void hideNavigationBar(View view) {
        hideViewUnderCondition(HIDE_NAVIGATION_BAR, view);
    }

}
