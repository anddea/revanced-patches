package app.revanced.extension.youtube.whitelist;

import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.isSDKAbove;
import static app.revanced.extension.shared.utils.Utils.showToastShort;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.widget.Button;

import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.patches.utils.PatchStatus;
import app.revanced.extension.youtube.shared.VideoInformation;
import app.revanced.extension.youtube.utils.ThemeUtils;

@SuppressWarnings("deprecation")
public class Whitelist {
    private static final String ZERO_WIDTH_SPACE_CHARACTER = "\u200B";
    private static final Map<WhitelistType, ArrayList<VideoChannel>> whitelistMap = parseWhitelist();

    private static final WhitelistType whitelistTypePlaybackSpeed = WhitelistType.PLAYBACK_SPEED;
    private static final WhitelistType whitelistTypeSponsorBlock = WhitelistType.SPONSOR_BLOCK;
    private static final String whitelistIncluded = str("revanced_whitelist_included");
    private static final String whitelistExcluded = str("revanced_whitelist_excluded");
    private static Drawable playbackSpeedDrawable;
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
     * @noinspection unchecked
     */
    private static Map<WhitelistType, ArrayList<VideoChannel>> parseWhitelist() {
        WhitelistType[] whitelistTypes = WhitelistType.values();
        Map<WhitelistType, ArrayList<VideoChannel>> whitelistMap = new EnumMap<>(WhitelistType.class);

        for (WhitelistType whitelistType : whitelistTypes) {
            SharedPreferences preferences = getPreferences(whitelistType.getPreferencesName());
            String serializedChannels = preferences.getString("channels", null);
            if (serializedChannels == null) {
                whitelistMap.put(whitelistType, new ArrayList<>());
                continue;
            }
            try {
                Object channelsObject = deserialize(serializedChannels);
                ArrayList<VideoChannel> deserializedChannels = (ArrayList<VideoChannel>) channelsObject;
                whitelistMap.put(whitelistType, deserializedChannels);
            } catch (Exception ex) {
                Logger.printException(() -> "parseWhitelist failure", ex);
            }
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

    private static boolean updateWhitelist(WhitelistType whitelistType, ArrayList<VideoChannel> channels) {
        SharedPreferences.Editor editor = getPreferences(whitelistType.getPreferencesName()).edit();

        final String channelName = serialize(channels);
        if (channelName != null && !channelName.isEmpty()) {
            editor.putString("channels", channelName);
            editor.apply();
            return true;
        }
        return false;
    }

    public static ArrayList<VideoChannel> getWhitelistedChannels(WhitelistType whitelistType) {
        return whitelistMap.get(whitelistType);
    }

    private static SharedPreferences getPreferences(@NonNull String prefName) {
        final Context context = Utils.getContext();
        return context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
    }

    private static String serialize(Serializable obj) {
        try {
            if (obj != null) {
                ByteArrayOutputStream serialObj = new ByteArrayOutputStream();
                Deflater def = new Deflater(Deflater.BEST_COMPRESSION);
                ObjectOutputStream objStream =
                        new ObjectOutputStream(new DeflaterOutputStream(serialObj, def));
                objStream.writeObject(obj);
                objStream.close();
                return encodeBytes(serialObj.toByteArray());
            }
        } catch (IOException ex) {
            Logger.printException(() -> "Serialization error: " + ex.getMessage(), ex);
        }
        return null;
    }

    private static Object deserialize(@NonNull String str) {
        try {
            final ByteArrayInputStream serialObj = new ByteArrayInputStream(decodeBytes(str));
            final ObjectInputStream objStream = new ObjectInputStream(new InflaterInputStream(serialObj));
            return objStream.readObject();
        } catch (ClassNotFoundException | IOException ex) {
            Logger.printException(() -> "Deserialization error: " + ex.getMessage(), ex);
        }
        return null;
    }

    private static String encodeBytes(byte[] bytes) {
        if (isSDKAbove(26)) {
            return Base64.getEncoder().encodeToString(bytes);
        } else {
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private static byte[] decodeBytes(String str) {
        if (isSDKAbove(26)) {
            return Base64.getDecoder().decode(str.getBytes(StandardCharsets.UTF_8));
        } else {
            return str.getBytes(StandardCharsets.UTF_8);
        }
    }

    public enum WhitelistType {
        PLAYBACK_SPEED(),
        SPONSOR_BLOCK();

        private final String friendlyName;
        private final String preferencesName;

        WhitelistType() {
            String name = name().toLowerCase();
            this.friendlyName = str("revanced_whitelist_" + name);
            this.preferencesName = "whitelist_" + name;
        }

        public String getFriendlyName() {
            return friendlyName;
        }

        public String getPreferencesName() {
            return preferencesName;
        }
    }
}
