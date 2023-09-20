package app.revanced.patches.youtube.seekbar.append.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.fingerprints.TotalTimeFingerprint
import app.revanced.patches.youtube.utils.overridequality.patch.OverrideQualityHookPatch
import app.revanced.patches.youtube.utils.overridespeed.patch.OverrideSpeedHookPatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.SEEKBAR
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c

@Patch
@Name("Append time stamps information")
@Description("Add the current video quality or playback speed in brackets next to the current time.")
@DependsOn(
    [
        OverrideQualityHookPatch::class,
        OverrideSpeedHookPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
class AppendTimeStampInformationPatch : BytecodePatch(
    listOf(TotalTimeFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        TotalTimeFingerprint.result?.let {
            it.mutableMethod.apply {
                var setTextIndex = -1

                for ((textViewIndex, textViewInstruction) in implementation!!.instructions.withIndex()) {
                    if (textViewInstruction.opcode != Opcode.INVOKE_VIRTUAL) continue

                    if (getInstruction<ReferenceInstruction>(textViewIndex).reference.toString() ==
                        "Landroid/widget/TextView;->getText()Ljava/lang/CharSequence;"
                    ) {
                        setTextIndex = textViewIndex + 2
                        val setTextRegister = getInstruction<Instruction35c>(setTextIndex).registerC
                        val textViewRegister = getInstruction<Instruction35c>(textViewIndex).registerC

                        addInstructions(
                            setTextIndex, """
                                invoke-static {v$setTextRegister}, $SEEKBAR->appendTimeStampInformation(Ljava/lang/String;)Ljava/lang/String;
                                move-result-object v$setTextRegister
                                """
                        )
                        addInstruction(
                            textViewIndex,
                            "invoke-static {v$textViewRegister}, $SEEKBAR->setContainerClickListener(Landroid/view/View;)V"
                        )
                        break
                    }
                }
                if (setTextIndex == -1)
                    throw PatchException("target Instruction not found!")
            }
        } ?: throw TotalTimeFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: SEEKBAR_SETTINGS",
                "SETTINGS: APPEND_TIME_STAMP_INFORMATION"
            )
        )

        SettingsPatch.updatePatchStatus("append-timestamps-information")

    }
}
