package app.revanced.patches.youtube.utils.returnyoutubedislike.rollingnumber

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.utils.fingerprints.RollingNumberTextViewAnimationUpdateFingerprint
import app.revanced.patches.youtube.utils.fingerprints.RollingNumberTextViewFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.returnyoutubedislike.rollingnumber.fingerprints.RollingNumberMeasureAnimatedTextFingerprint
import app.revanced.patches.youtube.utils.returnyoutubedislike.rollingnumber.fingerprints.RollingNumberMeasureStaticLabelFingerprint
import app.revanced.patches.youtube.utils.returnyoutubedislike.rollingnumber.fingerprints.RollingNumberMeasureTextParentFingerprint
import app.revanced.patches.youtube.utils.returnyoutubedislike.rollingnumber.fingerprints.RollingNumberSetterFingerprint
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.alsoResolve
import app.revanced.util.findMethodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch(dependencies = [SettingsPatch::class])
object ReturnYouTubeDislikeRollingNumberPatch : BytecodePatch(
    setOf(
        RollingNumberSetterFingerprint,
        RollingNumberMeasureTextParentFingerprint,
        RollingNumberTextViewFingerprint,
        RollingNumberMeasureAnimatedTextFingerprint
    )
) {
    private const val INTEGRATIONS_RYD_CLASS_DESCRIPTOR =
        "$UTILS_PATH/ReturnYouTubeDislikePatch;"

    override fun execute(context: BytecodeContext) {
        /**
         * RollingNumber is applied to YouTube v18.49.37+.
         *
         * In order to maintain compatibility with YouTube v18.48.39 or previous versions,
         * This patch is applied only to the version after YouTube v18.49.37
         */
        if (!SettingsPatch.upward1849) {
            return
        }

        RollingNumberSetterFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val rollingNumberClassIndex = it.scanResult.patternScanResult!!.startIndex
                val rollingNumberClassReference =
                    getInstruction<ReferenceInstruction>(rollingNumberClassIndex).reference.toString()
                val rollingNumberConstructorMethod =
                    context.findMethodOrThrow(rollingNumberClassReference)
                val charSequenceFieldReference = with(rollingNumberConstructorMethod) {
                    getInstruction<ReferenceInstruction>(
                        indexOfFirstInstructionOrThrow(Opcode.IPUT_OBJECT)
                    ).reference
                }

                val insertIndex = rollingNumberClassIndex + 1
                val charSequenceInstanceRegister =
                    getInstruction<OneRegisterInstruction>(rollingNumberClassIndex).registerA
                val registerCount = implementation!!.registerCount

                // This register is being overwritten, so it is free to use.
                val freeRegister = registerCount - 1
                val conversionContextRegister = registerCount - parameters.size + 1

                addInstructions(
                    insertIndex, """
                        iget-object v$freeRegister, v$charSequenceInstanceRegister, $charSequenceFieldReference
                        invoke-static {v$conversionContextRegister, v$freeRegister}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->onRollingNumberLoaded(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$freeRegister
                        iput-object v$freeRegister, v$charSequenceInstanceRegister, $charSequenceFieldReference
                        """
                )
            }
        }

        // Rolling Number text views use the measured width of the raw string for layout.
        // Modify the measure text calculation to include the left drawable separator if needed.
        RollingNumberMeasureAnimatedTextFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val endIndex = it.scanResult.patternScanResult!!.endIndex
                val measuredTextWidthIndex = endIndex - 2
                val measuredTextWidthRegister =
                    getInstruction<TwoRegisterInstruction>(measuredTextWidthIndex).registerA

                addInstructions(
                    endIndex + 1, """
                        invoke-static {p1, v$measuredTextWidthRegister}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->onRollingNumberMeasured(Ljava/lang/String;F)F
                        move-result v$measuredTextWidthRegister
                        """
                )

                val ifGeIndex = indexOfFirstInstructionOrThrow(opcode = Opcode.IF_GE)
                val ifGeInstruction = getInstruction<TwoRegisterInstruction>(ifGeIndex)

                removeInstruction(ifGeIndex)
                addInstructionsWithLabels(
                    ifGeIndex, """
                        if-ge v${ifGeInstruction.registerA}, v${ifGeInstruction.registerB}, :jump
                        """, ExternalLabel("jump", getInstruction(endIndex))
                )
            }
        }

        RollingNumberMeasureStaticLabelFingerprint.alsoResolve(
            context,
            RollingNumberMeasureTextParentFingerprint
        ).let {
            it.mutableMethod.apply {
                val measureTextIndex = it.scanResult.patternScanResult!!.startIndex + 1
                val freeRegister = getInstruction<TwoRegisterInstruction>(0).registerA

                addInstructions(
                    measureTextIndex + 1, """
                        move-result v$freeRegister
                        invoke-static {p1, v$freeRegister}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->onRollingNumberMeasured(Ljava/lang/String;F)F
                        """
                )
            }
        }

        // The rolling number Span is missing styling since it's initially set as a String.
        // Modify the UI text view and use the styled like/dislike Span.
        arrayOf(
            // Initial TextView is set in this method.
            RollingNumberTextViewFingerprint
                .resultOrThrow(),

            // Video less than 24 hours after uploaded, like counts will be updated in real time.
            // Whenever like counts are updated, TextView is set in this method.
            RollingNumberTextViewAnimationUpdateFingerprint
                .alsoResolve(context, RollingNumberTextViewFingerprint)
        ).forEach { fingerprintResult ->
            fingerprintResult.mutableMethod.apply {
                val setTextIndex = indexOfFirstInstructionOrThrow {
                    getReference<MethodReference>()?.name == "setText"
                }
                val textViewRegister =
                    getInstruction<FiveRegisterInstruction>(setTextIndex).registerC
                val textSpanRegister =
                    getInstruction<FiveRegisterInstruction>(setTextIndex).registerD

                addInstructions(
                    setTextIndex, """
                        invoke-static {v$textViewRegister, v$textSpanRegister}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->updateRollingNumber(Landroid/widget/TextView;Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
                        move-result-object v$textSpanRegister
                        """
                )
            }
        }
    }
}
