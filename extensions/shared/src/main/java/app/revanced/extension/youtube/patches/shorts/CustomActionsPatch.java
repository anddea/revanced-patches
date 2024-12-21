package app.revanced.extension.youtube.patches.shorts;

import static app.revanced.extension.shared.utils.ResourceUtils.getString;
import static app.revanced.extension.shared.utils.Utils.dpToPx;
import static app.revanced.extension.youtube.patches.components.ShortsCustomActionsFilter.isShortsFlyoutMenuVisible;
import static app.revanced.extension.youtube.utils.ExtendedUtils.isSpoofingToLessThan;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import app.revanced.extension.shared.settings.BooleanSetting;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.patches.components.ShortsCustomActionsFilter;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.ShortsPlayerState;
import app.revanced.extension.youtube.utils.ThemeUtils;
import app.revanced.extension.youtube.utils.VideoUtils;

@SuppressWarnings("unused")
public final class CustomActionsPatch {
    private static final boolean IS_SPOOFING_TO_YOUTUBE_2023 =
            isSpoofingToLessThan("19.00.00");
    private static final boolean SHORTS_CUSTOM_ACTIONS_FLYOUT_MENU_ENABLED =
            !IS_SPOOFING_TO_YOUTUBE_2023 && Settings.ENABLE_SHORTS_CUSTOM_ACTIONS_FLYOUT_MENU.get();
    private static final boolean SHORTS_CUSTOM_ACTIONS_TOOLBAR_ENABLED =
            Settings.ENABLE_SHORTS_CUSTOM_ACTIONS_TOOLBAR.get();

    private static final int arrSize = CustomAction.values().length;
    private static final Map<CustomAction, Object> flyoutMenuMap = new LinkedHashMap<>(arrSize);
    private static WeakReference<Context> contextRef = new WeakReference<>(null);
    private static WeakReference<RecyclerView> recyclerViewRef = new WeakReference<>(null);

    /**
     * Injection point.
     */
    public static void setToolbarMenu(String enumString, View toolbarView) {
        if (!SHORTS_CUSTOM_ACTIONS_TOOLBAR_ENABLED) {
            return;
        }
        if (ShortsPlayerState.getCurrent().isClosed()) {
            return;
        }
        if (!isMoreButton(enumString)) {
            return;
        }
        setToolbarMenuOnLongClickListener((ViewGroup) toolbarView);
    }

    private static void setToolbarMenuOnLongClickListener(ViewGroup parentView) {
        ImageView imageView = Utils.getChildView(parentView, v -> v instanceof ImageView);
        if (imageView == null) {
            return;
        }
        Context context = imageView.getContext();
        contextRef = new WeakReference<>(context);

        // Overriding is possible only after OnClickListener is assigned to the more button.
        Utils.runOnMainThreadDelayed(() -> imageView.setOnLongClickListener(button -> {
            showMoreButtonDialog(context);
            return true;
        }), 0);
    }

    private static void showMoreButtonDialog(Context context) {
        ScrollView scrollView = new ScrollView(context);
        LinearLayout container = new LinearLayout(context);

        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, 0, 0, 0);

        Map<LinearLayout, Runnable> toolbarMap = new LinkedHashMap<>(arrSize);

        for (CustomAction customAction : CustomAction.values()) {
            if (customAction.settings.get()) {
                String title = customAction.getLabel();
                int iconId = customAction.getDrawableId();
                Runnable action = customAction.getOnClickAction();
                LinearLayout itemLayout = createItemLayout(context, title, iconId);
                toolbarMap.putIfAbsent(itemLayout, action);
                container.addView(itemLayout);
            }
        }

        scrollView.addView(container);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(scrollView);

        AlertDialog dialog = builder.create();
        dialog.show();

        toolbarMap.forEach((view, action) ->
                view.setOnClickListener(v -> {
                    action.run();
                    dialog.dismiss();
                })
        );
        toolbarMap.clear();

        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }

        // round corners
        GradientDrawable dialogBackground = new GradientDrawable();
        dialogBackground.setCornerRadius(32);
        window.setBackgroundDrawable(dialogBackground);

        // fit screen width
        int dialogWidth = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.95);
        window.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);

        // move dialog to bottom
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.gravity = Gravity.BOTTOM;

        // adjust the vertical offset
        layoutParams.y = dpToPx(5);

        window.setAttributes(layoutParams);
    }

    private static LinearLayout createItemLayout(Context context, String title, int iconId) {
        // Item Layout
        LinearLayout itemLayout = new LinearLayout(context);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        itemLayout.setGravity(Gravity.CENTER_VERTICAL);
        itemLayout.setClickable(true);
        itemLayout.setFocusable(true);

        // Create a StateListDrawable for the background
        StateListDrawable background = new StateListDrawable();
        ColorDrawable pressedDrawable = new ColorDrawable(ThemeUtils.getPressedElementColor());
        ColorDrawable defaultDrawable = new ColorDrawable(ThemeUtils.getBackgroundColor());
        background.addState(new int[]{android.R.attr.state_pressed}, pressedDrawable);
        background.addState(new int[]{}, defaultDrawable);
        itemLayout.setBackground(background);

        // Icon
        ColorFilter cf = new PorterDuffColorFilter(ThemeUtils.getForegroundColor(), PorterDuff.Mode.SRC_ATOP);
        ImageView iconView = new ImageView(context);
        iconView.setImageResource(iconId);
        iconView.setColorFilter(cf);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24));
        iconParams.setMarginEnd(dpToPx(16));
        iconView.setLayoutParams(iconParams);
        itemLayout.addView(iconView);

        // Text container
        LinearLayout textContainer = new LinearLayout(context);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(16);
        titleView.setTextColor(ThemeUtils.getForegroundColor());
        textContainer.addView(titleView);

        itemLayout.addView(textContainer);

        return itemLayout;
    }

    private static boolean isMoreButton(String enumString) {
        return StringUtils.equalsAny(
                enumString,
                "MORE_VERT",
                "MORE_VERT_BOLD"
        );
    }

    /**
     * Injection point.
     */
    public static void setFlyoutMenuObject(Object bottomSheetMenuObject) {
        if (!SHORTS_CUSTOM_ACTIONS_FLYOUT_MENU_ENABLED) {
            return;
        }
        if (ShortsPlayerState.getCurrent().isClosed()) {
            return;
        }
        if (bottomSheetMenuObject == null) {
            return;
        }
        for (CustomAction customAction : CustomAction.values()) {
            flyoutMenuMap.putIfAbsent(customAction, bottomSheetMenuObject);
        }
    }

    /**
     * Injection point.
     */
    public static void addFlyoutMenu(Object bottomSheetMenuClass, Object bottomSheetMenuList) {
        if (!SHORTS_CUSTOM_ACTIONS_FLYOUT_MENU_ENABLED) {
            return;
        }
        if (ShortsPlayerState.getCurrent().isClosed()) {
            return;
        }
        for (CustomAction customAction : CustomAction.values()) {
            if (customAction.settings.get()) {
                addFlyoutMenu(bottomSheetMenuClass, bottomSheetMenuList, customAction);
            }
        }
    }

    /**
     * Rest of the implementation added by patch.
     */
    private static void addFlyoutMenu(Object bottomSheetMenuClass, Object bottomSheetMenuList, CustomAction customAction) {
        Object bottomSheetMenuObject = flyoutMenuMap.get(customAction);
        // These instructions are ignored by patch.
        Logger.printInfo(() -> customAction.name() + bottomSheetMenuClass + bottomSheetMenuList + bottomSheetMenuObject);
    }

    /**
     * Injection point.
     */
    public static void onFlyoutMenuCreate(final RecyclerView recyclerView) {
        if (!SHORTS_CUSTOM_ACTIONS_FLYOUT_MENU_ENABLED) {
            return;
        }
        recyclerView.getViewTreeObserver().addOnDrawListener(() -> {
            try {
                if (ShortsPlayerState.getCurrent().isClosed()) {
                    return;
                }
                contextRef = new WeakReference<>(recyclerView.getContext());
                if (!isShortsFlyoutMenuVisible) {
                    return;
                }
                int childCount = recyclerView.getChildCount();
                if (childCount < arrSize + 1) {
                    return;
                }
                for (int i = 0; i < arrSize; i++) {
                    if (recyclerView.getChildAt(childCount - i - 1) instanceof ViewGroup parentViewGroup) {
                        childCount = recyclerView.getChildCount();
                        if (childCount > 3 && parentViewGroup.getChildAt(1) instanceof TextView textView) {
                            for (CustomAction customAction : CustomAction.values()) {
                                if (customAction.getLabel().equals(textView.getText().toString())) {
                                    View.OnClickListener onClick = customAction.getOnClickListener();
                                    View.OnLongClickListener onLongClick = customAction.getOnLongClickListener();
                                    recyclerViewRef = new WeakReference<>(recyclerView);
                                    parentViewGroup.setOnClickListener(onClick);
                                    if (onLongClick != null) {
                                        parentViewGroup.setOnLongClickListener(onLongClick);
                                    }
                                }
                            }
                        }
                    }
                }
                isShortsFlyoutMenuVisible = false;
            } catch (Exception ex) {
                Logger.printException(() -> "onFlyoutMenuCreate failure", ex);
            }
        });
    }

    /**
     * Injection point.
     */
    public static void onLiveHeaderElementsContainerCreate(final View view) {
        if (!SHORTS_CUSTOM_ACTIONS_TOOLBAR_ENABLED) {
            return;
        }
        view.getViewTreeObserver().addOnDrawListener(() -> {
            try {
                if (view instanceof ViewGroup viewGroup) {
                    setToolbarMenuOnLongClickListener(viewGroup);
                }
            } catch (Exception ex) {
                Logger.printException(() -> "onFlyoutMenuCreate failure", ex);
            }
        });
    }

    private static void hideFlyoutMenu() {
        if (!SHORTS_CUSTOM_ACTIONS_FLYOUT_MENU_ENABLED) {
            return;
        }
        RecyclerView recyclerView = recyclerViewRef.get();
        if (recyclerView == null) {
            return;
        }

        if (!(Utils.getParentView(recyclerView, 3) instanceof ViewGroup parentView3rd)) {
            return;
        }

        if (!(parentView3rd.getParent() instanceof ViewGroup parentView4th)) {
            return;
        }

        // Dismiss View [R.id.touch_outside] is the 1st ChildView of the 4th ParentView.
        // This only shows in phone layout.
        Utils.clickView(parentView4th.getChildAt(0));

        // In tablet layout there is no Dismiss View, instead we just hide all two parent views.
        parentView3rd.setVisibility(View.GONE);
        parentView4th.setVisibility(View.GONE);
    }

    public enum CustomAction {
        COPY_URL(
                Settings.SHORTS_CUSTOM_ACTIONS_COPY_VIDEO_URL,
                "yt_outline_link_black_24",
                () -> VideoUtils.copyUrl(
                        VideoUtils.getVideoUrl(
                                ShortsCustomActionsFilter.getShortsVideoId(),
                                false
                        ),
                        false
                ),
                () -> VideoUtils.copyUrl(
                        VideoUtils.getVideoUrl(
                                ShortsCustomActionsFilter.getShortsVideoId(),
                                true
                        ),
                        true
                )
        ),
        COPY_URL_WITH_TIMESTAMP(
                Settings.SHORTS_CUSTOM_ACTIONS_COPY_VIDEO_URL_TIMESTAMP,
                "yt_outline_arrow_time_black_24",
                () -> VideoUtils.copyUrl(
                        VideoUtils.getVideoUrl(
                                ShortsCustomActionsFilter.getShortsVideoId(),
                                true
                        ),
                        true
                ),
                () -> VideoUtils.copyUrl(
                        VideoUtils.getVideoUrl(
                                ShortsCustomActionsFilter.getShortsVideoId(),
                                false
                        ),
                        false
                )
        ),
        EXTERNAL_DOWNLOADER(
                Settings.SHORTS_CUSTOM_ACTIONS_EXTERNAL_DOWNLOADER,
                "yt_outline_download_black_24",
                () -> VideoUtils.launchVideoExternalDownloader(
                        ShortsCustomActionsFilter.getShortsVideoId()
                )
        ),
        OPEN_VIDEO(
                Settings.SHORTS_CUSTOM_ACTIONS_OPEN_VIDEO,
                "yt_outline_youtube_logo_icon_black_24",
                () -> VideoUtils.openVideo(
                        ShortsCustomActionsFilter.getShortsVideoId(),
                        true
                )
        ),
        REPEAT_STATE(
                Settings.SHORTS_CUSTOM_ACTIONS_REPEAT_STATE,
                "yt_outline_arrow_repeat_1_black_24",
                () -> VideoUtils.showShortsRepeatDialog(contextRef.get())
        );

        @NonNull
        private final BooleanSetting settings;

        @NonNull
        private final Drawable drawable;

        private final int drawableId;

        @NonNull
        private final String label;

        @NonNull
        private final Runnable onClickAction;

        @Nullable
        private final Runnable onLongClickAction;

        CustomAction(@NonNull BooleanSetting settings,
                     @NonNull String icon,
                     @NonNull Runnable onClickAction
        ) {
            this(settings, icon, onClickAction, null);
        }

        CustomAction(@NonNull BooleanSetting settings,
                     @NonNull String icon,
                     @NonNull Runnable onClickAction,
                     @Nullable Runnable onLongClickAction
        ) {
            this.drawable = Objects.requireNonNull(ResourceUtils.getDrawable(icon));
            this.drawableId = ResourceUtils.getDrawableIdentifier(icon);
            this.label = getString(settings.key + "_label");
            this.settings = settings;
            this.onClickAction = onClickAction;
            this.onLongClickAction = onLongClickAction;
        }

        @NonNull
        public Drawable getDrawable() {
            return drawable;
        }

        public int getDrawableId() {
            return drawableId;
        }

        @NonNull
        public String getLabel() {
            return label;
        }

        @NonNull
        public Runnable getOnClickAction() {
            return onClickAction;
        }

        @NonNull
        public View.OnClickListener getOnClickListener() {
            return v -> {
                hideFlyoutMenu();
                onClickAction.run();
            };
        }

        @Nullable
        public View.OnLongClickListener getOnLongClickListener() {
            if (onLongClickAction == null) {
                return null;
            } else {
                return v -> {
                    hideFlyoutMenu();
                    onLongClickAction.run();
                    return true;
                };
            }
        }
    }

}