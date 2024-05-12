package app.revanced.patches.music.misc.tracking

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object SanitizeUrlQueryPatch : BaseResourcePatch(
    name = "Sanitize sharing links",
    description = "Adds an option to remove tracking query parameters from URLs when sharing links.",
    dependencies = setOf(
        SanitizeUrlQueryBytecodePatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    override fun execute(context: ResourceContext) {

        SettingsPatch.addSwitchPreference(
            CategoryType.MISC,
            "revanced_sanitize_sharing_links",
            "true"
        )

    }
}
