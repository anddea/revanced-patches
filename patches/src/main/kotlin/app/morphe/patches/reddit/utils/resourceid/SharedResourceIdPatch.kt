package app.morphe.patches.reddit.utils.resourceid

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.mapping.ResourceType.STRING
import app.morphe.patches.shared.mapping.getResourceId
import app.morphe.patches.shared.mapping.resourceMappingPatch

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
