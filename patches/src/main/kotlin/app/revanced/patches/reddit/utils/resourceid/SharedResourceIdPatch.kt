package app.revanced.patches.reddit.utils.resourceid

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.shared.mapping.ResourceType.STRING
import app.revanced.patches.shared.mapping.get
import app.revanced.patches.shared.mapping.resourceMappingPatch
import app.revanced.patches.shared.mapping.resourceMappings

var screenShotShareBanner = -1L
    private set

internal val sharedResourceIdPatch = resourcePatch(
    description = "sharedResourceIdPatch"
) {
    dependsOn(resourceMappingPatch)

    execute {
        screenShotShareBanner = resourceMappings[
            STRING,
            "screenshot_share_banner_title",
        ]
    }
}