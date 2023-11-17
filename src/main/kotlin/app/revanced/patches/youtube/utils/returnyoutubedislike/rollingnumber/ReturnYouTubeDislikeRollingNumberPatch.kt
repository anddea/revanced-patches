package app.revanced.patches.youtube.utils.returnyoutubedislike.rollingnumber

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.returnyoutubedislike.rollingnumber.fingerprints.RollingNumberSetterFingerprint
import app.revanced.patches.youtube.utils.returnyoutubedislike.rollingnumber.fingerprints.RollingNumberTextViewFingerprint
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.integrations.Constants.UTILS_PATH
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference

@Patch(dependencies = [SettingsPatch::class])
object ReturnYouTubeDislikeRollingNumberPatch : BytecodePatch(
    setOf(
        RollingNumberSetterFingerprint,
        RollingNumberTextViewFingerprint
    )
) {
    private const val CONVERSION_CONTEXT_PARAMETER = 2

    private const val INTEGRATIONS_RYD_CLASS_DESCRIPTOR =
        "$UTILS_PATH/ReturnYouTubeDislikePatch;"

    override fun execute(context: BytecodeContext) {
        /**
         * RollingNumber is applied to YouTube v18.41.39+.
         *
         * In order to maintain compatibility with YouTube v18.40.34 or previous versions,
         * This patch is applied only to the version after YouTube v18.41.39
         *
         * Resolves following issue:
         * https://github.com/revanced/revanced-patches/issues/2904
         */
        if (SettingsPatch.upward1841) {

            RollingNumberSetterFingerprint.result?.let {
                it.mutableMethod.apply {
                    val rollingNumberClassIndex = it.scanResult.patternScanResult!!.startIndex
                    val rollingNumberClassReference =
                        getInstruction<BuilderInstruction21c>(rollingNumberClassIndex).reference
                    val rollingNumberClass =
                        context.findClass(rollingNumberClassReference.toString())!!.mutableClass

                    lateinit var charSequenceFieldReference: Reference

                    rollingNumberClass.methods.find { method -> method.name == "<init>" }
                        ?.apply {
                            val rollingNumberFieldIndex =
                                implementation!!.instructions.indexOfFirst { instruction ->
                                    instruction.opcode == Opcode.IPUT_OBJECT
                                }
                            charSequenceFieldReference = getInstruction<ReferenceInstruction>(rollingNumberFieldIndex).reference
                        } ?: throw PatchException("RollingNumberClass not found!")

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
            } ?: throw RollingNumberSetterFingerprint.exception

            // The rolling number Span is missing styling since it's initially set as a String.
            // Modify the UI text view and use the styled like/dislike Span.
            RollingNumberTextViewFingerprint.result?.let {
                // Initial TextView is set in this method.
                val initiallyCreatedTextViewMethod = it.mutableMethod

                // Video less than 24 hours after uploaded, like counts will be updated in real time.
                // Whenever like counts are updated, TextView is set in this method.
                val realTimeUpdateTextViewMethod = it.mutableClass.methods.find { method ->
                    method.parameterTypes.first() == "Landroid/graphics/Bitmap;"
                } ?: throw PatchException("Failed to find realTimeUpdateTextViewMethod")

                arrayOf(
                    initiallyCreatedTextViewMethod,
                    realTimeUpdateTextViewMethod
                ).forEach { insertMethod ->
                    insertMethod.apply {
                        val setTextIndex =
                            implementation!!.instructions.indexOfFirst { instruction ->
                                ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.name == "setText"
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
            } ?: throw RollingNumberTextViewFingerprint.exception
        }
    }
}
