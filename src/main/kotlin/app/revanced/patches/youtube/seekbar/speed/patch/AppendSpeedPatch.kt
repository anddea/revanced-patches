package app.revanced.patches.youtube.seekbar.speed.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.fingerprints.TotalTimeFingerprint
import app.revanced.patches.youtube.utils.overridespeed.patch.OverrideSpeedHookPatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.SEEKBAR
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c

@Patch
@Name("Enable time stamps speed")
@Description("Add the current playback speed in brackets next to the current time.")
@DependsOn(
    [
        OverrideSpeedHookPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
class AppendSpeedPatch : BytecodePatch(
    listOf(TotalTimeFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        TotalTimeFingerprint.result?.let {
            it.mutableMethod.apply {
                var insertIndex = -1

                for ((targetIndex, targetInstruction) in implementation!!.instructions.withIndex()) {
                    if (targetInstruction.opcode != Opcode.INVOKE_VIRTUAL) continue

                    if (getInstruction<ReferenceInstruction>(targetIndex).reference.toString() ==
                        "Landroid/widget/TextView;->getText()Ljava/lang/CharSequence;"
                    ) {
                        insertIndex = targetIndex + 2
                        val insertRegister = getInstruction<Instruction35c>(insertIndex).registerC

                        addInstructions(
                            insertIndex, """
                                invoke-static {v$insertRegister}, $SEEKBAR->enableTimeStampSpeed(Ljava/lang/String;)Ljava/lang/String;
                                move-result-object v$insertRegister
                                """
                        )
                        break
                    }
                }
                if (insertIndex == -1)
                    throw PatchException("target Instruction not found!")
            }
        } ?: throw TotalTimeFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: SEEKBAR_SETTINGS",
                "SETTINGS: ENABLE_TIME_STAMP_SPEED"
            )
        )

        SettingsPatch.updatePatchStatus("enable-timestamps-speed")

    }
}
