package app.revanced.patches.music.general.landscapemode

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.music.general.landscapemode.fingerprints.TabletIdentifierFingerprint
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object LandScapeModePatch : BaseBytecodePatch(
    name = "Enable landscape mode",
    description = "Adds an option to enable landscape mode when rotating the screen on phones.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(TabletIdentifierFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        TabletIdentifierFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$targetRegister}, $GENERAL_CLASS_DESCRIPTOR->enableLandScapeMode(Z)Z
                        move-result v$targetRegister
                        """
                )
            }
        }

        SettingsPatch.addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_enable_landscape_mode",
            "true"
        )

    }
}
