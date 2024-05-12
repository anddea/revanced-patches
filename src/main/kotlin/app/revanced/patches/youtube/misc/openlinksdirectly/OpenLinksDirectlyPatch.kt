package app.revanced.patches.youtube.misc.openlinksdirectly

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patches.youtube.misc.openlinksdirectly.fingerprints.OpenLinksDirectlyFingerprintPrimary
import app.revanced.patches.youtube.misc.openlinksdirectly.fingerprints.OpenLinksDirectlyFingerprintSecondary
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getTargetIndexWithMethodReferenceName
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Suppress("unused")
object OpenLinksDirectlyPatch : BaseBytecodePatch(
    name = "Enable open links directly",
    description = "Adds an option to skip over redirection URLs in external links.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        OpenLinksDirectlyFingerprintPrimary,
        OpenLinksDirectlyFingerprintSecondary
    )
) {
    override fun execute(context: BytecodeContext) {

        arrayOf(
            OpenLinksDirectlyFingerprintPrimary,
            OpenLinksDirectlyFingerprintSecondary
        ).forEach { fingerprint ->
            fingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val insertIndex = getTargetIndexWithMethodReferenceName("parse")
                    val insertRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerC

                    replaceInstruction(
                        insertIndex,
                        "invoke-static {v$insertRegister}, $MISC_PATH/OpenLinksDirectlyPatch;->enableBypassRedirect(Ljava/lang/String;)Landroid/net/Uri;"
                    )
                }
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: ENABLE_OPEN_LINKS_DIRECTLY"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
