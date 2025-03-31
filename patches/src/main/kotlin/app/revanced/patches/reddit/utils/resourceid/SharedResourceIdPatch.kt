package app.revanced.patches.reddit.utils.resourceid

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.shared.mapping.ResourceType.STRING
import app.revanced.patches.shared.mapping.getResourceId
import app.revanced.patches.shared.mapping.resourceMappingPatch

var actionShare = -1L
    private set
var nsfwDialogTitle = -1L
    private set
var screenShotShareBanner = -1L
    private set

internal val sharedResourceIdPatch = resourcePatch(
    description = "sharedResourceIdPatch"
) {
    dependsOn(resourceMappingPatch)

    execute {
        actionShare = getResourceId(STRING, "action_share")
        nsfwDialogTitle = getResourceId(STRING, "nsfw_dialog_title")
        screenShotShareBanner = getResourceId(STRING, "screenshot_share_banner_title")
    }
}