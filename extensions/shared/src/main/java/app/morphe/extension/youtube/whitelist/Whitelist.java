package app.morphe.extension.youtube.whitelist;

import static app.morphe.extension.shared.utils.BaseThemeUtils.getAppForegroundColor;
import static app.morphe.extension.shared.utils.BaseThemeUtils.getDialogBackgroundColor;
import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.shared.utils.Utils.createCornerRadii;
import static app.morphe.extension.shared.utils.Utils.dipToPixels;
import static app.morphe.extension.shared.utils.Utils.showToastShort;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import app.morphe.extension.shared.ui.CustomDialog;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.patches.utils.PatchStatus;
import app.morphe.extension.youtube.settings.Settings; // Add this import
import app.morphe.extension.youtube.shared.VideoInformation;
import app.morphe.extension.youtube.utils.VideoUtils;

public class Whitelist {
    private static final Map<WhitelistType, ArrayList<VideoChannel>> whitelistMap = parseWhitelist();

    private static final WhitelistType whitelistTypePlaybackSpeed = WhitelistType.PLAYBACK_SPEED;
    private static final WhitelistType whitelistTypeSponsorBlock = WhitelistType.SPONSOR_BLOCK;
    private static final String whitelistIncluded = str("revanced_whitelist_included");
    private static final String whitelistExcluded = str("revanced_whitelist_excluded");

    public static boolean isChannelWhitelistedSponsorBlock(String channelId) {
        return isWhitelisted(whitelistTypeSponsorBlock, channelId);
    }

    public static boolean isChannelWhitelistedPlaybackSpeed(String channelId) {
        return isWhitelisted(whitelistTypePlaybackSpeed, channelId);
    }

    public static void showWhitelistDialog(Context context) {
        final String channelId = VideoInformation.getChannelId();
        final String channelName = VideoInformation.getChannelName();

        if (channelId.isEmpty() || channelName.isEmpty()) {
            Utils.showToastShort(str("revanced_whitelist_failure_generic"));
            return;
        }

        Logger.printDebug(() -> "Creating custom dialog");

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // Remove default title bar.

        // Preset size constants.
        final int dip4 = dipToPixels(4);
        final int dip8 = dipToPixels(8);
        final int dip16 = dipToPixels(16);
        final int dip24 = dipToPixels(24);

        // Create main layout.
        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(dip24, dip16, dip24, dip24);
        // Set rounded rectangle background.
        ShapeDrawable mainBackground = new ShapeDrawable(new RoundRectShape(
                createCornerRadii(28), null, null));
        mainBackground.getPaint().setColor(getDialogBackgroundColor()); // Dialog background.
        mainLayout.setBackground(mainBackground);

        // Title.
        TextView titleView = new TextView(context);
        titleView.setText(channelName);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setTextSize(18);
        titleView.setTextColor(getAppForegroundColor());
        titleView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 0, 0, dip16);
        titleView.setLayoutParams(layoutParams);
        mainLayout.addView(titleView);

        StringBuilder sb = new StringBuilder("\n");

        if (PatchStatus.VideoPlayback()) {
            appendStringBuilder(sb, whitelistTypePlaybackSpeed, channelId, false);
        }

        if (PatchStatus.SponsorBlock()) {
            appendStringBuilder(sb, whitelistTypeSponsorBlock, channelId, true);
        }

        // Create content container (message) inside a ScrollView.
        ScrollView contentScrollView = new ScrollView(context);
        LinearLayout contentContainer = new LinearLayout(context);
        contentScrollView.setVerticalScrollBarEnabled(false); // Disable the vertical scrollbar.
        contentScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f // Weight to take available space.
        );
        contentScrollView.setLayoutParams(contentParams);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentScrollView.addView(contentContainer);

        // Message (if not replaced by EditText).
        TextView messageView = new TextView(context);
        messageView.setText(sb.toString()); // Supports Spanned (HTML).
        messageView.setTextSize(16);
        messageView.setTextColor(getAppForegroundColor());
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        messageView.setLayoutParams(messageParams);
        contentContainer.addView(messageView);

        // Button container.
        LinearLayout buttonContainer = new LinearLayout(context);
        buttonContainer.setOrientation(LinearLayout.VERTICAL);
        buttonContainer.removeAllViews();
        LinearLayout.LayoutParams buttonContainerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonContainerParams.setMargins(0, dip16, 0, 0);
        buttonContainer.setLayoutParams(buttonContainerParams);

        // Lists to track buttons.
        List<Button> buttons = new ArrayList<>();
        List<Integer> buttonWidths = new ArrayList<>();

        // Create buttons in order: Neutral, Cancel, OK.
        if (PatchStatus.VideoPlayback()) {
            Button playbackSpeedButton = Utils.addButton(
                    context,
                    whitelistTypePlaybackSpeed.friendlyName,
                    () -> whitelistListener(
                            context,
                            whitelistTypePlaybackSpeed,
                            channelId,
                            channelName
                    ),
                    false,
                    true,
                    dialog
            );
            buttons.add(playbackSpeedButton);
            playbackSpeedButton.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            buttonWidths.add(playbackSpeedButton.getMeasuredWidth());
        }
        if (PatchStatus.SponsorBlock()) {
            Button sponsorBlockButton = Utils.addButton(
                    context,
                    whitelistTypeSponsorBlock.friendlyName,
                    () -> whitelistListener(
                            context,
                            whitelistTypeSponsorBlock,
                            channelId,
                            channelName
                    ),
                    true,
                    true,
                    dialog
            );
            buttons.add(sponsorBlockButton);
            sponsorBlockButton.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            buttonWidths.add(sponsorBlockButton.getMeasuredWidth());
        }

        // Handle button layout.
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int totalWidth = 0;
        for (Integer width : buttonWidths) {
            totalWidth += width;
        }
        if (buttonWidths.size() > 1) {
            totalWidth += (buttonWidths.size() - 1) * dip8; // Add margins for gaps.
        }

        if (buttons.size() == 1) {
            // Single button: stretch to full width.
            Button singleButton = buttons.get(0);
            LinearLayout singleContainer = new LinearLayout(context);
            singleContainer.setOrientation(LinearLayout.HORIZONTAL);
            singleContainer.setGravity(Gravity.CENTER);
            ViewGroup parent = (ViewGroup) singleButton.getParent();
            if (parent != null) {
                parent.removeView(singleButton);
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dipToPixels(36)
            );
            params.setMargins(0, 0, 0, 0);
            singleButton.setLayoutParams(params);
            singleContainer.addView(singleButton);
            buttonContainer.addView(singleContainer);
        } else if (buttons.size() > 1) {
            // Check if buttons fit in one row.
            if (totalWidth <= screenWidth * 0.8) {
                // Single row: Neutral, Cancel, OK.
                LinearLayout rowContainer = new LinearLayout(context);
                rowContainer.setOrientation(LinearLayout.HORIZONTAL);
                rowContainer.setGravity(Gravity.CENTER);
                rowContainer.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));

                // Add all buttons with proportional weights and specific margins.
                for (int i = 0; i < buttons.size(); i++) {
                    Button button = buttons.get(i);
                    ViewGroup parent = (ViewGroup) button.getParent();
                    if (parent != null) {
                        parent.removeView(button);
                    }
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            0,
                            dipToPixels(36),
                            buttonWidths.get(i) // Use measured width as weight.
                    );
                    // Set margins based on button type and combination.
                    if (buttons.size() == 2) {
                        // Neutral + OK or Cancel + OK.
                        if (i == 0) { // Neutral or Cancel.
                            params.setMargins(0, 0, dip4, 0);
                        } else { // OK
                            params.setMargins(dip4, 0, 0, 0);
                        }
                    } else if (buttons.size() == 3) {
                        if (i == 0) { // Neutral.
                            params.setMargins(0, 0, dip4, 0);
                        } else if (i == 1) { // Cancel
                            params.setMargins(dip4, 0, dip4, 0);
                        } else { // OK
                            params.setMargins(dip4, 0, 0, 0);
                        }
                    }
                    button.setLayoutParams(params);
                    rowContainer.addView(button);
                }
                buttonContainer.addView(rowContainer);
            } else {
                // Multiple rows: OK, Cancel, Neutral.
                List<Button> reorderedButtons = new ArrayList<>();
                // Reorder: OK, Cancel, Neutral.
                if (PatchStatus.SponsorBlock()) {
                    reorderedButtons.add(buttons.get(buttons.size() - 1));
                }
                if (PatchStatus.VideoPlayback()) {
                    reorderedButtons.add(buttons.get(0));
                }

                // Add each button in its own row with spacers.
                for (int i = 0; i < reorderedButtons.size(); i++) {
                    Button button = reorderedButtons.get(i);
                    LinearLayout singleContainer = new LinearLayout(context);
                    singleContainer.setOrientation(LinearLayout.HORIZONTAL);
                    singleContainer.setGravity(Gravity.CENTER);
                    singleContainer.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dipToPixels(36)
                    ));
                    ViewGroup parent = (ViewGroup) button.getParent();
                    if (parent != null) {
                        parent.removeView(button);
                    }
                    LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dipToPixels(36)
                    );
                    buttonParams.setMargins(0, 0, 0, 0);
                    button.setLayoutParams(buttonParams);
                    singleContainer.addView(button);
                    buttonContainer.addView(singleContainer);

                    // Add a spacer between the buttons (except the last one).
                    // Adding a margin between buttons is not suitable, as it conflicts with the single row layout.
                    if (i < reorderedButtons.size() - 1) {
                        View spacer = new View(context);
                        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                dipToPixels(8)
                        );
                        spacer.setLayoutParams(spacerParams);
                        buttonContainer.addView(spacer);
                    }
                }
            }
        }

        // Add ScrollView to main layout only if content exist.
        mainLayout.addView(contentScrollView);

        mainLayout.addView(buttonContainer);
        dialog.setContentView(mainLayout);

        // Set dialog window attributes.
        Window window = dialog.getWindow();
        if (window != null) {
            Utils.setDialogWindowParameters(window, Gravity.CENTER, 0, 90, false);
        }
        dialog.show();
    }

    private static void appendStringBuilder(StringBuilder sb, WhitelistType whitelistType,
                                            String channelId, boolean eol) {
        final String status = isWhitelisted(whitelistType, channelId)
                ? whitelistIncluded
                : whitelistExcluded;
        sb.append(whitelistType.getFriendlyName());
        sb.append(":\n");
        sb.append(status);
        sb.append("\n");
        if (!eol) sb.append("\n");
    }

    private static void whitelistListener(Context context, WhitelistType whitelistType, String channelId, String channelName) {
        try {
            if (isWhitelisted(whitelistType, channelId)) {
                removeFromWhitelist(whitelistType, channelId, context);
            } else {
                addToWhitelist(whitelistType, channelId, channelName, context);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "whitelistListener failure", ex);
        }
    }

    /**
     * Parses the serialized whitelist data into a map of whitelist types and channels.
     */
    private static Map<WhitelistType, ArrayList<VideoChannel>> parseWhitelist() {
        WhitelistType[] whitelistTypes = WhitelistType.values();
        Map<WhitelistType, ArrayList<VideoChannel>> whitelistMap = new EnumMap<>(WhitelistType.class);

        for (WhitelistType whitelistType : whitelistTypes) {
            String serializedChannels = whitelistType == WhitelistType.PLAYBACK_SPEED
                    ? Settings.OVERLAY_BUTTON_WHITELIST_PLAYBACK_SPEED.get()
                    : Settings.OVERLAY_BUTTON_WHITELIST_SPONSORBLOCK.get();
            ArrayList<VideoChannel> channels = new ArrayList<>();
            if (!serializedChannels.isEmpty()) {
                try {
                    String[] parts = serializedChannels.split("~");
                    for (int i = 0; i < parts.length - 1; i += 2) {
                        channels.add(new VideoChannel(parts[i], parts[i + 1]));
                    }
                } catch (Exception ex) {
                    Logger.printException(() -> "parseWhitelist failure", ex);
                }
            }
            whitelistMap.put(whitelistType, channels);
        }
        return whitelistMap;
    }

    private static boolean isWhitelisted(WhitelistType whitelistType, String channelId) {
        for (VideoChannel channel : getWhitelistedChannels(whitelistType)) {
            if (channel.getChannelId().equals(channelId)) {
                return true;
            }
        }
        return false;
    }

    private static void addToWhitelist(WhitelistType whitelistType, String channelId, String channelName, Context context) {
        final VideoChannel channel = new VideoChannel(channelName, channelId);
        ArrayList<VideoChannel> whitelisted = getWhitelistedChannels(whitelistType);
        for (VideoChannel whitelistedChannel : whitelisted) {
            if (whitelistedChannel.getChannelId().equals(channel.getChannelId()))
                return;
        }
        whitelisted.add(channel);
        String friendlyName = whitelistType.getFriendlyName();
        if (updateWhitelist(whitelistType, whitelisted)) {
            showDialogOrToast(context, str("revanced_whitelist_added", channelName, friendlyName));
        } else {
            showToastShort(str("revanced_whitelist_add_failed", channelName, friendlyName));
        }
    }

    public static void removeFromWhitelist(WhitelistType whitelistType, String channelId) {
        removeFromWhitelist(whitelistType, channelId, null);
    }

    public static void removeFromWhitelist(WhitelistType whitelistType, String channelId, Context context) {
        ArrayList<VideoChannel> whitelisted = getWhitelistedChannels(whitelistType);
        Iterator<VideoChannel> iterator = whitelisted.iterator();
        String channelName = "";
        while (iterator.hasNext()) {
            VideoChannel channel = iterator.next();
            if (channel.getChannelId().equals(channelId)) {
                channelName = channel.getChannelName();
                iterator.remove();
                break;
            }
        }
        String friendlyName = whitelistType.getFriendlyName();
        if (updateWhitelist(whitelistType, whitelisted)) {
            showDialogOrToast(context, str("revanced_whitelist_removed", channelName, friendlyName));
        } else {
            showToastShort(str("revanced_whitelist_remove_failed", channelName, friendlyName));
        }
    }

    private static void showDialogOrToast(Context context, String message) {
        if (context == null) {
            showToastShort(message);
            return;
        } else {
            message = message + "\n \n" + str("revanced_whitelist_reload_video");
        }
        // Create the custom dialog.
        Pair<Dialog, LinearLayout> dialogPair = CustomDialog.create(
                context,
                str("revanced_whitelist_settings_title"), // Title.
                message,                    // Message.
                null,                       // No EditText.
                null,                       // OK button text.
                VideoUtils::reloadVideo,    // Convert DialogInterface.OnClickListener to Runnable.
                () -> {
                },                   // Cancel button action (Dismiss).
                null,                       // No Neutral button text.
                null,                       // No Neutral button action.
                true                        // Dismiss dialog when onNeutralClick.
        );
        dialogPair.first.show();
    }

    private static boolean updateWhitelist(WhitelistType whitelistType, ArrayList<VideoChannel> channels) {
        StringBuilder serialized = new StringBuilder();
        for (VideoChannel channel : channels) {
            if (serialized.length() > 0) {
                serialized.append("~");
            }
            serialized.append(channel.getChannelName()).append("~").append(channel.getChannelId());
        }
        String serializedString = serialized.toString();
        try {
            if (whitelistType == WhitelistType.PLAYBACK_SPEED) {
                Settings.OVERLAY_BUTTON_WHITELIST_PLAYBACK_SPEED.save(serializedString);
            } else {
                Settings.OVERLAY_BUTTON_WHITELIST_SPONSORBLOCK.save(serializedString);
            }
            return true;
        } catch (Exception ex) {
            Logger.printException(() -> "updateWhitelist failure", ex);
            return false;
        }
    }

    public static ArrayList<VideoChannel> getWhitelistedChannels(WhitelistType whitelistType) {
        return whitelistMap.get(whitelistType);
    }

    public enum WhitelistType {
        PLAYBACK_SPEED(),
        SPONSOR_BLOCK();

        private final String friendlyName;

        WhitelistType() {
            String name = name().toLowerCase();
            this.friendlyName = str("revanced_whitelist_" + name);
        }

        public String getFriendlyName() {
            return friendlyName;
        }
    }
}
