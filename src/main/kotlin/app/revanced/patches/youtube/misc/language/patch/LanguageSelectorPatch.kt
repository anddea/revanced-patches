package app.revanced.patches.youtube.misc.language.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.misc.language.fingerprints.GeneralPrefsFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("Language switch")
@Description("Add language switch toggle.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
class LanguageSelectorPatch : BytecodePatch(
    listOf(GeneralPrefsFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        GeneralPrefsFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex + 1
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex,
                    "const/4 v$targetRegister, 0x1"
                )
            }
        } ?: throw GeneralPrefsFingerprint.exception

        SettingsPatch.updatePatchStatus("language-switch")

    }
}
