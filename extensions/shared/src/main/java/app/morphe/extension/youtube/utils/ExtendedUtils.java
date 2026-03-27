package app.morphe.extension.youtube.utils;

import static app.morphe.extension.shared.utils.ResourceUtils.getAnimation;
import static app.morphe.extension.shared.utils.ResourceUtils.getInteger;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.PackageUtils;
import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.PlayerType;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class ExtendedUtils extends PackageUtils {

    @SuppressWarnings("unused")
    public static final boolean IS_19_17_OR_GREATER = isVersionOrGreater("19.17.00");
    public static final boolean IS_19_20_OR_GREATER = isVersionOrGreater("19.20.00");
    public static final boolean IS_19_21_OR_GREATER = isVersionOrGreater("19.21.00");
    public static final boolean IS_19_26_OR_GREATER = isVersionOrGreater("19.26.00");
    public static final boolean IS_19_28_OR_GREATER = isVersionOrGreater("19.28.00");
    public static final boolean IS_19_29_OR_GREATER = isVersionOrGreater("19.29.00");
    public static final boolean IS_19_34_OR_GREATER = isVersionOrGreater("19.34.00");
    public static final boolean IS_20_09_OR_GREATER = isVersionOrGreater("20.09.00");
    public static final boolean IS_20_10_OR_GREATER = isVersionOrGreater("20.10.00");
    public static final boolean IS_20_22_OR_GREATER = isVersionOrGreater("20.22.00");

    public static final boolean IS_ARC = hasSystemFeature("org.chromium.arc");
    public static final boolean IS_AUTOMOTIVE = hasSystemFeature("android.hardware.type.automotive");
    public static final boolean IS_WATCH = hasSystemFeature("android.hardware.type.watch");

    private static String osName = "";

    public static String getOSName() {
        if (osName.isEmpty()) {
            if (IS_WATCH) {
                osName = "Android Wear";
            } else if (IS_AUTOMOTIVE) {
                osName = "Android Automotive";
            } else if (IS_ARC) {
                osName = "ChromeOS";
            } else {
                osName = "Android";
            }
        }

        return osName;
    }

    public static boolean isFullscreenHidden() {
        return Settings.DISABLE_ENGAGEMENT_PANEL.get() || Settings.HIDE_QUICK_ACTIONS.get();
    }

    public static boolean isSpoofingToLessThan(@NonNull String versionName) {
        if (!Settings.SPOOF_APP_VERSION.get()) return false;
        return isVersionToLessThan(Settings.SPOOF_APP_VERSION_TARGET.get(), versionName);
    }

    public static void setCommentPreviewSettings() {
        final boolean enabled = Settings.HIDE_PREVIEW_COMMENT.get();
        final boolean newMethod = Settings.HIDE_PREVIEW_COMMENT_TYPE.get();

        Settings.HIDE_PREVIEW_COMMENT_OLD_METHOD.save(enabled && !newMethod);
        Settings.HIDE_PREVIEW_COMMENT_NEW_METHOD.save(enabled && newMethod);
    }

    private static final Setting<?>[] additionalSettings = {
            Settings.HIDE_PLAYER_FLYOUT_MENU_AMBIENT,
            Settings.HIDE_PLAYER_FLYOUT_MENU_HELP,
            Settings.HIDE_PLAYER_FLYOUT_MENU_LOOP,
            Settings.HIDE_PLAYER_FLYOUT_MENU_PIP,
            Settings.HIDE_PLAYER_FLYOUT_MENU_PREMIUM_CONTROLS,
            Settings.HIDE_PLAYER_FLYOUT_MENU_STABLE_VOLUME,
            Settings.HIDE_PLAYER_FLYOUT_MENU_STATS_FOR_NERDS,
            Settings.HIDE_PLAYER_FLYOUT_MENU_WATCH_IN_VR,
            Settings.HIDE_PLAYER_FLYOUT_MENU_YT_MUSIC,
            Settings.SPOOF_APP_VERSION,
            Settings.SPOOF_APP_VERSION_TARGET
    };

    public static boolean anyMatchSetting(Setting<?> setting) {
        for (Setting<?> s : additionalSettings) {
            if (setting == s) return true;
        }
        return false;
    }

    public static void setPlayerFlyoutMenuAdditionalSettings() {
        Settings.HIDE_PLAYER_FLYOUT_MENU_ADDITIONAL_SETTINGS.save(isAdditionalSettingsEnabled());
    }

    private static boolean isAdditionalSettingsEnabled() {
        // In the old player flyout panels, the video quality icon and additional quality icon are the same
        // Therefore, additional Settings should not be blocked in old player flyout panels
        if (isSpoofingToLessThan("18.22.00"))
            return false;

        boolean additionalSettingsEnabled = true;
        final BooleanSetting[] additionalSettings = {
                Settings.HIDE_PLAYER_FLYOUT_MENU_AMBIENT,
                Settings.HIDE_PLAYER_FLYOUT_MENU_HELP,
                Settings.HIDE_PLAYER_FLYOUT_MENU_LOOP,
                Settings.HIDE_PLAYER_FLYOUT_MENU_PIP,
                Settings.HIDE_PLAYER_FLYOUT_MENU_PREMIUM_CONTROLS,
                Settings.HIDE_PLAYER_FLYOUT_MENU_STABLE_VOLUME,
                Settings.HIDE_PLAYER_FLYOUT_MENU_STATS_FOR_NERDS,
                Settings.HIDE_PLAYER_FLYOUT_MENU_WATCH_IN_VR,
                Settings.HIDE_PLAYER_FLYOUT_MENU_YT_MUSIC,
        };
        for (BooleanSetting s : additionalSettings) {
            additionalSettingsEnabled &= s.get();
        }
        return additionalSettingsEnabled;
    }

    public static LinearLayout prepareMainLayout(Context mContext) {
        return prepareMainLayout(mContext, false);
    }

    public static LinearLayout prepareMainLayout(Context mContext, boolean wideBottomMargins) {
        // Create main vertical LinearLayout for dialog content.
        LinearLayout mainLayout = new LinearLayout(mContext);
        mainLayout.setOrientation(LinearLayout.VERTICAL);

        // Preset size constants.
        final int dip4 = dipToPixels(4);   // Height for handle bar.
        final int dip5 = dipToPixels(5);
        final int dip8 = dipToPixels(8);   // Padding for mainLayout from left and right.
        final int dip20 = dipToPixels(20);
        final int dip40 = dipToPixels(40); // Width for handle bar.

        mainLayout.setPadding(dip5, dip8, dip5, dip8);

        // Set rounded rectangle background for the main layout.
        RoundRectShape roundRectShape = new RoundRectShape(
                Utils.createCornerRadii(12), null, null);
        ShapeDrawable background = new ShapeDrawable(roundRectShape);
        background.getPaint().setColor(ThemeUtils.getDialogBackgroundColor());
        mainLayout.setBackground(background);

        // Add handle bar at the top.
        View handleBar = new View(mContext);
        ShapeDrawable handleBackground = new ShapeDrawable(new RoundRectShape(
                Utils.createCornerRadii(4), null, null));
        handleBackground.getPaint().setColor(ThemeUtils.getAdjustedBackgroundColor(true));
        handleBar.setBackground(handleBackground);
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(
                dip40, // handle bar width.
                dip4   // handle bar height.
        );
        handleParams.gravity = Gravity.CENTER_HORIZONTAL; // Center horizontally.
        handleParams.setMargins(0, 0, 0, wideBottomMargins ? dip20 : dip8);
        handleBar.setLayoutParams(handleParams);
        // Add handle bar view to main layout.
        mainLayout.addView(handleBar);

        return mainLayout;
    }

    public static void showBottomSheetDialog(Context mContext, LinearLayout mainLayout) {
        showBottomSheetDialog(mContext, mainLayout, null);
    }

    public static void showBottomSheetDialog(Context mContext, LinearLayout mainLayout,
                                             @Nullable Map<LinearLayout, Runnable> actionsMap) {
        // Create a dialog without a theme for custom appearance.
        Dialog dialog = new Dialog(mContext);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // Remove default title bar.

        // Enable dismissing the dialog when tapping outside.
        dialog.setCanceledOnTouchOutside(true);

        final int dip6 = dipToPixels(6);   // Padding for mainLayout from bottom.
        final int dip8 = dipToPixels(8);   // Padding for mainLayout from left and right.

        // Wrap mainLayout in another LinearLayout for side margins.
        LinearLayout wrapperLayout = new LinearLayout(mContext);
        wrapperLayout.setOrientation(LinearLayout.VERTICAL);
        wrapperLayout.setPadding(dip8, 0, dip8, 0); // 8dp side margins.
        wrapperLayout.addView(mainLayout);

        ScrollView scrollView = new ScrollView(mContext);
        scrollView.addView(wrapperLayout);

        dialog.setContentView(scrollView);

        // Configure dialog window to appear at the bottom.
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.BOTTOM; // Position at bottom of screen.
            params.y = dip6; // 6dp margin from bottom.
            // In landscape, use the smaller dimension (height) as portrait width.
            int portraitWidth = mContext.getResources().getDisplayMetrics().widthPixels;
            if (isLandscapeOrientation()) {
                portraitWidth = Math.min(
                        portraitWidth,
                        mContext.getResources().getDisplayMetrics().heightPixels);
            }
            params.width = portraitWidth; // Use portrait width.
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
            window.setBackgroundDrawable(null); // Remove default dialog background.
        }

        // Apply slide-in animation when showing the dialog.
        final int fadeDurationFast = getInteger("fade_duration_fast");
        Animation slideInABottomAnimation = getAnimation("slide_in_bottom");
        if (slideInABottomAnimation != null) {
            slideInABottomAnimation.setDuration(fadeDurationFast);
        }
        mainLayout.startAnimation(slideInABottomAnimation);

        // Animate dialog off-screen and dismiss.
        final float remainingDistance = mContext.getResources().getDisplayMetrics().heightPixels
                - mainLayout.getTop();
        TranslateAnimation slideOut = new TranslateAnimation(
                0, 0, mainLayout.getTranslationY(), remainingDistance);
        slideOut.setDuration(fadeDurationFast);
        slideOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                dialog.dismiss();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        // Set touch listener on mainLayout to enable drag-to-dismiss.
        //noinspection ClickableViewAccessibility
        mainLayout.setOnTouchListener(new View.OnTouchListener() {
            /** Threshold for dismissing the dialog. */
            final float dismissThreshold = dipToPixels(100); // Distance to drag to dismiss.
            /** Store initial Y position of touch. */
            float touchY;
            /** Track current translation. */
            float translationY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Capture initial Y position of touch.
                        touchY = event.getRawY();
                        translationY = mainLayout.getTranslationY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        // Calculate drag distance and apply translation downwards only.
                        final float deltaY = event.getRawY() - touchY;
                        // Only allow downward drag (positive deltaY).
                        if (deltaY >= 0) {
                            mainLayout.setTranslationY(translationY + deltaY);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        // Check if dialog should be dismissed based on drag distance.
                        if (mainLayout.getTranslationY() > dismissThreshold) {
                            mainLayout.startAnimation(slideOut);
                        } else {
                            // Animate back to original position if not dragged far enough.
                            TranslateAnimation slideBack = new TranslateAnimation(
                                    0, 0, mainLayout.getTranslationY(), 0);
                            slideBack.setDuration(fadeDurationFast);
                            mainLayout.startAnimation(slideBack);
                            mainLayout.setTranslationY(0);
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });

        // Store the dialog reference.
        Function1<PlayerType, Unit> playerTypeObserver = getPlayerTypeUnitFunction(dialog);

        // Add observer to dismiss dialog when entering PiP mode.
        PlayerType.getOnChange().addObserver(playerTypeObserver);

        // Remove observer when dialog is dismissed.
        if (actionsMap != null) {
            dialog.setOnShowListener(d -> actionsMap.forEach((view, action) ->
                    view.setOnClickListener(v -> {
                        PlayerType.getOnChange().removeObserver(playerTypeObserver);
                        mainLayout.startAnimation(slideOut);
                        action.run();
                    })
            ));
        }

        // Remove observer when dialog is dismissed.
        dialog.setOnDismissListener(d -> {
            PlayerType.getOnChange().removeObserver(playerTypeObserver);
            Logger.printDebug(() -> "PlayerType observer removed on dialog dismiss");
        });

        dialog.show(); // Display the dialog.
    }

    @NonNull
    private static Function1<PlayerType, Unit> getPlayerTypeUnitFunction(Dialog dialog) {
        WeakReference<Dialog> currentDialog = new WeakReference<>(dialog);

        // Create observer for PlayerType changes.
        // Should never happen.
        return new Function1<>() {
            @Override
            public Unit invoke(PlayerType type) {
                Dialog current = currentDialog.get();
                if (current == null || !current.isShowing()) {
                    // Should never happen.
                    PlayerType.getOnChange().removeObserver(this);
                    Logger.printException(() -> "Removing player type listener as dialog is null or closed");
                } else if (type == PlayerType.WATCH_WHILE_PICTURE_IN_PICTURE) {
                    current.dismiss();
                    Logger.printDebug(() -> "Playback speed dialog dismissed due to PiP mode");
                }
                return Unit.INSTANCE;
            }
        };
    }

    public static LinearLayout createItemLayout(Context mContext, String title) {
        return createItemLayout(mContext, title, 0);
    }

    public static LinearLayout createItemLayout(Context mContext, String title, int iconId) {
        // Item Layout
        LinearLayout itemLayout = new LinearLayout(mContext);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setPadding(dipToPixels(16), dipToPixels(12), dipToPixels(16), dipToPixels(12));
        itemLayout.setGravity(Gravity.CENTER_VERTICAL);
        itemLayout.setClickable(true);
        itemLayout.setFocusable(true);

        // Create a StateListDrawable for the background
        StateListDrawable background = new StateListDrawable();
        ColorDrawable pressedDrawable = new ColorDrawable(ThemeUtils.getAdjustedBackgroundColor(true));
        ColorDrawable defaultDrawable = new ColorDrawable(Color.TRANSPARENT);
        background.addState(new int[]{android.R.attr.state_pressed}, pressedDrawable);
        background.addState(new int[]{}, defaultDrawable);
        itemLayout.setBackground(background);

        // Icon
        ColorFilter cf = new PorterDuffColorFilter(ThemeUtils.getAppForegroundColor(), PorterDuff.Mode.SRC_ATOP);
        ImageView iconView = new ImageView(mContext);
        if (iconId != 0) {
            iconView.setImageResource(iconId);
            iconView.setVisibility(View.VISIBLE);
        } else {
            iconView.setVisibility(View.GONE);
        }
        iconView.setColorFilter(cf);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dipToPixels(24), dipToPixels(24));
        iconParams.setMarginEnd(dipToPixels(16));
        iconView.setLayoutParams(iconParams);
        itemLayout.addView(iconView);

        // Text container
        LinearLayout textContainer = new LinearLayout(mContext);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = new TextView(mContext);
        titleView.setText(title);
        titleView.setTextSize(16);
        titleView.setTextColor(ThemeUtils.getAppForegroundColor());
        textContainer.addView(titleView);

        itemLayout.addView(textContainer);

        return itemLayout;
    }

    public static class CustomAdapter extends ArrayAdapter<String> {
        private int selectedPosition = -1;

        public CustomAdapter(@NonNull Context context, @NonNull List<String> objects) {
            super(context, 0, objects);
        }

        void setSelectedPosition(int position) {
            this.selectedPosition = position;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            ViewHolder viewHolder;

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(
                        ResourceUtils.getLayoutIdentifier("revanced_custom_list_item_checked"),
                        parent,
                        false
                );
                viewHolder = new ViewHolder();
                viewHolder.checkIcon = convertView.findViewById(
                        ResourceUtils.getIdIdentifier("revanced_check_icon")
                );
                viewHolder.placeholder = convertView.findViewById(
                        ResourceUtils.getIdIdentifier("revanced_check_icon_placeholder")
                );
                viewHolder.textView = convertView.findViewById(
                        ResourceUtils.getIdIdentifier("revanced_item_text")
                );
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.textView.setText(getItem(position));
            final boolean isSelected = position == selectedPosition;
            viewHolder.checkIcon.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            viewHolder.placeholder.setVisibility(isSelected ? View.GONE : View.INVISIBLE);

            return convertView;
        }

        private static class ViewHolder {
            ImageView checkIcon;
            View placeholder;
            TextView textView;
        }
    }

}
