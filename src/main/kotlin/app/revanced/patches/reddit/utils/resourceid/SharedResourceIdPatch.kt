package app.revanced.patches.reddit.utils.resourceid

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.shared.mapping.ResourceMappingPatch
import app.revanced.patches.shared.mapping.ResourceMappingPatch.getId
import app.revanced.patches.shared.mapping.ResourceType.ID
import app.revanced.patches.shared.mapping.ResourceType.STRING
import app.revanced.patches.shared.mapping.ResourceType.STYLE

@Patch(dependencies = [ResourceMappingPatch::class])
object SharedResourceIdPatch : ResourcePatch() {
    var CancelButton = -1L
    var LabelAcknowledgements = -1L
    var ScreenShotShareBanner = -1L
    var TextAppearanceRedditBaseOldButtonColored = -1L
    var ToolBarNavSearchCtaContainer = -1L

    override fun execute(context: ResourceContext) {

        CancelButton = getId(ID, "cancel_button")
        LabelAcknowledgements = getId(STRING, "label_acknowledgements")
        ScreenShotShareBanner = getId(STRING, "screenshot_share_banner_title")
        TextAppearanceRedditBaseOldButtonColored =
            getId(STYLE, "TextAppearance.RedditBase.OldButton.Colored")
        ToolBarNavSearchCtaContainer = getId(ID, "toolbar_nav_search_cta_container")

    }
}