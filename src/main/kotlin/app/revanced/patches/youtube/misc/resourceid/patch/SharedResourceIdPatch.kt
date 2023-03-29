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
        var chapterRepeatOnResourceId: Long = -1
        var compactLinkLabelId: Long = -1
        var controlsLayoutStubResourceId: Long = -1
        var donationCompanionResourceId: Long = -1
        var emptyColorLabelId: Long = -1
        var fabLabelId: Long = -1
        var filterBarHeightLabelId: Long = -1
        var floatyBarQueueLabelId: Long = -1
        var imageOnlyTabId: Long = -1
        var imageWithTextTabId: Long = -1
        var layoutCircle: Long = -1
        var layoutIcon: Long = -1
        var layoutVideo: Long = -1
        var liveChatButtonId: Long = -1
        var reelPlayerFooterLabelId: Long = -1
        var relatedChipCloudMarginLabelId: Long = -1
        var scrubbingLabelId: Long = -1
        var timeStampsContainerLabelId: Long = -1
        var tooltipLabelId: Long = -1
        var videoQualityFragmentLabelId: Long = -1
    }

    override fun execute(context: ResourceContext): PatchResult {

        fun findSharedResourceId(type: String, name: String) = ResourceMappingPatch
            .resourceMappings
            .single { it.type == type && it.name == name }.id

        accessibilityProgressTimeLabelId = findSharedResourceId("string", "accessibility_player_progress_time")
        accountSwitcherAccessibilityLabelId = findSharedResourceId("string", "account_switcher_accessibility_label")
        appearanceStringId = findSharedResourceId("string", "app_theme_appearance_dark")
        backgroundCategoryLabelId = findSharedResourceId("string", "pref_background_and_offline_category")
        barContainerHeightLabelId = findSharedResourceId("dimen", "bar_container_height")
        bottomUiContainerResourceId = findSharedResourceId("id", "bottom_ui_container_stub")
        chapterRepeatOnResourceId = findSharedResourceId("string", "chapter_repeat_on")
        compactLinkLabelId = findSharedResourceId("layout", "compact_link")
        controlsLayoutStubResourceId = findSharedResourceId("id", "controls_layout_stub")
        donationCompanionResourceId = findSharedResourceId("layout", "donation_companion")
        emptyColorLabelId = findSharedResourceId("color", "inline_time_bar_colorized_bar_empty_color_dark")
        fabLabelId = findSharedResourceId("id", "fab")
        filterBarHeightLabelId = findSharedResourceId("dimen", "filter_bar_height")
        floatyBarQueueLabelId = findSharedResourceId("string", "floaty_bar_queue_status")
        imageOnlyTabId = findSharedResourceId("layout", "image_only_tab")
        imageWithTextTabId = findSharedResourceId("layout", "image_with_text_tab")
        layoutCircle = findSharedResourceId("layout", "endscreen_element_layout_circle")
        layoutIcon = findSharedResourceId("layout", "endscreen_element_layout_icon")
        layoutVideo = findSharedResourceId("layout", "endscreen_element_layout_video")
        liveChatButtonId = findSharedResourceId("id", "live_chat_overlay_button")
        reelPlayerFooterLabelId = findSharedResourceId("layout", "reel_player_dyn_footer_vert_stories3")
        relatedChipCloudMarginLabelId = findSharedResourceId("layout", "related_chip_cloud_reduced_margins")
        scrubbingLabelId = findSharedResourceId("dimen", "vertical_touch_offset_to_enter_fine_scrubbing")
        timeStampsContainerLabelId = findSharedResourceId("id", "timestamps_container")
        tooltipLabelId = findSharedResourceId("layout", "tooltip_content_view")
        videoQualityFragmentLabelId = findSharedResourceId("layout", "video_quality_bottom_sheet_list_fragment_title")

        return PatchResultSuccess()
    }
}