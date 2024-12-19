package app.revanced.extension.youtube.settings.preference;

import static app.revanced.extension.shared.utils.StringRef.str;

import android.app.AlertDialog;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.lang3.BooleanUtils;

import java.util.ArrayList;

import app.revanced.extension.youtube.patches.utils.PatchStatus;
import app.revanced.extension.youtube.utils.ThemeUtils;
import app.revanced.extension.youtube.whitelist.VideoChannel;
import app.revanced.extension.youtube.whitelist.Whitelist;
import app.revanced.extension.youtube.whitelist.Whitelist.WhitelistType;

@SuppressWarnings({"unused", "deprecation"})
public class WhitelistedChannelsPreference extends Preference implements Preference.OnPreferenceClickListener {

    private static final WhitelistType whitelistTypePlaybackSpeed = WhitelistType.PLAYBACK_SPEED;
    private static final WhitelistType whitelistTypeSponsorBlock = WhitelistType.SPONSOR_BLOCK;
    private static final boolean playbackSpeedIncluded = PatchStatus.RememberPlaybackSpeed();
    private static final boolean sponsorBlockIncluded = PatchStatus.SponsorBlock();
    private static String[] mEntries;
    private static WhitelistType[] mEntryValues;

    static {
        final int entrySize = BooleanUtils.toInteger(playbackSpeedIncluded)
                + BooleanUtils.toInteger(sponsorBlockIncluded);

        if (entrySize != 0) {
            mEntries = new String[entrySize];
            mEntryValues = new WhitelistType[entrySize];

            int index = 0;
            if (playbackSpeedIncluded) {
                mEntries[index] = "  " + whitelistTypePlaybackSpeed.getFriendlyName() + "  ";
                mEntryValues[index] = whitelistTypePlaybackSpeed;
                index++;
            }
            if (sponsorBlockIncluded) {
                mEntries[index] = "  " + whitelistTypeSponsorBlock.getFriendlyName() + "  ";
                mEntryValues[index] = whitelistTypeSponsorBlock;
            }
        }
    }

    private void init() {
        setOnPreferenceClickListener(this);
    }

    public WhitelistedChannelsPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public WhitelistedChannelsPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public WhitelistedChannelsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WhitelistedChannelsPreference(Context context) {
        super(context);
        init();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        showWhitelistedChannelDialog(getContext());

        return true;
    }

    public static void showWhitelistedChannelDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(str("revanced_whitelist_settings_title"));
        builder.setItems(mEntries, (dialog, which) -> showWhitelistedChannelDialog(context, mEntryValues[which]));
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    private static void showWhitelistedChannelDialog(Context context, WhitelistType whitelistType) {
        final ArrayList<VideoChannel> mEntries = Whitelist.getWhitelistedChannels(whitelistType);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(whitelistType.getFriendlyName());

        if (mEntries.isEmpty()) {
            TextView emptyView = new TextView(context);
            emptyView.setText(str("revanced_whitelist_empty"));
            emptyView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            emptyView.setTextSize(16);
            emptyView.setPadding(60, 40, 60, 0);
            builder.setView(emptyView);
        } else {
            LinearLayout entriesContainer = new LinearLayout(context);
            entriesContainer.setOrientation(LinearLayout.VERTICAL);
            for (final VideoChannel entry : mEntries) {
                String author = entry.getChannelName();
                View entryView = getEntryView(context, author, v -> new AlertDialog.Builder(context)
                        .setMessage(str("revanced_whitelist_remove_dialog_message", author, whitelistType.getFriendlyName()))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            Whitelist.removeFromWhitelist(whitelistType, entry.getChannelId());
                            entriesContainer.removeView(entriesContainer.findViewWithTag(author));
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show());
                entryView.setTag(author);
                entriesContainer.addView(entryView);
            }
            builder.setView(entriesContainer);
        }

        builder.setPositiveButton(android.R.string.ok, null);
        builder.show();
    }

    private static View getEntryView(Context context, CharSequence entry, View.OnClickListener onDeleteClickListener) {
        LinearLayout.LayoutParams entryContainerParams = new LinearLayout.LayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
        entryContainerParams.setMargins(60, 40, 60, 0);

        LinearLayout.LayoutParams entryLabelLayoutParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        entryLabelLayoutParams.gravity = Gravity.CENTER;

        LinearLayout entryContainer = new LinearLayout(context);
        entryContainer.setOrientation(LinearLayout.HORIZONTAL);
        entryContainer.setLayoutParams(entryContainerParams);

        TextView entryLabel = new TextView(context);
        entryLabel.setText(entry);
        entryLabel.setLayoutParams(entryLabelLayoutParams);
        entryLabel.setTextSize(16);
        entryLabel.setOnClickListener(onDeleteClickListener);

        ImageButton deleteButton = new ImageButton(context);
        deleteButton.setImageDrawable(ThemeUtils.getTrashButtonDrawable());
        deleteButton.setOnClickListener(onDeleteClickListener);
        deleteButton.setBackground(null);

        entryContainer.addView(entryLabel);
        entryContainer.addView(deleteButton);
        return entryContainer;
    }

}