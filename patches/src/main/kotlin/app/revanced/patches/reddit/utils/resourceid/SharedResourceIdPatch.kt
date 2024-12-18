package app.revanced.patches.reddit.utils.resourceid

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.shared.mapping.ResourceType.ID
import app.revanced.patches.shared.mapping.ResourceType.STRING
import app.revanced.patches.shared.mapping.ResourceType.STYLE
import app.revanced.patches.shared.mapping.get
import app.revanced.patches.shared.mapping.resourceMappingPatch
import app.revanced.patches.shared.mapping.resourceMappings

var cancelButton = -1L
    private set
var labelAcknowledgements = -1L
    private set
var screenShotShareBanner = -1L
    private set
var textAppearanceRedditBaseOldButtonColored = -1L
    private set
var toolBarNavSearchCtaContainer = -1L
    private set

internal val sharedResourceIdPatch = resourcePatch(
    description = "sharedResourceIdPatch"
) {
    dependsOn(resourceMappingPatch)

    execute {
        cancelButton = resourceMappings[
            ID,
            "cancel_button",
        ]
        labelAcknowledgements = resourceMappings[
            STRING,
            "label_acknowledgements"
        ]
        screenShotShareBanner = resourceMappings[
            STRING,
            "screenshot_share_banner_title"
        ]
        textAppearanceRedditBaseOldButtonColored = resourceMappings[
            STYLE,
            "TextAppearance.RedditBase.OldButton.Colored"
        ]
        toolBarNavSearchCtaContainer = resourceMappings[
            ID,
            "toolbar_nav_search_cta_container"
        ]
    }
}