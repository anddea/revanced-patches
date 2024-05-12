package app.revanced.patches.youtube.general.startpage

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patches.youtube.general.startpage.fingerprints.StartActivityFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow

@Suppress("unused")
object ChangeStartPagePatch : BaseBytecodePatch(
    name = "Change start page",
    description = "Adds an option to set which page the app opens in instead of the homepage.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(StartActivityFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        StartActivityFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                addInstruction(
                    0,
                    "invoke-static { p1 }, $GENERAL_CLASS_DESCRIPTOR->changeStartPage(Landroid/content/Intent;)V"
                )
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: CHANGE_START_PAGE"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
