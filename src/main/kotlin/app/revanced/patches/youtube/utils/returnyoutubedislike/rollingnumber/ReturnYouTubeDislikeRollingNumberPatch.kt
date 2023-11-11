package app.revanced.patches.youtube.utils.returnyoutubedislike.rollingnumber

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.returnyoutubedislike.rollingnumber.fingerprints.RollingNumberTextViewFingerprint
import app.revanced.patches.youtube.utils.returnyoutubedislike.rollingnumber.fingerprints.RollingNumberTypeFingerprint
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.integrations.Constants.UTILS_PATH
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch(dependencies = [SettingsPatch::class])
object ReturnYouTubeDislikeRollingNumberPatch : BytecodePatch(
    setOf(
        RollingNumberTextViewFingerprint,
        RollingNumberTypeFingerprint
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

            RollingNumberTypeFingerprint.result?.let {
                it.mutableMethod.apply {
                    val rollingNumberClassIndex = it.scanResult.patternScanResult!!.startIndex
                    val rollingNumberClassReference =
                        getInstruction<BuilderInstruction21c>(rollingNumberClassIndex).reference
                    val rollingNumberClass =
                        context.findClass(rollingNumberClassReference.toString())!!.mutableClass

                    /**
                     * This class handles RollingNumber.
                     * Pass an instance of this class to integrations to use Java Reflection.
                     */
                    rollingNumberClass.methods.find { method -> method.name == "<init>" }
                        ?.apply {
                            val rollingNumberFieldIndex =
                                implementation!!.instructions.indexOfFirst { instruction ->
                                    instruction.opcode == Opcode.IPUT_OBJECT
                                }
                            val rollingNumberFieldName =
                                (getInstruction<ReferenceInstruction>(rollingNumberFieldIndex).reference as FieldReference).name

                            addInstructions(
                                1, """
                                    const-string v0, "$rollingNumberFieldName"
                                    invoke-static {p0, v0}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->initialize(Ljava/lang/Object;Ljava/lang/String;)V
                                    """
                            )
                        } ?: throw PatchException("RollingNumberClass not found!")

                    /**
                     * RollingNumber is initialized in this method.
                     * This method also contains information about ConversionContext.
                     */
                    addInstruction(
                        1,
                        "invoke-static {p$CONVERSION_CONTEXT_PARAMETER}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->onRollingNumberTextLoaded(Ljava/lang/Object;)V"
                    )
                }
            } ?: throw RollingNumberTypeFingerprint.exception

            /**
             * TextView with RollingNumber.
             * Apply spanned text to TextView.
             */
            RollingNumberTextViewFingerprint.result?.let {
                // Initial TextView is set in this method.
                val initiallyCreatedTextViewMethod = it.mutableMethod

                // Video less than 24 hours after uploaded, like counts will be updated in real time.
                // Whenever like counts are updated, TextView is set in this method.
                val realTimeUpdateTextViewMethod = it.mutableClass.methods.find {
                    method -> method.parameterTypes.first() == "Landroid/graphics/Bitmap;"
                } ?: throw PatchException("Failed to find realTimeUpdateTextViewMethod")

                arrayOf(
                    initiallyCreatedTextViewMethod,
                    realTimeUpdateTextViewMethod
                ).forEach { insertMethod ->
                    insertMethod.apply {
                        val setTextIndex = implementation!!.instructions.indexOfFirst { instruction ->
                            ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.name == "setText"
                        }
                        val textViewRegister =
                            getInstruction<FiveRegisterInstruction>(setTextIndex).registerC

                        addInstruction(
                            setTextIndex + 1,
                            "invoke-static {v$textViewRegister}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->updateRollingNumberTextView(Landroid/widget/TextView;)V"
                        )
                    }
                }
            } ?: throw RollingNumberTextViewFingerprint.exception
        }
    }
}
