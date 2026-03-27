package app.morphe.extension.youtube.patches.components;

import app.morphe.extension.shared.patches.components.ByteArrayFilterGroup;
import app.morphe.extension.shared.patches.components.ByteArrayFilterGroupList;
import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;
import app.morphe.extension.shared.patches.components.StringFilterGroupList;
import app.morphe.extension.shared.utils.StringTrieSearch;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.PlayerType;
import app.morphe.extension.youtube.shared.RootView;

@SuppressWarnings("unused")
public final class PlayerComponentsFilter extends Filter {
    private final StringFilterGroupList channelBarGroupList = new StringFilterGroupList();
    private final StringFilterGroup channelBar;
    private final StringFilterGroup seekMessage;
    private final StringFilterGroup singleItemInformationPanel;
    private final StringTrieSearch suggestedActionsException = new StringTrieSearch();
    private final StringFilterGroup suggestedActions;
    private final ByteArrayFilterGroup seekMessageGroupExceptions = new ByteArrayFilterGroup(
            null,
            "yt_fill_fast_forward_vd_theme_24"
    );
    private final ByteArrayFilterGroupList seekMessageGroupList = new ByteArrayFilterGroupList();

    public PlayerComponentsFilter() {
        suggestedActionsException.addPatterns(
                "channel_bar",
                "shorts"
        );

        final StringFilterGroup crowdFundingBox = new StringFilterGroup(
                Settings.HIDE_CROWDFUNDING_BOX,
                "donation_shelf"
        );

        seekMessage = new StringFilterGroup(
                null,
                "seek_edu_overlay"
        );

        addIdentifierCallbacks(crowdFundingBox, seekMessage);

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
                "featured_channel_watermark_overlay."
        );

        final StringFilterGroup infoCards = new StringFilterGroup(
                Settings.HIDE_INFO_CARDS,
                "info_card_teaser_overlay."
        );

        final StringFilterGroup infoPanel = new StringFilterGroup(
                Settings.HIDE_INFO_PANEL,
                "compact_banner",
                "publisher_transparency_panel"
        );

        singleItemInformationPanel = new StringFilterGroup(
                Settings.HIDE_INFO_PANEL,
                "single_item_information_panel"
        );

        final StringFilterGroup medicalPanel = new StringFilterGroup(
                Settings.HIDE_MEDICAL_PANEL,
                "emergency_onebox",
                "medical_panel"
        );

        suggestedActions = new StringFilterGroup(
                Settings.HIDE_SUGGESTED_ACTION,
                "|suggested_action."
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
                medicalPanel,
                singleItemInformationPanel,
                suggestedActions,
                timedReactions
        );

        final StringFilterGroup joinMembership = new StringFilterGroup(
                Settings.HIDE_JOIN_BUTTON,
                "compact_sponsor_button",
                "|ContainerType|button."
        );

        final StringFilterGroup startTrial = new StringFilterGroup(
                Settings.HIDE_START_TRIAL_BUTTON,
                "channel_purchase_button"
        );

        channelBarGroupList.addAll(
                joinMembership,
                startTrial
        );

        seekMessageGroupList.addAll(
                new ByteArrayFilterGroup(
                        Settings.HIDE_FILMSTRIP_OVERLAY,
                        "seek_edu_fine_scrubbing_text"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_SEEK_UNDO_MESSAGE,
                        "ADBE Vector Shape"
                )
        );
    }

    private boolean isEveryFilterGroupEnabled() {
        for (ByteArrayFilterGroup group : seekMessageGroupList)
            if (!group.isEnabled()) return false;

        return true;
    }

    @Override
    public boolean isFiltered(String path, String identifier, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        // This identifier is used not only in players but also in search results:
        // https://github.com/ReVanced/revanced-patches/issues/3245
        // Until 2024, medical information panels such as Covid 19 also used this identifier and were shown in the search results.
        // From 2025, the medical information panel is no longer shown in the search results.
        // Therefore, this identifier does not filter when the search bar is activated.
        if (matchedGroup == singleItemInformationPanel) {
            return RootView.isPlayerActive() || !RootView.isSearchBarActive();
        } else if (matchedGroup == suggestedActions) {
            // suggested actions button on shorts and the suggested actions button on video players use the same path builder.
            // Check PlayerType to make each setting work independently.
            return !suggestedActionsException.matches(path) && !PlayerType.getCurrent().isNoneOrHidden();
        } else if (matchedGroup == channelBar) {
            return channelBarGroupList.check(path).isFiltered();
        } else if (matchedGroup == seekMessage) {
            if (seekMessageGroupExceptions.check(buffer).isFiltered()) {
                return false;
            }
            return isEveryFilterGroupEnabled() || seekMessageGroupList.check(buffer).isFiltered();
        }

        return true;
    }
}
