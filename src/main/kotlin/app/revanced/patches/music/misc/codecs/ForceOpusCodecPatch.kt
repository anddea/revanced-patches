package app.revanced.patches.music.misc.codecs

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseResourcePatch


@Suppress("unused")
object ForceOpusCodecPatch : BaseResourcePatch(
    name = "Enable opus codec",
    description = "Adds an option use the opus audio codec instead of the mp4a audio codec.",
    dependencies = setOf(
        ForceOpusCodecBytecodePatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    override fun execute(context: ResourceContext) {

        SettingsPatch.addSwitchPreference(
            CategoryType.MISC,
            "revanced_enable_opus_codec",
            "true"
        )

    }
}
