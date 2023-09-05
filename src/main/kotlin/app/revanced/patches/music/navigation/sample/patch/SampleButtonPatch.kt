package app.revanced.patches.music.navigation.sample.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.navigation.sample.fingerprints.PivotBarConstructorFingerprint
import app.revanced.patches.music.navigation.upgrade.patch.UpgradeButtonPatch
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_NAVIGATION
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch
@Name("Hide sample buttons")
@Description("Adds options to hide sample buttons.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class,
        UpgradeButtonPatch::class
    ]
)
@MusicCompatibility
class SampleButtonPatch : BytecodePatch(
    listOf(PivotBarConstructorFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        PivotBarConstructorFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex
                val targetRegisterA = getInstruction<TwoRegisterInstruction>(targetIndex).registerA
                val targetRegisterB = getInstruction<TwoRegisterInstruction>(targetIndex).registerB

                val replaceReference =
                    getInstruction<ReferenceInstruction>(targetIndex).reference.toString()

                replaceInstruction(
                    targetIndex,
                    "invoke-static {v$targetRegisterA}, $MUSIC_NAVIGATION->hideSampleButton(Ljava/util/List;)V"
                )
                addInstruction(
                    targetIndex + 1,
                    "iput-object v$targetRegisterA, v$targetRegisterB, $replaceReference"
                )
            }
        } ?: throw PivotBarConstructorFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_sample_button",
            "false"
        )

    }
}
