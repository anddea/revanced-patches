package app.revanced.extension.youtube.whitelist;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.widget.Button;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.patches.utils.PatchStatus;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.VideoInformation;
import app.revanced.extension.youtube.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.showToastShort;

/**
 * Manages whitelisting functionality for YouTube channels, allowing specific channels to bypass
 * restrictions for features like playback speed or SponsorBlock.
 */
@SuppressWarnings("deprecation")
public class Whitelist {
    /**
     * Zero-width space character used for dialog button placeholders.
     */
    private static final String ZERO_WIDTH_SPACE_CHARACTER = "\u200B";

    /**
     * Map storing whitelisted channels for each whitelist type.
     */
    private static final Map<WhitelistType, ArrayList<VideoChannel>> whitelistMap = parseWhitelist();

    /**
     * Whitelist type for playback speed feature.
     */
    private static final WhitelistType whitelistTypePlaybackSpeed = WhitelistType.PLAYBACK_SPEED;

    /**
     * Whitelist type for SponsorBlock feature.
     */
    private static final WhitelistType whitelistTypeSponsorBlock = WhitelistType.SPONSOR_BLOCK;

    /**
     * String resource for indicating a channel is whitelisted.
     */
    private static final String whitelistIncluded = str("revanced_whitelist_included");

    /**
     * String resource for indicating a channel is not whitelisted.
     */
    private static final String whitelistExcluded = str("revanced_whitelist_excluded");

    /**
     * Drawable for playback speed button icon.
     */
    private static Drawable playbackSpeedDrawable;

    /**
     * Drawable for SponsorBlock button icon.
     */
    private static Drawable sponsorBlockDrawable;

    static {
        final Resources resource = Utils.getResources();

        final int playbackSpeedDrawableId = ResourceUtils.getDrawableIdentifier("yt_outline_play_arrow_half_circle_black_24");
        if (playbackSpeedDrawableId != 0) {
            playbackSpeedDrawable = resource.getDrawable(playbackSpeedDrawableId);
        }

        final int sponsorBlockDrawableId = ResourceUtils.getDrawableIdentifier("revanced_sb_logo");
        if (sponsorBlockDrawableId != 0) {
            sponsorBlockDrawable = resource.getDrawable(sponsorBlockDrawableId);
        }
    }

    /**
     * Checks if a channel is whitelisted for SponsorBlock.
     *
     * @param channelId The ID of the channel to check.
     * @return True if the channel is whitelisted, false otherwise.
     */
    public static boolean isChannelWhitelistedSponsorBlock(String channelId) {
        return isWhitelisted(whitelistTypeSponsorBlock, channelId);
    }

    /**
     * Checks if a channel is whitelisted for playback speed.
     *
     * @param channelId The ID of the channel to check.
     * @return True if the channel is whitelisted, false otherwise.
     */
    public static boolean isChannelWhitelistedPlaybackSpeed(String channelId) {
        return isWhitelisted(whitelistTypePlaybackSpeed, channelId);
    }

    /**
     * Displays a dialog to manage whitelist settings for the current channel.
     *
     * @param context The context used to create the dialog.
     */
    public static void showWhitelistDialog(Context context) {
        final String channelId = VideoInformation.getChannelId();
        final String channelName = VideoInformation.getChannelName();

        if (channelId.isEmpty() || channelName.isEmpty()) {
            Utils.showToastShort(str("revanced_whitelist_failure_generic"));
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(channelName);

        StringBuilder sb = new StringBuilder("\n");

        if (PatchStatus.RememberPlaybackSpeed()) {
            appendStringBuilder(sb, whitelistTypePlaybackSpeed, channelId, false);
            builder.setNeutralButton(ZERO_WIDTH_SPACE_CHARACTER,
                    (dialog, id) -> whitelistListener(
                            whitelistTypePlaybackSpeed,
                            channelId,
                            channelName
                    )
            );
        }

        if (PatchStatus.SponsorBlock()) {
            appendStringBuilder(sb, whitelistTypeSponsorBlock, channelId, true);
            builder.setPositiveButton(ZERO_WIDTH_SPACE_CHARACTER,
                    (dialog, id) -> whitelistListener(
                            whitelistTypeSponsorBlock,
                            channelId,
                            channelName
                    )
            );
        }

        builder.setMessage(sb.toString());

        AlertDialog dialog = builder.show();

        final ColorFilter cf = new PorterDuffColorFilter(ThemeUtils.getForegroundColor(), PorterDuff.Mode.SRC_ATOP);
        Button sponsorBlockButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button playbackSpeedButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        if (sponsorBlockButton != null && sponsorBlockDrawable != null) {
            sponsorBlockDrawable.setColorFilter(cf);
            sponsorBlockButton.setCompoundDrawablesWithIntrinsicBounds(null, null, sponsorBlockDrawable, null);
            sponsorBlockButton.setContentDescription(str("revanced_whitelist_sponsor_block"));
        }
        if (playbackSpeedButton != null && playbackSpeedDrawable != null) {
            playbackSpeedDrawable.setColorFilter(cf);
            playbackSpeedButton.setCompoundDrawablesWithIntrinsicBounds(playbackSpeedDrawable, null, null, null);
            playbackSpeedButton.setContentDescription(str("revanced_whitelist_playback_speed"));
        }
    }

    /**
     * Appends whitelist status text to a StringBuilder for display in the dialog.
     *
     * @param sb            The StringBuilder to append to.
     * @param whitelistType The type of whitelist.
     * @param channelId     The ID of the channel.
     * @param eol           Whether to append an extra newline at the end.
     */
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

    /**
     * Handles whitelist toggle actions for a channel.
     *
     * @param whitelistType The type of whitelist.
     * @param channelId     The ID of the channel.
     * @param channelName   The name of the channel.
     */
    private static void whitelistListener(WhitelistType whitelistType, String channelId, String channelName) {
        try {
            if (isWhitelisted(whitelistType, channelId)) {
                removeFromWhitelist(whitelistType, channelId);
            } else {
                addToWhitelist(whitelistType, channelId, channelName);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "whitelistListener failure", ex);
        }
    }

    /**
     * Parses the serialized whitelist data into a map of whitelist types and channels.
     *
     * @return A map containing whitelisted channels for each whitelist type.
     */
    private static Map<WhitelistType, ArrayList<VideoChannel>> parseWhitelist() {
        WhitelistType[] whitelistTypes = WhitelistType.values();
        Map<WhitelistType, ArrayList<VideoChannel>> whitelistMap = new EnumMap<>(WhitelistType.class);

        for (WhitelistType whitelistType : whitelistTypes) {
            String serializedChannels = whitelistType == WhitelistType.PLAYBACK_SPEED
                    ? Settings.OVERLAY_BUTTON_WHITELIST_PLAYBACK_SPEED.get()
                    : Settings.OVERLAY_BUTTON_WHITELIST_SPONSORBLOCK.get();
            ArrayList<VideoChannel> channels = new ArrayList<>();
            if (!TextUtils.isEmpty(serializedChannels)) {
                try {
                    String[] parts = TextUtils.split(serializedChannels, "~");
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

    /**
     * Checks if a channel is whitelisted for a specific whitelist type.
     *
     * @param whitelistType The type of whitelist.
     * @param channelId     The ID of the channel.
     * @return True if the channel is whitelisted, false otherwise.
     */
    private static boolean isWhitelisted(WhitelistType whitelistType, String channelId) {
        for (VideoChannel channel : getWhitelistedChannels(whitelistType)) {
            if (channel.getChannelId().equals(channelId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a channel to the whitelist for a specific whitelist type.
     *
     * @param whitelistType The type of whitelist.
     * @param channelId     The ID of the channel.
     * @param channelName   The name of the channel.
     */
    private static void addToWhitelist(WhitelistType whitelistType, String channelId, String channelName) {
        final VideoChannel channel = new VideoChannel(channelName, channelId);
        ArrayList<VideoChannel> whitelisted = getWhitelistedChannels(whitelistType);
        for (VideoChannel whitelistedChannel : whitelisted) {
            if (whitelistedChannel.getChannelId().equals(channel.getChannelId()))
                return;
        }
        whitelisted.add(channel);
        String friendlyName = whitelistType.getFriendlyName();
        if (updateWhitelist(whitelistType, whitelisted)) {
            showToastShort(str("revanced_whitelist_added", channelName, friendlyName));
        } else {
            showToastShort(str("revanced_whitelist_add_failed", channelName, friendlyName));
        }
    }

    /**
     * Removes a channel from the whitelist for a specific whitelist type.
     *
     * @param whitelistType The type of whitelist.
     * @param channelId     The ID of the channel.
     */
    public static void removeFromWhitelist(WhitelistType whitelistType, String channelId) {
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
            showToastShort(str("revanced_whitelist_removed", channelName, friendlyName));
        } else {
            showToastShort(str("revanced_whitelist_remove_failed", channelName, friendlyName));
        }
    }

    /**
     * Updates the serialized whitelist data for a specific whitelist type.
     *
     * @param whitelistType The type of whitelist.
     * @param channels      The list of whitelisted channels.
     * @return True if the update was successful, false otherwise.
     */
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

    /**
     * Retrieves the list of whitelisted channels for a specific whitelist type.
     *
     * @param whitelistType The type of whitelist.
     * @return The list of whitelisted channels.
     */
    public static ArrayList<VideoChannel> getWhitelistedChannels(WhitelistType whitelistType) {
        return whitelistMap.get(whitelistType);
    }

    /**
     * Enum representing the types of whitelists available.
     */
    public enum WhitelistType {
        /**
         * Whitelist for playback speed settings.
         */
        PLAYBACK_SPEED,

        /**
         * Whitelist for SponsorBlock settings.
         */
        SPONSOR_BLOCK;

        /**
         * Friendly name for display purposes.
         */
        private final String friendlyName;

        WhitelistType() {
            String name = name().toLowerCase();
            this.friendlyName = str("revanced_whitelist_" + name);
        }

        /**
         * Gets the friendly name of the whitelist type.
         *
         * @return The friendly name.
         */
        public String getFriendlyName() {
            return friendlyName;
        }
    }
}
