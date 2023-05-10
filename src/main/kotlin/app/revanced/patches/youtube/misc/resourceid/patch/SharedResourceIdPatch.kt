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
        var accessibilityProgressTimeId: Long = -1
        var accessibilityVideoTimeId: Long = -1
        var accountSwitcherAccessibilityId: Long = -1
        var adAttributionId: Long = -1
        var appearanceStringId: Long = -1
        var autoNavPreviewId: Long = -1
        var backgroundCategoryId: Long = -1
        var barContainerHeightId: Long = -1
        var bottomUiContainerResourceId: Long = -1
        var channelListSubMenuId: Long = -1
        var chapterRepeatOnResourceId: Long = -1
        var compactLinkId: Long = -1
        var controlsLayoutStubResourceId: Long = -1
        var dislikeButtonId: Long = -1
        var donationCompanionResourceId: Long = -1
        var easySeekEduContainerId: Long = -1
        var expandButtonId: Long = -1
        var fabId: Long = -1
        var filterBarHeightId: Long = -1
        var floatyBarTopMarginId: Long = -1
        var horizontalCardListId: Long = -1
        var imageOnlyTabId: Long = -1
        var inlineTimeBarColorizedBarPlayedColorDarkId: Long = -1
        var inlineTimeBarPlayedNotHighlightedColorId: Long = -1
        var layoutCircleId: Long = -1
        var layoutIconId: Long = -1
        var layoutVideoId: Long = -1
        var likeButtonId: Long = -1
        var liveChatButtonId: Long = -1
        var reelPlayerBadgeId: Long = -1
        var reelPlayerBadge2Id: Long = -1
        var reelPlayerFooterId: Long = -1
        var reelPlayerInfoPanelId: Long = -1
        var reelPlayerPausedId: Long = -1
        var reelRemixId: Long = -1
        var relatedChipCloudMarginId: Long = -1
        var rightCommentId: Long = -1
        var searchSuggestionEntryId: Long = -1
        var scrubbingId: Long = -1
        var slimMetadataToggleButtonId: Long = -1
        var toolTipId: Long = -1
        var videoQualityFragmentId: Long = -1
    }

    override fun execute(context: ResourceContext): PatchResult {

        fun find(type: ResourceType, name: String) = ResourceMappingPatch
            .resourceMappings
            .single { it.type == type.value && it.name == name }.id

        accessibilityProgressTimeId = find(STRING, "accessibility_player_progress_time")
        accessibilityVideoTimeId = find(STRING, "accessibility_video_time")
        accountSwitcherAccessibilityId = find(STRING, "account_switcher_accessibility_label")
        adAttributionId = find(ID, "ad_attribution")
        appearanceStringId = find(STRING, "app_theme_appearance_dark")
        autoNavPreviewId = find(ID, "autonav_preview_stub")
        backgroundCategoryId = find(STRING, "pref_background_and_offline_category")
        barContainerHeightId = find(DIMEN, "bar_container_height")
        bottomUiContainerResourceId = find(ID, "bottom_ui_container_stub")
        channelListSubMenuId = find(LAYOUT, "channel_list_sub_menu")
        chapterRepeatOnResourceId = find(STRING, "chapter_repeat_on")
        compactLinkId = find(LAYOUT, "compact_link")
        controlsLayoutStubResourceId = find(ID, "controls_layout_stub")
        dislikeButtonId = find(ID, "dislike_button")
        donationCompanionResourceId = find(LAYOUT, "donation_companion")
        easySeekEduContainerId = find(ID, "easy_seek_edu_container")
        expandButtonId = find(LAYOUT, "expand_button_down")
        fabId = find(ID, "fab")
        filterBarHeightId = find(DIMEN, "filter_bar_height")
        floatyBarTopMarginId = find(DIMEN, "floaty_bar_button_top_margin")
        horizontalCardListId = find(LAYOUT, "horizontal_card_list")
        imageOnlyTabId = find(LAYOUT, "image_only_tab")
        inlineTimeBarColorizedBarPlayedColorDarkId = find(COLOR, "inline_time_bar_colorized_bar_played_color_dark")
        inlineTimeBarPlayedNotHighlightedColorId = find(COLOR, "inline_time_bar_played_not_highlighted_color")
        layoutCircleId = find(LAYOUT, "endscreen_element_layout_circle")
        layoutIconId = find(LAYOUT, "endscreen_element_layout_icon")
        layoutVideoId = find(LAYOUT, "endscreen_element_layout_video")
        likeButtonId = find(ID, "like_button")
        liveChatButtonId = find(ID, "live_chat_overlay_button")
        reelPlayerBadgeId = find(ID, "reel_player_badge")
        reelPlayerBadge2Id = find(ID, "reel_player_badge2")
        reelPlayerFooterId = find(LAYOUT, "reel_player_dyn_footer_vert_stories3")
        reelPlayerInfoPanelId = find(ID, "reel_player_info_panel")
        reelPlayerPausedId = find(ID, "reel_player_paused_state_buttons")
        reelRemixId = find(ID, "reel_dyn_remix")
        relatedChipCloudMarginId = find(LAYOUT, "related_chip_cloud_reduced_margins")
        rightCommentId = find(DRAWABLE, "ic_right_comment_32c")
        searchSuggestionEntryId = find(LAYOUT, "search_suggestion_entry")
        scrubbingId = find(DIMEN, "vertical_touch_offset_to_enter_fine_scrubbing")
        slimMetadataToggleButtonId = find(COLOR, "slim_metadata_toggle_button")
        toolTipId = find(LAYOUT, "tooltip_content_view")
        videoQualityFragmentId = find(LAYOUT, "video_quality_bottom_sheet_list_fragment_title")

        return PatchResultSuccess()
    }
}