package app.revanced.patches.youtube.seekbar.append

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.fingerprints.TotalTimeFingerprint
import app.revanced.patches.youtube.utils.overridequality.OverrideQualityHookPatch
import app.revanced.patches.youtube.utils.overridespeed.OverrideSpeedHookPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.integrations.Constants.SEEKBAR
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c

@Patch(
    name = "Append time stamps information",
    description = "Add the current video quality or playback speed in brackets next to the current time.",
    dependencies = [
        OverrideQualityHookPatch::class,
        OverrideSpeedHookPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.22.37",
                "18.23.36",
                "18.24.37",
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39"
            ]
        )
    ]
)
@Suppress("unused")
object AppendTimeStampInformationPatch : BytecodePatch(
    setOf(TotalTimeFingerprint)
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
                        val textViewRegister =
                            getInstruction<Instruction35c>(textViewIndex).registerC

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
