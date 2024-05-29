package app.revanced.patches.music.general.startpage

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patches.music.general.startpage.fingerprints.ColdStartUpFingerprint
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object ChangeStartPagePatch : BaseBytecodePatch(
    name = "Change start page",
    description = "Adds an option to set which page the app opens in instead of the homepage.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(ColdStartUpFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        ColdStartUpFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$targetRegister}, $GENERAL_CLASS_DESCRIPTOR->changeStartPage(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$targetRegister
                        return-object v$targetRegister
                        """
                )
                removeInstruction(targetIndex)
            }
        }

        SettingsPatch.addPreferenceWithIntent(
            CategoryType.GENERAL,
            "revanced_change_start_page"
        )

    }
}
