package app.revanced.patches.youtube.misc.codecs

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object OpusCodecPatch : BaseResourcePatch(
    name = "Enable OPUS codec",
    description = "Adds an options to enable the OPUS audio codec if the player response includes.",
    dependencies = setOf(
        OpusCodecBytecodePatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    override fun execute(context: ResourceContext) {

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_CATEGORY: MISC_EXPERIMENTAL_FLAGS",
                "SETTINGS: ENABLE_OPUS_CODEC"
            )
        )

        SettingsPatch.updatePatchStatus(this)

    }
}
