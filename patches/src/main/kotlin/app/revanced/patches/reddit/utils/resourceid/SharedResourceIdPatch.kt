package app.revanced.patches.reddit.utils.resourceid

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.shared.mapping.ResourceType.STRING
import app.revanced.patches.shared.mapping.getResourceId
import app.revanced.patches.shared.mapping.resourceMappingPatch

var nsfwDialogTitle = -1L
    private set

internal val sharedResourceIdPatch = resourcePatch(
    description = "sharedResourceIdPatch"
) {
    dependsOn(resourceMappingPatch)

    execute {
        nsfwDialogTitle = getResourceId(STRING, "nsfw_dialog_title")
    }
}