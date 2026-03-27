package app.morphe.extension.youtube.settings.preference;

import static app.morphe.extension.shared.utils.StringRef.str;

import android.app.Dialog;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.apache.commons.lang3.BooleanUtils;

import java.util.ArrayList;

import app.morphe.extension.shared.ui.CustomDialog;
import app.morphe.extension.youtube.patches.utils.PatchStatus;
import app.morphe.extension.youtube.utils.ExtendedUtils;
import app.morphe.extension.youtube.utils.ThemeUtils;
import app.morphe.extension.youtube.whitelist.VideoChannel;
import app.morphe.extension.youtube.whitelist.Whitelist;
import app.morphe.extension.youtube.whitelist.Whitelist.WhitelistType;

@SuppressWarnings({"unused", "deprecation"})
public class WhitelistedChannelsPreference extends Preference implements Preference.OnPreferenceClickListener {

    private static final WhitelistType whitelistTypePlaybackSpeed = WhitelistType.PLAYBACK_SPEED;
    private static final WhitelistType whitelistTypeSponsorBlock = WhitelistType.SPONSOR_BLOCK;
    private static final boolean playbackSpeedIncluded = PatchStatus.VideoPlayback();
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
        // Create the custom dialog.
        Pair<Dialog, LinearLayout> dialogPair = CustomDialog.create(
                context,
                str("revanced_whitelist_settings_title"), // Title.
                null, // No message (replaced by contentLayout).
                null, // No EditText.
                null, // OK button text.
                null, // OK button action.
                () -> {
                }, // Cancel button action (dismiss only).
                null, // No Neutral button text.
                null, // Neutral button action.
                false // Do not dismiss dialog on Neutral button click.
        );
        Dialog dialog = dialogPair.first;

        // Create the main layout for the dialog content.
        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0; i < mEntries.length; i++) {
            int finalI = i;
            LinearLayout itemLayout = ExtendedUtils.createItemLayout(context, mEntries[finalI]);
            itemLayout.setOnClickListener(v -> {
                showWhitelistedChannelDialog(context, mEntryValues[finalI]);
                dialog.dismiss();
            });
            contentLayout.addView(itemLayout);
        }

        // Create ScrollView to wrap the content layout.
        ScrollView contentScrollView = new ScrollView(context);
        contentScrollView.setVerticalScrollBarEnabled(false); // Disable vertical scrollbar.
        contentScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER); // Disable overscroll effect.
        LinearLayout.LayoutParams scrollViewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
        );
        contentScrollView.setLayoutParams(scrollViewParams);
        contentScrollView.addView(contentLayout);

        // Add the ScrollView to the dialog's main layout.
        LinearLayout dialogMainLayout = dialogPair.second;
        dialogMainLayout.addView(contentScrollView, dialogMainLayout.getChildCount() - 1);

        dialog.show();
    }

    private static void showWhitelistedChannelDialog(Context context, WhitelistType whitelistType) {
        final ArrayList<VideoChannel> mEntries = Whitelist.getWhitelistedChannels(whitelistType);

        // Create the main layout for the dialog content.
        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);

        if (mEntries.isEmpty()) {
            TextView textView = new TextView(context);
            textView.setText(str("revanced_whitelist_empty"));
            textView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            textView.setTextSize(16);
            textView.setPadding(60, 40, 60, 0);
            contentLayout.addView(textView);
        } else {
            for (VideoChannel entry : mEntries) {
                String author = entry.getChannelName();
                Runnable runnable = () -> {
                    // Create the custom dialog.
                    Pair<Dialog, LinearLayout> dialogPair = CustomDialog.create(
                            context,
                            null, // No title.
                            str("revanced_whitelist_remove_dialog_message", author, whitelistType.getFriendlyName()), // Message.
                            null, // No EditText.
                            null, // OK button text.
                            () -> {
                                // OK button action.
                                Whitelist.removeFromWhitelist(whitelistType, entry.getChannelId());
                                contentLayout.removeView(contentLayout.findViewWithTag(author));
                            }, // OK button action (dismiss only).
                            () -> {
                            }, // Cancel button action (dismiss only).
                            null, // No Neutral button text.
                            null, // Neutral button action.
                            false // Do not dismiss dialog on Neutral button click.
                    );

                    dialogPair.first.show();
                };
                View entryView = getEntryView(context, author, v -> runnable.run());
                entryView.setTag(author);
                contentLayout.addView(entryView);
            }
        }

        // Create ScrollView to wrap the content layout.
        ScrollView contentScrollView = new ScrollView(context);
        contentScrollView.setVerticalScrollBarEnabled(false); // Disable vertical scrollbar.
        contentScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER); // Disable overscroll effect.
        LinearLayout.LayoutParams scrollViewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
        );
        contentScrollView.setLayoutParams(scrollViewParams);
        contentScrollView.addView(contentLayout);

        // Create the custom dialog.
        Pair<Dialog, LinearLayout> dialogPair = CustomDialog.create(
                context,
                whitelistType.getFriendlyName(), // Title.
                null, // No message (replaced by contentLayout).
                null, // No EditText.
                null, // OK button text.
                () -> {
                }, // OK button action (dismiss only).
                () -> {
                }, // Cancel button action (dismiss only).
                null, // No Neutral button text.
                null, // Neutral button action.
                false // Do not dismiss dialog on Neutral button click.
        );

        // Add the ScrollView to the dialog's main layout.
        LinearLayout dialogMainLayout = dialogPair.second;
        dialogMainLayout.addView(contentScrollView, dialogMainLayout.getChildCount() - 1);

        dialogPair.first.show();
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