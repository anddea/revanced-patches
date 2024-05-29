package app.revanced.patches.youtube.layout.animated

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.patch.BaseResourcePatch


@Suppress("unused")
object AnimatedLikePatch : BaseResourcePatch(
    name = "Hide double tap to like animations",
    description = "Removes, at compile time, the like animations that appear when double-tapping in the Shorts player.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    use = false
) {
    override fun execute(context: ResourceContext) {
        /**
         * Copy json
         */
        context.copyResources(
            "youtube/animated",
            ResourceGroup(
                "raw",
                "like_tap_feedback.json"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}