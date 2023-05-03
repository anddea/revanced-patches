package app.revanced.patches.youtube.misc.resourceid.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.patch.mapping.ResourceMappingPatch
import app.revanced.util.enum.ResourceType
import app.revanced.util.enum.ResourceType.*

@Name("shared-resource-id")
@DependsOn([ResourceMappingPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class SharedResourceIdPatch : ResourcePatch {
    internal companion object {
        var accessibilityProgressTimeLabelId: Long = -1
        var accountSwitcherAccessibilityLabelId: Long = -1
        var appearanceStringId: Long = -1
        var backgroundCategoryLabelId: Long = -1
        var barContainerHeightLabelId: Long = -1
        var bottomUiContainerResourceId: Long = -1
        var channelListSubMenuLabelId: Long = -1
        var chapterRepeatOnResourceId: Long = -1
        var compactLinkLabelId: Long = -1
        var controlsLayoutStubResourceId: Long = -1
        var dislikeButtonLabelId: Long = -1
        var donationCompanionResourceId: Long = -1
        var fabLabelId: Long = -1
        var filterBarHeightLabelId: Long = -1
        var floatyBarTopMarginLabelId: Long = -1
        var imageOnlyTabId: Long = -1
        var layoutCircle: Long = -1
        var layoutIcon: Long = -1
        var layoutVideo: Long = -1
        var likeButtonLabelId: Long = -1
        var liveChatButtonId: Long = -1
        var reelPlayerBadgeLabelId: Long = -1
        var reelPlayerBadge2LabelId: Long = -1
        var reelPlayerFooterLabelId: Long = -1
        var reelPlayerInfoPanelLabelId: Long = -1
        var reelPlayerPausedLabelId: Long = -1
        var reelRemixLabelId: Long = -1
        var relatedChipCloudMarginLabelId: Long = -1
        var rightCommentLabelId: Long = -1
        var searchSuggestionEntryLabelId: Long = -1
        var scrubbingLabelId: Long = -1
        var slimMetadataToggleButtonLabelId: Long = -1
        var timeBarPlayedDarkLabelId: Long = -1
        var tooltipLabelId: Long = -1
        var videoQualityFragmentLabelId: Long = -1
    }

    override fun execute(context: ResourceContext): PatchResult {

        fun find(type: ResourceType, name: String) = ResourceMappingPatch
            .resourceMappings
            .single { it.type == type.value && it.name == name }.id

        accessibilityProgressTimeLabelId = find(STRING, "accessibility_player_progress_time")
        accountSwitcherAccessibilityLabelId = find(STRING, "account_switcher_accessibility_label")
        appearanceStringId = find(STRING, "app_theme_appearance_dark")
        backgroundCategoryLabelId = find(STRING, "pref_background_and_offline_category")
        barContainerHeightLabelId = find(DIMEN, "bar_container_height")
        bottomUiContainerResourceId = find(ID, "bottom_ui_container_stub")
        channelListSubMenuLabelId = find(LAYOUT, "channel_list_sub_menu")
        chapterRepeatOnResourceId = find(STRING, "chapter_repeat_on")
        compactLinkLabelId = find(LAYOUT, "compact_link")
        controlsLayoutStubResourceId = find(ID, "controls_layout_stub")
        dislikeButtonLabelId = find(ID, "dislike_button")
        donationCompanionResourceId = find(LAYOUT, "donation_companion")
        fabLabelId = find(ID, "fab")
        filterBarHeightLabelId = find(DIMEN, "filter_bar_height")
        floatyBarTopMarginLabelId = find(DIMEN, "floaty_bar_button_top_margin")
        imageOnlyTabId = find(LAYOUT, "image_only_tab")
        layoutCircle = find(LAYOUT, "endscreen_element_layout_circle")
        layoutIcon = find(LAYOUT, "endscreen_element_layout_icon")
        layoutVideo = find(LAYOUT, "endscreen_element_layout_video")
        likeButtonLabelId = find(ID, "like_button")
        liveChatButtonId = find(ID, "live_chat_overlay_button")
        reelPlayerBadgeLabelId = find(ID, "reel_player_badge")
        reelPlayerBadge2LabelId = find(ID, "reel_player_badge2")
        reelPlayerFooterLabelId = find(LAYOUT, "reel_player_dyn_footer_vert_stories3")
        reelPlayerInfoPanelLabelId = find(ID, "reel_player_info_panel")
        reelPlayerPausedLabelId = find(ID, "reel_player_paused_state_buttons")
        reelRemixLabelId = find(ID, "reel_dyn_remix")
        relatedChipCloudMarginLabelId = find(LAYOUT, "related_chip_cloud_reduced_margins")
        rightCommentLabelId = find(DRAWABLE, "ic_right_comment_32c")
        searchSuggestionEntryLabelId = find(LAYOUT, "search_suggestion_entry")
        scrubbingLabelId = find(DIMEN, "vertical_touch_offset_to_enter_fine_scrubbing")
        slimMetadataToggleButtonLabelId = find(COLOR, "slim_metadata_toggle_button")
        timeBarPlayedDarkLabelId = find(COLOR, "inline_time_bar_colorized_bar_played_color_dark")
        tooltipLabelId = find(LAYOUT, "tooltip_content_view")
        videoQualityFragmentLabelId = find(LAYOUT, "video_quality_bottom_sheet_list_fragment_title")

        return PatchResultSuccess()
    }
}