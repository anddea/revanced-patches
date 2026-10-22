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
import android.view.WindowInsets;
import androidx.annotation.ChecksSdkIntAtLeast;
import app.morphe.extension.shared.ui.Dim;
import app.morphe.extension.shared.utils.ResourceType;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.MessageLite;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

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
import app.morphe.extension.youtube.innertube.GuideResponseOuterClass.ButtonRendererAccessibilityData;
import app.morphe.extension.youtube.innertube.GuideResponseOuterClass.ButtonSheetRenderer;
import app.morphe.extension.youtube.innertube.GuideResponseOuterClass.Buttons;
import app.morphe.extension.youtube.innertube.GuideResponseOuterClass.CreationEntrySheetEndpoint;
import app.morphe.extension.youtube.innertube.GuideResponseOuterClass.CreationMode;
import app.morphe.extension.youtube.innertube.GuideResponseOuterClass.CreationModesCommand;
import app.morphe.extension.youtube.innertube.GuideResponseOuterClass.CreationModesControlsRenderer;
import app.morphe.extension.youtube.innertube.GuideResponseOuterClass.CreationModesControlsRendererHolder;
import app.morphe.extension.youtube.innertube.GuideResponseOuterClass.CreationModesSwitcherRenderer;
import app.morphe.extension.youtube.innertube.GuideResponseOuterClass.CreationModesSwitcherRendererHolder;
import app.morphe.extension.youtube.innertube.GuideResponseOuterClass.NavigationEndpoint;
import app.morphe.extension.youtube.innertube.GuideResponseOuterClass.PivotBarItemRenderer;
import app.morphe.extension.youtube.innertube.GuideResponseOuterClass.RendererAccessibilityData;
import app.morphe.extension.youtube.innertube.IconOuterClass.Icon;
import app.morphe.extension.youtube.innertube.IconOuterClass.YTIconType;
import app.morphe.extension.youtube.patches.general.offline.OfflineScreens;
import app.morphe.extension.youtube.patches.theme.ThemePatch.StatusBarTranslucency;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.RootView;
import app.morphe.extension.youtube.utils.ExtendedUtils;

@SuppressWarnings({"deprecation", "unused"})
public final class NavigationButtonsPatch {

    private static final boolean ENABLE_NARROW_NAVIGATION_BUTTONS
            = Settings.ENABLE_NARROW_NAVIGATION_BUTTONS.get();

    private static final StatusBarTranslucency STATUS_BAR_TRANSLUCENCY
            = Settings.STATUS_BAR_TRANSLUCENCY.get();

    private static final boolean DISABLE_TRANSLUCENT_NAVIGATION_BAR
            = Settings.DISABLE_TRANSLUCENT_NAVIGATION_BAR.get();

    private static final boolean HIDE_NAVIGATION_LABEL
            = Settings.HIDE_NAVIGATION_LABEL.get();

    private static final boolean HIDE_NAVIGATION_BAR
            = Settings.HIDE_NAVIGATION_BAR.get();

    private static final boolean HIDE_SHORTS_NAVIGATION_BAR = Settings.HIDE_SHORTS_NAVIGATION_BAR.get();

    /**
     * The view the Shorts player is in, inside the layout of the Shorts player fragment.
     */
    private static final int shortsPlayerContainerId = HIDE_SHORTS_NAVIGATION_BAR
            ? ResourceUtils.getIdentifier(ResourceType.ID, "fragment_container_view")
            : 0;

    /**
     * If the room below the Shorts player is set by {@link #setShortsPlayerBottomInset(View)}.
     * The system navigation buttons are read from the window insets, available from Android 11.
     */
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    private static final boolean SETS_SHORTS_PLAYER_BOTTOM_ROOM = HIDE_SHORTS_NAVIGATION_BAR
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;

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
    private static final boolean SHOW_TOOLBAR_SETTINGS_BUTTON_TYPE_IN_YOU_TAB =
            Settings.SHOW_TOOLBAR_SETTINGS_BUTTON_TYPE_IN_YOU_TAB.get();

    private static final String SETTINGS_BUTTON_ENUM_NAME = "SETTINGS_CAIRO";

    private static final String[] CREATE_BUTTON_ENUMS = {
            "CREATION_ENTRY", // Phone layout.
            "FAB_CAMERA" // Tablet layout.
    };

    private static final String[] NOTIFICATION_BUTTON_ENUMS = {
            "TAB_ACTIVITY_CAIRO", // New layout.
            "TAB_ACTIVITY" // Old layout.
    };

    private static final boolean HIDE_TOOLBAR_CREATE_BUTTON =
            Settings.HIDE_TOOLBAR_CREATE_BUTTON.get();
    private static final boolean HIDE_TOOLBAR_NOTIFICATION_BUTTON =
            Settings.HIDE_TOOLBAR_NOTIFICATION_BUTTON.get();
    private static final boolean HIDE_TOOLBAR_SEARCH_BUTTON =
            Settings.HIDE_TOOLBAR_SEARCH_BUTTON.get();

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
     * <p>
     * With the translucent navigation bar turned on, the window is drawn behind the system
     * navigation buttons, the navigation bar makes room for them, and the pages end above
     * the navigation bar. With the navigation bar hidden in the Shorts player, nothing makes that
     * room, so the Shorts player is drawn behind the system navigation buttons
     * (https://github.com/MorpheApp/morphe-patches/issues/3908). The Shorts player then makes
     * the room itself.
     * <p>
     * The room the Shorts player reserves for the navigation bar depends on how it's opened,
     * so with the navigation bar hidden that room is left out ({@link #getShortsPlayerBottomRoom(int)}),
     * and the Shorts player always ends a slight room above the system navigation buttons,
     * or the seekbar thumb at the bottom of the Shorts player overlaps them.
     *
     * @param shortsFragmentLayout The layout of the Shorts player fragment.
     */
    public static void setShortsPlayerBottomInset(View shortsFragmentLayout) {
        if (!SETS_SHORTS_PLAYER_BOTTOM_ROOM) {
            return;
        }

        try {
            shortsFragmentLayout.addOnLayoutChangeListener((view, left, top, right, bottom,
                                                            oldLeft, oldTop, oldRight, oldBottom) -> {
                try {
                    WindowInsets insets = view.getRootWindowInsets();
                    View shortsPlayerContainer = view.findViewById(shortsPlayerContainerId);
                    if (insets == null || shortsPlayerContainer == null) return;

                    // The window can already end above the system navigation buttons, such as
                    // without the translucent navigation bar, so only the room still missing
                    // is added. The padding added here is left out, or the room would be counted twice.
                    final int navigationButtonsHeight = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
                    int[] location = new int[2];
                    shortsPlayerContainer.getLocationInWindow(location);
                    final int roomBelow = view.getRootView().getHeight()
                            - (location[1] + shortsPlayerContainer.getHeight())
                            - view.getPaddingBottom();
                    final int padding = Math.max(0, navigationButtonsHeight + Dim.dp12 - roomBelow);

                    if (view.getPaddingBottom() != padding) {
                        view.post(() -> view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                                view.getPaddingRight(), padding));
                    }
                } catch (Exception ex) {
                    Logger.printException(() -> "setShortsPlayerBottomInset layout failure", ex);
                }
            });
        } catch (Exception ex) {
            Logger.printException(() -> "setShortsPlayerBottomInset failure", ex);
        }
    }

    /**
     * Injection point.
     *
     * @param original The room the Shorts player reserves below itself for the navigation bar.
     */
    public static int getShortsPlayerBottomRoom(int original) {
        return SETS_SHORTS_PLAYER_BOTTOM_ROOM ? 0 : original;
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
        if (STATUS_BAR_TRANSLUCENCY != StatusBarTranslucency.DEFAULT) return false;
        return original;
    }

    /**
     * Injection point.
     *
     * @param original The color the app theme gives the system navigation buttons.
     */
    public static int getSystemNavigationBarColor(int original) {
        if (!DISABLE_TRANSLUCENT_NAVIGATION_BAR) return original;

        try {
            return OpaqueSystemBars.systemNavigationBarColor(original);
        } catch (Exception ex) {
            Logger.printException(() -> "getSystemNavigationBarColor failure", ex);
            return original;
        }
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
     * Injection point.
     * <p>
     * The toolbar create button of the Android Automotive layout, which swaps the create button with the
     * notifications button, opens a bottom sheet whose video button uses the upload endpoint, which the app
     * handles only in the creation modes. The upload endpoint is opened in the creation modes instead,
     * as the create button of the phone layout does.
     */
    public static void fixToolbarCreateButtonUpload(List<MessageLite> rawButtonList) {
        if (HIDE_TOOLBAR_CREATE_BUTTON || rawButtonList == null || rawButtonList.isEmpty()) return;

        try {
            for (int i = 0, size = rawButtonList.size(); i < size; i++) {
                MessageLite rawButtons = rawButtonList.get(i);
                Buttons buttons = Buttons.parseFrom(rawButtons.toByteArray());
                if (!buttons.hasButtonRenderer() || !buttons.getButtonRenderer().hasIcon()
                        || !StringUtils.equalsAny(buttons.getButtonRenderer().getIcon().getYtIconType().name(),
                        CREATE_BUTTON_ENUMS)) {
                    continue;
                }

                ButtonRenderer createButton = openUploadInCreationModes(buttons.getButtonRenderer());
                if (createButton == null) {
                    return;
                }
                ExtensionRegistryLite registry = getGeneratedRegistry();
                MessageLite fixedButtons = registry == null ? null : parseWithRegistry(rawButtons,
                        buttons.toBuilder().setButtonRenderer(createButton).build().toByteArray(), registry);
                if (fixedButtons != null) {
                    rawButtonList.set(i, fixedButtons);
                    Logger.printDebug(() -> "Toolbar create button opens the upload in the creation modes");
                }
                return;
            }
        } catch (Exception ex) {
            Logger.printException(() -> "fixToolbarCreateButtonUpload failure", ex);
        }
    }

    /**
     * Parses a message with the extension registry. The commands of the buttons are extensions,
     * which are lost if the message is parsed without the registry.
     *
     * @param message Message of the class to parse.
     * @return The parsed message, or null if the parse method was not found during patching.
     */
    @Nullable
    private static MessageLite parseWithRegistry(MessageLite message, byte[] bytes, ExtensionRegistryLite registry) {
        return null; // Modified during patching.
    }

    /**
     * @return The extension registry of the app, or null if not found during patching.
     */
    @Nullable
    private static ExtensionRegistryLite getGeneratedRegistry() {
        return null; // Modified during patching.
    }

    /**
     * @return The create button whose upload buttons open the upload endpoint in the creation modes,
     *         or null if the create button has no upload button.
     */
    @Nullable
    private static ButtonRenderer openUploadInCreationModes(ButtonRenderer createButton) {
        NavigationEndpoint endpoint = createButton.getNavigationEndpoint();
        if (!endpoint.hasCreationEntrySheetEndpoint()) {
            return null;
        }
        CreationEntrySheetEndpoint sheetEndpoint = endpoint.getCreationEntrySheetEndpoint();
        ButtonSheetRenderer sheet = sheetEndpoint.getButtonSheetSupportedRenderers().getButtonSheetRenderer();
        ButtonSheetRenderer.Builder sheetBuilder = sheet.toBuilder();
        boolean replaced = false;

        for (int i = 0, count = sheet.getButtonsCount(); i < count; i++) {
            Buttons button = sheet.getButtons(i);
            ButtonRenderer buttonRenderer = button.getButtonRenderer();
            NavigationEndpoint uploadEndpoint = buttonRenderer.getNavigationEndpoint();
            if (!button.hasButtonRenderer() || !uploadEndpoint.hasShortsCreationEndpoint()) {
                continue;
            }

            // The only mode button of the creation modes opens the upload endpoint.
            ButtonRenderer modeButton = buttonRenderer.toBuilder()
                    .clearNavigationEndpoint()
                    .setCommand(uploadEndpoint)
                    .build();
            CreationModesSwitcherRenderer switcher = CreationModesSwitcherRenderer.newBuilder()
                    .addModeButtons(Buttons.newBuilder().setButtonRenderer(modeButton))
                    .addModes(CreationMode.CREATION_MODE_UPLOADS)
                    .setDefaultMode(CreationMode.CREATION_MODE_UPLOADS)
                    .build();
            CreationModesCommand creationModesCommand = CreationModesCommand.newBuilder()
                    .setCreationModesControlsRenderer(CreationModesControlsRendererHolder.newBuilder()
                            .setCreationModesControlsRenderer(CreationModesControlsRenderer.newBuilder()
                                    .setCreationModesSwitcherRenderer(CreationModesSwitcherRendererHolder.newBuilder()
                                            .setCreationModesSwitcherRenderer(switcher))))
                    .build();

            NavigationEndpoint creationModesEndpoint = uploadEndpoint.toBuilder()
                    .clearShortsCreationEndpoint()
                    .setCreationModesCommand(creationModesCommand)
                    .build();
            sheetBuilder.setButtons(i, button.toBuilder().setButtonRenderer(
                    buttonRenderer.toBuilder().setNavigationEndpoint(creationModesEndpoint)));
            replaced = true;
        }
        if (!replaced) {
            return null;
        }

        CreationEntrySheetEndpoint creationModesSheetEndpoint = sheetEndpoint.toBuilder()
                .setButtonSheetSupportedRenderers(sheetEndpoint.getButtonSheetSupportedRenderers().toBuilder()
                        .setButtonSheetRenderer(sheetBuilder))
                .build();
        return createButton.toBuilder()
                .setNavigationEndpoint(endpoint.toBuilder().setCreationEntrySheetEndpoint(creationModesSheetEndpoint))
                .build();
    }

    /**
     * Injection point. Removes toolbar buttons hidden by the user before YouTube lays them out.
     */
    public static void modifyToolbarButtons(List<MessageLite> rawButtonList) {
        if (rawButtonList == null || rawButtonList.isEmpty()) return;

        try {
            for (int i = rawButtonList.size() - 1; i >= 0; i--) {
                MessageLite msg = rawButtonList.get(i);
                Buttons buttons = Buttons.parseFrom(msg.toByteArray());

                if (buttons.hasButtonRenderer() && buttons.getButtonRenderer().hasIcon()) {
                    String iconName = buttons.getButtonRenderer().getIcon().getYtIconType().name();

                    boolean isCreate = StringUtils.equalsAny(iconName, CREATE_BUTTON_ENUMS);
                    boolean isNotification = StringUtils.equalsAny(iconName, NOTIFICATION_BUTTON_ENUMS);
                    boolean isSearch = NavigationButton.SEARCH.ytEnumNames.contains(iconName);

                    if ((HIDE_TOOLBAR_CREATE_BUTTON && isCreate) ||
                            (HIDE_TOOLBAR_NOTIFICATION_BUTTON && isNotification) ||
                            (HIDE_TOOLBAR_SEARCH_BUTTON && isSearch)) {
                        rawButtonList.remove(i);
                    }
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to modify toolbar buttons", ex);
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

                    // Replace the accessibility label of the copied button.
                    ButtonRendererAccessibilityData accessibilityData = ButtonRendererAccessibilityData
                            .newBuilder()
                            .setLabel(ResourceUtils.getString("revanced_change_start_page_entry_settings"))
                            .build();
                    renderer.setButtonRendererAccessibilityData(accessibilityData);
                    renderer.setRendererAccessibilityData(RendererAccessibilityData.newBuilder()
                            .setButtonRendererAccessibilityData(accessibilityData)
                            .build());

                    renderer.clearIcon();
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

    // Downloads button of the offline library in the You tab.
    private static final boolean SHOW_TOOLBAR_DOWNLOADS_BUTTON =
            Settings.IN_APP_DOWNLOADER.get() && Settings.SHOW_OFFLINE_LIBRARY_BUTTON.get();

    private static final ThreadLocal<byte[]> pendingToolbarDownloadsButton = new ThreadLocal<>();

    /**
     * Injection point. Called for every button of a page toolbar, such as the one of the You tab,
     * before YouTube creates its menu item.
     * If it is YouTube's own Settings button, a copy with the download icon is kept for
     * {@link #takeToolbarDownloadsButton()}, which adds it after the Settings button.
     *
     * @param buttonRenderer The button renderer YouTube is about to show.
     */
    public static void prepareToolbarDownloadsButton(Object buttonRenderer) {
        pendingToolbarDownloadsButton.set(createToolbarDownloadsButton(buttonRenderer));
    }

    /**
     * Injection point. Called after YouTube added the menu item of {@link #prepareToolbarDownloadsButton(Object)}.
     *
     * @return The Downloads button to add, or null.
     */
    @Nullable
    public static byte[] takeToolbarDownloadsButton() {
        byte[] button = pendingToolbarDownloadsButton.get();
        pendingToolbarDownloadsButton.remove();
        return button;
    }

    @Nullable
    private static byte[] createToolbarDownloadsButton(Object buttonRenderer) {
        if (!SHOW_TOOLBAR_DOWNLOADS_BUTTON || !(buttonRenderer instanceof MessageLite message)) {
            return null;
        }

        try {
            ButtonRenderer settings = ButtonRenderer.parseFrom(message.toByteArray());
            if (!settings.hasIcon() || settings.getIcon().getYtIconType() != YTIconType.SETTINGS_CAIRO) {
                return null;
            }

            ButtonRendererAccessibilityData accessibilityData = ButtonRendererAccessibilityData
                    .newBuilder()
                    .setLabel(ResourceUtils.getString("revanced_offline_library_title"))
                    .build();
            // The native command of the copy is replaced by the click listener of setToolbarDownloadsOnClickListener.
            return settings.toBuilder()
                    .setButtonRendererAccessibilityData(accessibilityData)
                    .setRendererAccessibilityData(RendererAccessibilityData.newBuilder()
                            .setButtonRendererAccessibilityData(accessibilityData)
                            .build())
                    .setIcon(Icon.newBuilder().setYtIconType(YTIconType.OFFLINE_DOWNLOAD).build())
                    .build()
                    .toByteArray();
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to create toolbar Downloads button", ex);
            return null;
        }
    }

    /**
     * Injection point. Replaces the copied native listener after YouTube finishes binding the toolbar button.
     */
    public static void setToolbarDownloadsOnClickListener(String enumName, View toolbarView) {
        if (!SHOW_TOOLBAR_DOWNLOADS_BUTTON || !YTIconType.OFFLINE_DOWNLOAD.name().equals(enumName)
                || !(toolbarView instanceof ViewGroup viewGroup)) {
            return;
        }

        ImageView imageView = Utils.getChildView(viewGroup, view -> view instanceof ImageView);
        if (imageView == null) {
            return;
        }

        Utils.runOnMainThreadDelayed(() -> {
            imageView.setClickable(true);
            imageView.setOnClickListener(button -> OfflineScreens.openLibrary(button.getContext()));
            imageView.setOnLongClickListener(null);
        }, 100);
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

        // The hook also sees YouTube's native Settings button in the "You" tab. Keep its original
        // behavior when the toolbar action override is not meant to apply there.
        if (!SHOW_TOOLBAR_SETTINGS_BUTTON_TYPE_IN_YOU_TAB
                && NavigationButton.getSelectedNavigationButton() == NavigationButton.LIBRARY) {
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
