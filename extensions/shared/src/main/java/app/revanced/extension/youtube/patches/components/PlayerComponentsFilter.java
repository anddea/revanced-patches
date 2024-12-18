package app.revanced.extension.youtube.patches.components;

import androidx.annotation.Nullable;

import app.revanced.extension.shared.patches.components.Filter;
import app.revanced.extension.shared.patches.components.StringFilterGroup;
import app.revanced.extension.shared.patches.components.StringFilterGroupList;
import app.revanced.extension.shared.utils.StringTrieSearch;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.PlayerType;

@SuppressWarnings("unused")
public final class PlayerComponentsFilter extends Filter {
    private final StringFilterGroupList channelBarGroupList = new StringFilterGroupList();
    private final StringFilterGroup channelBar;
    private final StringTrieSearch suggestedActionsException = new StringTrieSearch();
    private final StringFilterGroup suggestedActions;

    public PlayerComponentsFilter() {
        suggestedActionsException.addPatterns(
                "channel_bar",
                "shorts"
        );

        // The player audio track button does the exact same function as the audio track flyout menu option.
        // But if the copy url button is shown, these button clashes and the the audio button does not work.
        // Previously this was a setting to show/hide the player button.
        // But it was decided it's simpler to always hide this button because:
        // - it doesn't work with copy video url feature
        // - the button is rare
        // - always hiding makes the ReVanced settings simpler and easier to understand
        // - nobody is going to notice the redundant button is always hidden
        final StringFilterGroup audioTrackButton = new StringFilterGroup(
                null,
                "multi_feed_icon_button"
        );

        channelBar = new StringFilterGroup(
                null,
                "channel_bar_inner"
        );

        final StringFilterGroup channelWaterMark = new StringFilterGroup(
                Settings.HIDE_CHANNEL_WATERMARK,
                "featured_channel_watermark_overlay.eml"
        );

        final StringFilterGroup infoCards = new StringFilterGroup(
                Settings.HIDE_INFO_CARDS,
                "info_card_teaser_overlay.eml"
        );

        final StringFilterGroup infoPanel = new StringFilterGroup(
                Settings.HIDE_INFO_PANEL,
                "compact_banner",
                "publisher_transparency_panel",
                "single_item_information_panel"
        );

        final StringFilterGroup liveChatMessages = new StringFilterGroup(
                Settings.HIDE_LIVE_CHAT_MESSAGES,
                "live_chat_text_message",
                "viewer_engagement_message" // message about poll, not poll itself
        );

        final StringFilterGroup liveChatSummary = new StringFilterGroup(
                Settings.HIDE_LIVE_CHAT_SUMMARY,
                "live_chat_summary_banner"
        );

        final StringFilterGroup medicalPanel = new StringFilterGroup(
                Settings.HIDE_MEDICAL_PANEL,
                "emergency_onebox",
                "medical_panel"
        );

        final StringFilterGroup seekMessage = new StringFilterGroup(
                Settings.HIDE_SEEK_MESSAGE,
                "seek_edu_overlay"
        );

        suggestedActions = new StringFilterGroup(
                Settings.HIDE_SUGGESTED_ACTION,
                "|suggested_action.eml|"
        );

        final StringFilterGroup timedReactions = new StringFilterGroup(
                Settings.HIDE_TIMED_REACTIONS,
                "emoji_control_panel",
                "timed_reaction"
        );

        addPathCallbacks(
                audioTrackButton,
                channelBar,
                channelWaterMark,
                infoCards,
                infoPanel,
                liveChatMessages,
                liveChatSummary,
                medicalPanel,
                seekMessage,
                suggestedActions,
                timedReactions
        );

        final StringFilterGroup joinMembership = new StringFilterGroup(
                Settings.HIDE_JOIN_BUTTON,
                "compact_sponsor_button",
                "|ContainerType|button.eml|"
        );

        final StringFilterGroup startTrial = new StringFilterGroup(
                Settings.HIDE_START_TRIAL_BUTTON,
                "channel_purchase_button"
        );

        channelBarGroupList.addAll(
                joinMembership,
                startTrial
        );
    }

    @Override
    public boolean isFiltered(String path, @Nullable String identifier, String allValue, byte[] protobufBufferArray,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == suggestedActions) {
            // suggested actions button on shorts and the suggested actions button on video players use the same path builder.
            // Check PlayerType to make each setting work independently.
            if (suggestedActionsException.matches(path) || PlayerType.getCurrent().isNoneOrHidden()) {
                return false;
            }
        } else if (matchedGroup == channelBar) {
            if (!channelBarGroupList.check(path).isFiltered()) {
                return false;
            }
        }

        return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
    }
}
