/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Portions of this file are modified by anddea:
 * Copyright (C) 2026 anddea
 * https://github.com/anddea/revanced-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.patches.general;

import static app.morphe.extension.shared.utils.Utils.hideViewUnderCondition;
import static app.morphe.extension.youtube.shared.NavigationBar.NavigationButton;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.protobuf.MessageLite;

import org.apache.commons.lang3.BooleanUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import app.morphe.extension.shared.settings.IntegerSetting;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.innertube.GuideResponseOuterClass.Accessibility;
import app.morphe.extension.youtube.innertube.GuideResponseOuterClass.AccessibilityData;
import app.morphe.extension.youtube.innertube.GuideResponseOuterClass.ButtonRenderer;
import app.morphe.extension.youtube.innertube.GuideResponseOuterClass.Buttons;
import app.morphe.extension.youtube.innertube.GuideResponseOuterClass.PivotBarItemRenderer;
import app.morphe.extension.youtube.innertube.IconOuterClass.Icon;
import app.morphe.extension.youtube.innertube.IconOuterClass.YTIconType;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.RootView;
import app.morphe.extension.youtube.utils.ExtendedUtils;

@SuppressWarnings("unused")
public final class NavigationButtonsPatch {

    private static final boolean ENABLE_NARROW_NAVIGATION_BUTTONS
            = Settings.ENABLE_NARROW_NAVIGATION_BUTTONS.get();

    private static final boolean DISABLE_TRANSLUCENT_STATUS_BAR
            = Settings.DISABLE_TRANSLUCENT_STATUS_BAR.get();

    private static final boolean DISABLE_TRANSLUCENT_NAVIGATION_BAR
            = Settings.DISABLE_TRANSLUCENT_NAVIGATION_BAR.get();

    private static final boolean HIDE_NAVIGATION_LABEL
            = Settings.HIDE_NAVIGATION_LABEL.get();

    private static final boolean HIDE_NAVIGATION_BAR
            = Settings.HIDE_NAVIGATION_BAR.get();

    private static final boolean DISABLE_AUTO_HIDE_NAVIGATION_BAR
            = Settings.DISABLE_AUTO_HIDE_NAVIGATION_BAR.get();

    private static final boolean SWITCH_CREATE_WITH_NOTIFICATIONS_BUTTON
            = Settings.SWITCH_CREATE_WITH_NOTIFICATIONS_BUTTON.get();

    private static Map<NavigationButton, Boolean> shouldHideMap;

    private static volatile WeakReference<TextView> searchQueryRef = new WeakReference<>(null);

    private static View.OnClickListener openSearchBar;

    private static final boolean SHOW_SETTINGS_BUTTON = Settings.SHOW_SETTINGS_BUTTON.get();
    private static final boolean SHOW_SETTINGS_BUTTON_TYPE = Settings.SHOW_SETTINGS_BUTTON_TYPE.get();
    private static final boolean SHOW_SEARCH_BUTTON = Settings.SHOW_SEARCH_BUTTON.get();

    private static final boolean SHOW_TOOLBAR_SETTINGS_BUTTON =
            Settings.SHOW_TOOLBAR_SETTINGS_BUTTON.get();
    private static final IntegerSetting SHOW_TOOLBAR_SETTINGS_BUTTON_INDEX =
            Settings.SHOW_TOOLBAR_SETTINGS_BUTTON_INDEX;
    private static final boolean SHOW_TOOLBAR_SETTINGS_BUTTON_TYPE =
            Settings.SHOW_TOOLBAR_SETTINGS_BUTTON_TYPE.get();

    private static final String SETTINGS_BUTTON_ENUM_NAME = "SETTINGS_CAIRO";

    private static Object pivotBarSettingsRenderer;
    private static Object pivotBarSearchRenderer;

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
    public static boolean useAnimatedNavigationButtons(boolean original) {
        return Settings.NAVIGATION_BAR_ANIMATIONS.get();
    }

    /**
     * Injection point.
     */
    public static boolean allowCollapsingToolbarLayout(boolean original) {
        if (DISABLE_TRANSLUCENT_STATUS_BAR) return false;
        return original;
    }

    /**
     * Injection point.
     */
    public static boolean useTranslucentNavigationButtons(boolean original) {
        // Feature requires Android 13+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return original;
        }

        if (DISABLE_TRANSLUCENT_NAVIGATION_BAR) {
            return false;
        }

        return original;
    }

    /**
     * Injection point.
     *
     * @noinspection ALL
     */
    public static void setCairoNotificationFilledIcon(EnumMap enumMap, Enum tabActivityCairo) {
        final int fillBellCairoBlack = ResourceUtils.getDrawableIdentifier("yt_fill_bell_cairo_black_24");
        if (fillBellCairoBlack != 0) {
            // It's very unlikely, but Google might fix this issue someday.
            // If so, [fillBellCairoBlack] might already be in enumMap.
            // That's why 'EnumMap.putIfAbsent()' is used instead of 'EnumMap.put()'.
            enumMap.putIfAbsent(tabActivityCairo, Integer.valueOf(fillBellCairoBlack));
        }
    }

    /**
     * Injection point.
     */
    public static View getContentCountId(View view, int original) {
        return view.findViewById(original);
    }

    /**
     * Injection point.
     */
    public static View getContentDotId(View view, int original) {
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
    public static void setSearchBarOnClickListener(MessageLite messageLite, View.OnClickListener listener) {
        if (SHOW_SEARCH_BUTTON) {
            try {
                var buttonRenderer = ButtonRenderer.parseFrom(messageLite.toByteArray());
                if (buttonRenderer.hasIcon()) {
                    var iconName = buttonRenderer.getIcon().getYtIconType().name();

                    if (NavigationButton.SEARCH.ytEnumNames.contains(iconName)) {
                        openSearchBar = listener;
                    }
                }
            } catch (Exception ex) {
                Logger.printException(() -> "Failed to set search bar OnClickListener", ex);
            }
        }
    }

    /**
     * Clones the Home tab renderer while preserving YouTube's unknown proto fields and layout
     * metadata. The returned renderer is used for the requested additional navigation button.
     */
    @Nullable
    private static byte[] parseAdditionalPivotBarItemRenderer(
            MessageLite messageLite, YTIconType iconType, String label) {
        try {
            var builder = PivotBarItemRenderer.parseFrom(messageLite.toByteArray()).toBuilder();
            int originalIconType = builder.getIcon().getYtIconTypeValue();
            String iconName = builder.getIcon().getYtIconType().name();
            boolean isHome = NavigationButton.HOME.ytEnumNames.contains(iconName)
                    || originalIconType == 65
                    || originalIconType == 406
                    || originalIconType == 1154;
            if (isHome) {
                var accessibilityData = AccessibilityData.newBuilder()
                        .setLabel(ResourceUtils.getString(label))
                        .build();
                var accessibility = Accessibility.newBuilder()
                        .setAccessibilityData(accessibilityData)
                        .build();
                var icon = Icon.newBuilder().setYtIconType(iconType).build();

                builder.clearAccessibility();
                builder.setAccessibility(accessibility);
                builder.clearIcon();
                builder.setIcon(icon);
                return builder.build().toByteArray();
            }
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to parse additional PivotBarItemRenderer", ex);
        }
        return null;
    }

    /** Creates the additional Settings renderer from YouTube's Home renderer. */
    @Nullable
    public static byte[] parseSettingsPivotBarItemRenderer(MessageLite messageLite) {
        return SHOW_SETTINGS_BUTTON
                ? parseAdditionalPivotBarItemRenderer(messageLite, YTIconType.SETTINGS_CAIRO, "menu_settings")
                : null;
    }

    /** Creates the additional Search renderer from YouTube's Home renderer. */
    @Nullable
    public static byte[] parseSearchPivotBarItemRenderer(MessageLite messageLite) {
        return SHOW_SEARCH_BUTTON
                ? parseAdditionalPivotBarItemRenderer(messageLite, YTIconType.SEARCH_CAIRO, "menu_search")
                : null;
    }

    /**
     * Injection point. Stores the cloned Settings renderer until YouTube builds the pivot-bar list.
     */
    public static void setPivotBarSettingsRenderer(Object renderer) {
        if (SHOW_SETTINGS_BUTTON) {
            pivotBarSettingsRenderer = renderer;
        }
    }

    /** Injection point. Stores the cloned Search renderer until YouTube builds the pivot-bar list. */
    public static void setPivotBarSearchRenderer(Object renderer) {
        if (SHOW_SEARCH_BUTTON) {
            pivotBarSearchRenderer = renderer;
        }
    }

    /**
     * Injection point. Adds optional renderers without mutating YouTube's immutable proto list.
     */
    public static List<Object> getPivotBarRendererList(List<Object> list) {
        if (list == null || list.isEmpty()) {
            return list;
        }

        List<Object> newList = new ArrayList<>(list);
        if (SHOW_SETTINGS_BUTTON && pivotBarSettingsRenderer != null) {
            newList.add(pivotBarSettingsRenderer);
        }
        if (SHOW_SEARCH_BUTTON && pivotBarSearchRenderer != null) {
            newList.add(pivotBarSearchRenderer);
        }

        return reorderPivotBarRendererList(newList);
    }

    /**
     * Reorders the renderers while leaving YouTube's original order untouched until the user
     * saves an order. Unknown renderer types and optional buttons remain in their original slots.
     */
    private static List<Object> reorderPivotBarRendererList(List<Object> list) {
        List<NavigationButton> configuredOrder = getConfiguredNavigationButtonOrder();
        if (configuredOrder.isEmpty()) {
            return list;
        }

        EnumMap<NavigationButton, Object> rendererByButton = new EnumMap<>(NavigationButton.class);
        List<NavigationButton> originalButtons = new ArrayList<>();
        for (Object renderer : list) {
            NavigationButton button = getNavigationButton(renderer);
            if (button != null) {
                // A guide response should contain one renderer per navigation button. If that
                // changes, leave the response alone rather than risk replacing the wrong slot.
                if (rendererByButton.putIfAbsent(button, renderer) != null) {
                    return list;
                }
                originalButtons.add(button);
            }
        }

        if (rendererByButton.isEmpty()) {
            return list;
        }

        List<Object> orderedRenderers = new ArrayList<>(rendererByButton.size());
        Set<NavigationButton> placedButtons = new HashSet<>();
        for (NavigationButton button : configuredOrder) {
            Object renderer = rendererByButton.get(button);
            if (renderer != null && placedButtons.add(button)) {
                orderedRenderers.add(renderer);
            }
        }
        for (NavigationButton button : originalButtons) {
            if (placedButtons.add(button)) {
                orderedRenderers.add(rendererByButton.get(button));
            }
        }

        List<Object> reorderedList = new ArrayList<>(list);
        int orderedIndex = 0;
        for (int i = 0; i < list.size(); i++) {
            if (getNavigationButton(list.get(i)) != null) {
                reorderedList.set(i, orderedRenderers.get(orderedIndex++));
            }
        }
        return reorderedList;
    }

    /**
     * Reads the exported order format and ignores invalid or duplicate entries so a malformed
     * imported setting cannot prevent the navigation bar from being built.
     */
    private static List<NavigationButton> getConfiguredNavigationButtonOrder() {
        String serializedOrder = Settings.NAVIGATION_BAR_ORDER.get();
        if (serializedOrder.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<NavigationButton> order = new ArrayList<>();
        Set<NavigationButton> seenButtons = new HashSet<>();
        for (String value : serializedOrder.split(",")) {
            try {
                NavigationButton button = NavigationButton.valueOf(value.trim().toUpperCase(Locale.ROOT));
                if (seenButtons.add(button)) {
                    order.add(button);
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore unknown values so future YouTube buttons remain usable.
            }
        }
        return order;
    }

    /**
     * Identifies a pivot-bar renderer by its raw icon enum value. The YouTube extension proto
     * intentionally contains only a small icon subset, so raw values also support newer icons.
     */
    @Nullable
    private static NavigationButton getNavigationButton(Object renderer) {
        if (renderer == pivotBarSettingsRenderer) {
            return NavigationButton.SETTINGS;
        }
        if (renderer == pivotBarSearchRenderer) {
            return NavigationButton.SEARCH;
        }

        MessageLite messageLite = getRendererMessage(renderer);
        if (messageLite == null) {
            return null;
        }

        NavigationButton buttonById = getNavigationButtonByRendererId(messageLite);
        if (buttonById != null) {
            return buttonById;
        }

        try {
            int iconType = PivotBarItemRenderer.parseFrom(messageLite.toByteArray())
                    .getIcon()
                    .getYtIconTypeValue();
            return switch (iconType) {
                case 60, 1045, 1160 -> NavigationButton.SEARCH;
                case 65, 406, 1154 -> NavigationButton.HOME;
                case 66, 408, 1155 -> NavigationButton.SUBSCRIPTIONS;
                case 292, 777 -> NavigationButton.EXPLORE;
                case 355, 1156 -> NavigationButton.NOTIFICATIONS;
                case 405, 650, 670, 730, 734, 1161 -> NavigationButton.CREATE;
                case 68, 239, 410, 483, 504 -> NavigationButton.LIBRARY;
                case 44, 1162 -> NavigationButton.SETTINGS;
                case 776, 785, 1157 -> NavigationButton.SHORTS;
                default -> null;
            };
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private static MessageLite getRendererMessage(Object renderer) {
        if (renderer instanceof MessageLite directMessage) {
            return directMessage;
        }

        try {
            Object wrappedMessage = renderer.getClass().getField("a").get(renderer);
            return wrappedMessage instanceof MessageLite messageLite ? messageLite : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Identifies current guide renderers by their stable FE* navigation identifier. */
    @Nullable
    private static NavigationButton getNavigationButtonByRendererId(MessageLite renderer) {
        try {
            Object rendererId = renderer.getClass().getField("e").get(renderer);
            if (!(rendererId instanceof String id)) {
                return null;
            }

            return switch (id) {
                case "FEwhat_to_watch" -> NavigationButton.HOME;
                case "FEshorts" -> NavigationButton.SHORTS;
                case "FEsubscriptions" -> NavigationButton.SUBSCRIPTIONS;
                case "FElibrary" -> NavigationButton.LIBRARY;
                case "FEactivity", "FEnotifications" -> NavigationButton.NOTIFICATIONS;
                case "FEexplore" -> NavigationButton.EXPLORE;
                case "FEsearch" -> NavigationButton.SEARCH;
                case "FEsettings" -> NavigationButton.SETTINGS;
                default -> null;
            };
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Clones a native toolbar button and changes only its icon and accessibility metadata.
     * Unknown proto fields are retained so the button remains compatible across versions.
     */
    @Nullable
    public static byte[] createToolbarSettingsButton(List<MessageLite> rawButtonList) {
        if (!SHOW_TOOLBAR_SETTINGS_BUTTON || rawButtonList == null || rawButtonList.isEmpty()) {
            return null;
        }

        try {
            for (MessageLite message : rawButtonList) {
                Buttons buttons = Buttons.parseFrom(message.toByteArray());
                if (buttons.hasButtonRenderer() && buttons.getButtonRenderer().hasIcon()) {
                    ButtonRenderer.Builder renderer = buttons.getButtonRenderer().toBuilder();
                    renderer.clearButtonRendererAccessibilityData();
                    renderer.clearRendererAccessibilityData();
                    renderer.setIcon(
                            Icon.newBuilder()
                                    .setYtIconType(YTIconType.SETTINGS_CAIRO)
                                    .build()
                    );

                    return buttons.toBuilder()
                            .setButtonRenderer(renderer.build())
                            .build()
                            .toByteArray();
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to create toolbar Settings button", ex);
        }

        return null;
    }

    /**
     * Moves the newly appended Settings button to the configured one-based toolbar index.
     */
    public static void applyToolbarSettingsButtonIndex(List<MessageLite> rawButtonList) {
        if (!SHOW_TOOLBAR_SETTINGS_BUTTON || rawButtonList == null || rawButtonList.isEmpty()) {
            return;
        }

        int targetIndex = SHOW_TOOLBAR_SETTINGS_BUTTON_INDEX.get() - 1;
        targetIndex = Math.max(0, Math.min(targetIndex, rawButtonList.size() - 1));

        MessageLite settingsButton = rawButtonList.remove(rawButtonList.size() - 1);
        rawButtonList.add(targetIndex, settingsButton);
    }

    /**
     * Replaces the cloned native listener after YouTube finishes binding the toolbar button.
     */
    public static void setToolbarSettingsOnClickListener(String enumName, View toolbarView) {
        if (!SHOW_TOOLBAR_SETTINGS_BUTTON || !SETTINGS_BUTTON_ENUM_NAME.equals(enumName)
                || !(toolbarView instanceof ViewGroup viewGroup)) {
            return;
        }

        ImageView imageView = Utils.getChildView(viewGroup, view -> view instanceof ImageView);
        if (imageView == null) {
            return;
        }

        Utils.runOnMainThreadDelayed(() -> {
            imageView.setClickable(true);
            if (SHOW_TOOLBAR_SETTINGS_BUTTON_TYPE) {
                imageView.setOnClickListener(GeneralPatch::openRVXSettings);
                imageView.setOnLongClickListener(button -> {
                    GeneralPatch.openYouTubeSettings(button);
                    return true;
                });
            } else {
                imageView.setOnClickListener(GeneralPatch::openYouTubeSettings);
                imageView.setOnLongClickListener(button -> {
                    GeneralPatch.openRVXSettings(button);
                    return true;
                });
            }
        }, 100);
    }

    /**
     * Injection point.
     */
    public static void searchQueryViewLoaded(TextView searchQuery) {
        if (SHOW_SEARCH_BUTTON) {
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
        if (SHOW_SETTINGS_BUTTON && button == NavigationButton.SETTINGS) {
            Utils.runOnMainThread(() -> tabView.setOnClickListener(v -> {
                if (SHOW_SETTINGS_BUTTON_TYPE) {
                    GeneralPatch.openRVXSettings(v);
                } else {
                    GeneralPatch.openYouTubeSettings(v);
                }
            }));
            return;
        }

        if (SHOW_SEARCH_BUTTON && button == NavigationButton.SEARCH) {
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

    /**
     * Injection point.
     */
    public static boolean disableAutoHidingNavigationBar() {
        return DISABLE_AUTO_HIDE_NAVIGATION_BAR;
    }

}
