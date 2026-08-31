/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Portions of this file are modified by anddea:
 * Copyright (C) 2026 anddea
 * https://github.com/anddea/revanced-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.youtube.patches.utils;

import static app.morphe.extension.shared.utils.StringRef.str;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import app.morphe.extension.shared.ui.Dim;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceType;
import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.patches.SaveToWatchLaterPatch;
import app.morphe.extension.youtube.patches.general.DownloadActionsPatch;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.PlayerType;
import app.morphe.extension.youtube.utils.ThemeUtils;

@SuppressWarnings("unused")
public final class FlyoutUtils {

    public record FlyoutMenuInfo(
            LinearLayout menuContainer,
            int adjustedIndex,
            boolean isPopupWindow,
            @Nullable PopupWindow popupWindow
    ) {}

    private static final List<byte[]> KIDS_VIDEO_ELEMENTS_BYTES = List.of(
            getAsciiBytes("video_metadata_carousel.e"),
            getAsciiBytes("com.google.android.apps.youtube.kids"),
            getAsciiBytes("https://www.youtube.com/myfamily/#mf-compare")
    );

    private static final List<byte[]> SHORTS_VIDEO_ELEMENTS_BYTES = List.of(
            getAsciiBytes("shorts_pivot_item.e"),
            getAsciiBytes("shorts_shelf.e"),
            getAsciiBytes("shorts_video_cell.e")
    );

    private static final int SECONDARY_CONTAINER_ID =
            ResourceUtils.getIdentifier("list_item_secondary_container", ResourceType.ID);
    private static final int ITEM_TEXT_ID =
            ResourceUtils.getIdentifier("list_item_text", ResourceType.ID);
    private static final String saveToWatchLaterButtonName =
            str("morphe_save_to_watch_later_flyout_title");
    private static final Drawable saveToWatchLaterDrawable = getSaveToWatchLaterDrawable();

    private static WeakReference<TextView> customItemTextRef = new WeakReference<>(null);

    private static Dialog flyoutDialog;
    private static PopupWindow flyoutPopupWindow;
    private static boolean videoMarkedAsForKids;
    private static boolean videoMarkedAsShorts;

    private FlyoutUtils() {
    }

    private static byte[] getAsciiBytes(String string) {
        return string.getBytes(StandardCharsets.US_ASCII);
    }

    private static Drawable getSaveToWatchLaterDrawable() {
        Drawable drawable = ResourceUtils.getDrawable("yt_outline_experimental_clock_vd_theme_24");
        return drawable != null
                ? drawable
                : ResourceUtils.getDrawable("morphe_save_to_watch_later_button_bold");
    }

    public static void setVideoMarkedAsForKids(byte[] bytes) {
        List<Integer> kidsVideoElementsBytesIndexes = byteIndexesOf(bytes, KIDS_VIDEO_ELEMENTS_BYTES);
        if (!kidsVideoElementsBytesIndexes.isEmpty() &&
                kidsVideoElementsBytesIndexes.size() == KIDS_VIDEO_ELEMENTS_BYTES.size() - 1) {
            videoMarkedAsForKids = true;
        }
    }

    /**
     * Marks the current feed flyout payload as a Shorts item.
     *
     * @param bytes the serialized feed endpoint payload, or null when no payload is available
     */
    public static void setVideoMarkedAsShorts(@Nullable byte[] bytes) {
        videoMarkedAsShorts = !byteIndexesOf(bytes, SHORTS_VIDEO_ELEMENTS_BYTES).isEmpty();
    }

    /**
     * Injection point.
     */
    public static void setVideoMarkedAsForKids() {
        videoMarkedAsForKids = false;
    }

    /**
     * Injection point.
     */
    public static void setBottomSheetFlyout(@Nullable Dialog dialog) {
        try {
            if (dialog == null) {
                return;
            }
            flyoutDialog = dialog;
            runFlyoutPanelVisibilityHandler(dialog);

            Window window = dialog.getWindow();
            if (window == null) {
                Logger.printDebug(() -> "Cannot set flyout, window is null: " + dialog);
                return;
            }

            WeakReference<Dialog> dialogRef = new WeakReference<>(dialog);
            ViewTreeObserver viewTreeObserver = window.getDecorView().getViewTreeObserver();
            viewTreeObserver.addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        private boolean alreadyInjectedButton;

                        @Override
                        public void onGlobalLayout() {
                            try {
                                Dialog dialog = dialogRef.get();
                                if (dialog == null) {
                                    Logger.printDebug(() -> "Removing flyout listener");
                                    viewTreeObserver.removeOnGlobalLayoutListener(this);
                                    return;
                                }

                                if (dialog.isShowing()) {
                                    if (!alreadyInjectedButton) {
                                        addFlyoutElements(dialog);
                                        alreadyInjectedButton = true;
                                    }
                                    onFlyoutListBound(dialog);
                                } else {
                                    alreadyInjectedButton = false;
                                }
                            } catch (Exception ex) {
                                Logger.printException(() -> "setBottomSheetFlyout onGlobalLayout failure", ex);
                            }
                        }
                    }
            );
        } catch (Exception ex) {
            Logger.printException(() -> "setBottomSheetFlyout failure", ex);
        }
    }

    /**
     * Injection point.
     */
    public static void setPopupWindowFlyout(@Nullable PopupWindow popupWindow) {
        try {
            if (popupWindow == null) {
                return;
            }
            flyoutPopupWindow = popupWindow;
            runFlyoutPanelVisibilityHandler(popupWindow);

            addFlyoutElements(popupWindow);
            onFlyoutListBound(popupWindow);
        } catch (Exception ex) {
            Logger.printException(() -> "setPopupWindowFlyout failure", ex);
        }
    }

    public static void dismissFlyout() {
        if (flyoutDialog != null) {
            flyoutDialog.dismiss();
            flyoutDialog = null;
        }

        if (flyoutPopupWindow != null) {
            flyoutPopupWindow.dismiss();
            flyoutPopupWindow = null;
        }
    }

    private static void addFlyoutElements(Object flyoutPanel) {
        int nextButtonIndex = 0;

        boolean showKidsSaveToWatchLater = Settings.KIDS_SAVE_TO_WATCH_LATER_BUTTON.get()
                && PlayerType.getCurrent().isMaximizedOrFullscreen()
                && videoMarkedAsForKids;
        boolean showShortsSaveToWatchLater = Settings.SHORTS_SAVE_TO_WATCH_LATER_BUTTON.get()
                && videoMarkedAsShorts;

        if (showKidsSaveToWatchLater || showShortsSaveToWatchLater) {
            nextButtonIndex = addFlyoutButton(
                    flyoutPanel,
                    saveToWatchLaterDrawable,
                    saveToWatchLaterButtonName,
                    v -> {
                        SaveToWatchLaterPatch.saveVideo(DownloadActionsPatch.getFlyoutVideoId());

                        dismissFlyout(); // Must dismiss after showing dialog.
                    },
                    nextButtonIndex
            );
        }

        if (nextButtonIndex > 0) {
            addDivider(flyoutPanel, nextButtonIndex);
        }
    }

    /**
     * Applies the changes that are only possible once the menu list has bound its items.
     * Idempotent, so it can run on every layout pass and reapply itself after the app
     * binds the list again.
     */
    private static void onFlyoutListBound(Object flyoutPanel) {
        try {
            FlyoutMenuInfo menuInfo = getFlyoutMenuInfo(flyoutPanel, 0);
            if (menuInfo == null) {
                return;
            }

            // The items are inside the list, which is the last view of the menu container.
            LinearLayout menuContainer = menuInfo.menuContainer();
            View lastChild = menuContainer.getChildAt(menuContainer.getChildCount() - 1);
            if (!(lastChild instanceof ViewGroup itemList) || itemList.getChildCount() == 0) {
                return;
            }

            copyListItemTypeface(itemList);
        } catch (Exception ex) {
            Logger.printException(() -> "onFlyoutListBound failure", ex);
        }
    }

    /**
     * The app applies its own font weight to the menu items after they are bound,
     * so the custom item only matches them by taking the typeface of a bound item.
     */
    private static void copyListItemTypeface(ViewGroup itemList) {
        TextView customItemText = customItemTextRef.get();
        if (customItemText == null || ITEM_TEXT_ID == 0) {
            return;
        }

        if (itemList.getChildAt(0).findViewById(ITEM_TEXT_ID) instanceof TextView itemText) {
            // setTypeface always requests a layout, so only call it when the font really differs.
            Typeface itemTypeface = itemText.getTypeface();
            if (customItemText.getTypeface() != itemTypeface) {
                customItemText.setTypeface(itemTypeface);
            }
        }
    }

    /**
     * @return The height of the bottom sheet drag handle, or zero if the menu has no handle.
     * The handle is drawn over the top of the menu instead of being laid out in it,
     * so the first item has to be pushed down by its height.
     */
    private static int getDragHandleHeight(ViewGroup menuContainer) {
        for (int i = 0, count = menuContainer.getChildCount(); i < count; i++) {
            if (menuContainer.getChildAt(i) instanceof ImageView handle) {
                return handle.getHeight();
            }
        }

        return 0;
    }

    @SuppressWarnings("SameParameterValue")
    private static int addFlyoutButton(
            Object flyoutPanel,
            Drawable icon,
            String text,
            View.OnClickListener clickListener,
            int index
    ) {
        return addFlyoutMenuItem(flyoutPanel, icon, text, clickListener, index, false);
    }

    @SuppressWarnings("UnusedReturnValue")
    private static int addDivider(Object flyoutPanel, int index) {
        return addFlyoutMenuItem(flyoutPanel, null, null, null, index, true);
    }

    private static int addFlyoutMenuItem(
            Object flyoutPanel,
            @Nullable Drawable icon,
            @Nullable String text,
            @Nullable View.OnClickListener clickListener,
            int index,
            boolean isDivider
    ) {
        try {
            FlyoutMenuInfo menuInfo = getFlyoutMenuInfo(flyoutPanel, index);
            if (menuInfo == null) {
                return -1;
            }

            Context context = Utils.getActivity();
            if (context == null) {
                return -1;
            }

            View view = isDivider
                    ? createFlyoutDivider(context)
                    : addFlyoutButton(context, menuInfo.menuContainer(), icon, text, clickListener);

            int fixedIndex = menuInfo.adjustedIndex();
            menuInfo.menuContainer().addView(view, fixedIndex);

            PopupWindow popupWindow = menuInfo.popupWindow();
            if (popupWindow != null) {
                popupWindow.update();
            }

            // For new layout only:
            // Skip an index to inject the next element after the current button.
            if (menuInfo.isPopupWindow()) {
                fixedIndex++;
            }

            return fixedIndex;
        } catch (Exception ex) {
            Logger.printException(() -> "addFlyoutMenuItem failure", ex);
        }

        return -1;
    }

    private static void runFlyoutPanelVisibilityHandler(Object flyoutObject) {
        if (flyoutObject == null) {
            return;
        }

        final Handler visibilityHandler = new Handler(Looper.getMainLooper());
        visibilityHandler.post(new Runnable() {
            @Override
            public void run() {
                final boolean isShowing;

                if (flyoutObject instanceof Dialog flyoutDialogHandler) {
                    isShowing = flyoutDialogHandler.isShowing();
                } else if (flyoutObject instanceof PopupWindow flyoutPopupWindowHandler) {
                    isShowing = flyoutPopupWindowHandler.isShowing();
                } else {
                    isShowing = false;
                }

                if (isShowing) {
                    visibilityHandler.postDelayed(this, 100);
                } else {
                    Utils.runOnMainThreadDelayed(
                            DownloadActionsPatch::clearFlyoutVideoId,
                            500
                    );
                }
            }
        });
    }

    @Nullable
    private static FlyoutMenuInfo getFlyoutMenuInfo(Object flyoutPanel, int initialIndex) {
        LinearLayout menuContainer = null;
        PopupWindow popupWindow = null;
        boolean isPopupWindow = false;
        int adjustedIndex = initialIndex;

        if (flyoutPanel instanceof PopupWindow checkedPopupWindow) {
            popupWindow = checkedPopupWindow;
            if (checkedPopupWindow.getContentView() instanceof FrameLayout frameLayout) {
                if (frameLayout.getChildAt(0) instanceof ViewGroup viewGroup
                        && viewGroup.getChildAt(0) instanceof LinearLayout checkedMenuContainer) {
                    menuContainer = checkedMenuContainer;
                }
            }
            isPopupWindow = true;
        } else if (flyoutPanel instanceof Dialog checkedDialog) {
            Window window = checkedDialog.getWindow();
            if (window != null) {
                View decorView = window.getDecorView();
                final int containerId = ResourceUtils.getIdentifier("container", ResourceType.ID);
                if (containerId != 0) {
                    View container = decorView.findViewById(containerId);
                    if (container instanceof FrameLayout frameLayout) {
                        if (frameLayout.getChildAt(0) instanceof ViewGroup coordinator
                                && coordinator.getChildAt(1) instanceof ViewGroup nestedFrame) {
                            View menuRoot = nestedFrame.getChildAt(0);
                            if (menuRoot instanceof ViewGroup group
                                    && group.getChildAt(0) instanceof LinearLayout linearLayout) {
                                menuContainer = linearLayout;
                                // Skip an index to inject the button after the bottom sheet handle.
                                adjustedIndex += 1;
                            }
                        }
                    }
                }
            }
        }

        if (menuContainer == null) {
            return null;
        }

        return new FlyoutMenuInfo(menuContainer, adjustedIndex, isPopupWindow, popupWindow);
    }

    @SuppressLint("ResourceType")
    private static View addFlyoutButton(
            Context context,
            ViewGroup parent,
            @Nullable Drawable icon,
            String text,
            View.OnClickListener clickListener
    ) {
        // Inflating the same layout the app uses for its own items keeps the row height,
        // paddings, font and icon size identical to them.
        // 20.21 has no modern layout and uses the older one for its own items.
        int layoutId = ResourceUtils.getIdentifier(
                "modern_bottom_sheet_enableable_list_item", ResourceType.LAYOUT);
        if (layoutId == 0) {
            layoutId = ResourceUtils.getIdentifier(
                    "bottom_sheet_enableable_list_item", ResourceType.LAYOUT);
        }

        View customButton = LayoutInflater.from(context).inflate(layoutId, parent, false);

        TextView textView = customButton.findViewById(ITEM_TEXT_ID);
        if (textView != null) {
            textView.setText(text);
            customItemTextRef = new WeakReference<>(textView);
        }

        ImageView iconView = customButton.findViewById(
                ResourceUtils.getIdentifier("list_item_icon_primary", ResourceType.ID));
        if (iconView != null && icon != null) {
            iconView.setImageDrawable(icon);
            // The layout tints the icon with ytIconInactive, but the menu items themselves
            // are drawn with the text color.
            iconView.setImageTintList(ColorStateList.valueOf(textView != null
                    ? textView.getCurrentTextColor()
                    : ThemeUtils.getAppForegroundColor()));
        }

        if (customButton.getLayoutParams() instanceof ViewGroup.MarginLayoutParams marginParams) {
            marginParams.topMargin = getDragHandleHeight(parent);
        }

        // The layout reserves space for a secondary icon this item does not have.
        View secondaryContainer = customButton.findViewById(SECONDARY_CONTAINER_ID);
        if (secondaryContainer != null) {
            secondaryContainer.setVisibility(View.GONE);
        }

        int[] attrs = {android.R.attr.selectableItemBackground};
        try (TypedArray typedArray = context.obtainStyledAttributes(attrs)) {
            customButton.setForeground(typedArray.getDrawable(0));
        }

        customButton.setOnClickListener(clickListener);

        return customButton;
    }

    public static View createFlyoutDivider(Context context) {
        int height = ResourceUtils.getDimension("line_separator_height");
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height > 0 ? height : Dim.dp(1)
        );

        // A plain View measures to the full available width and stretches the whole menu
        // when the menu is not measured with a fixed width. An empty ViewGroup measures to zero.
        LinearLayout divider = new LinearLayout(context);
        divider.setLayoutParams(dividerParams);
        // Same 20% of the foreground the app draws its own separators with.
        divider.setBackgroundColor((ThemeUtils.getAppForegroundColor() & 0xFFFFFF) | 0x33000000);

        return divider;
    }

    @SuppressWarnings("SameParameterValue")
    private static List<Integer> byteIndexesOf(byte[] haystack, List<byte[]> needles) {
        List<Integer> indices = new ArrayList<>();
        if (haystack == null || needles == null) {
            return indices;
        }

        final int haystackLen = haystack.length;
        final boolean[] found = new boolean[needles.size()];
        for (int i = 0; i < haystackLen; i++) {
            for (int k = 0; k < needles.size(); k++) {
                byte[] needle = needles.get(k);
                if (found[k] || needle == null) {
                    continue;
                }

                final int needleLen = needle.length;
                if (needleLen == 0 || i + needleLen > haystackLen) {
                    continue;
                }

                boolean match = true;
                for (int j = 0; j < needleLen; j++) {
                    if (haystack[i + j] != needle[j]) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    indices.add(i);
                    found[k] = true;
                }
            }
        }
        return indices;
    }
}
