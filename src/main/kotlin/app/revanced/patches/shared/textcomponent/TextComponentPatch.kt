package app.revanced.patches.shared.textcomponent

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.textcomponent.fingerprints.SpannableStringBuilderFingerprint
import app.revanced.patches.shared.textcomponent.fingerprints.TextComponentConstructorFingerprint
import app.revanced.patches.shared.textcomponent.fingerprints.TextComponentContextFingerprint
import app.revanced.util.alsoResolve
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

object TextComponentPatch : BytecodePatch(
    setOf(
        SpannableStringBuilderFingerprint,
        TextComponentConstructorFingerprint,
    )
) {
    override fun execute(context: BytecodeContext) {

        SpannableStringBuilderFingerprint.resultOrThrow().mutableMethod.apply {
            spannedMethod = this
            spannedIndex = SpannableStringBuilderFingerprint.indexOfSpannableStringInstruction(this)
            spannedRegister = getInstruction<FiveRegisterInstruction>(spannedIndex).registerC
            spannedContextRegister =
                getInstruction<TwoRegisterInstruction>(0).registerA

            replaceInstruction(
                spannedIndex,
                "nop"
            )
            addInstruction(
                ++spannedIndex,
                "invoke-static {v$spannedRegister}, ${SpannableStringBuilderFingerprint.SPANNABLE_STRING_REFERENCE}"
            )
        }

        TextComponentContextFingerprint.alsoResolve(
            context, TextComponentConstructorFingerprint
        ).let {
            it.mutableMethod.apply {
                textComponentMethod = this
                val conversionContextFieldIndex = indexOfFirstInstructionOrThrow {
                    getReference<FieldReference>()?.type == "Ljava/util/Map;"
                } - 1
                val conversionContextFieldReference =
                    getInstruction<ReferenceInstruction>(conversionContextFieldIndex).reference

                // ~ YouTube 19.32.xx
                val legacyCharSequenceIndex = indexOfFirstInstruction {
                    getReference<FieldReference>()?.type == "Ljava/util/BitSet;"
                } - 1
                val charSequenceIndex = indexOfFirstInstruction {
                    val reference = getReference<MethodReference>()
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            reference?.returnType == "V" &&
                            reference.parameterTypes.firstOrNull() == "Ljava/lang/CharSequence;"
                }

                val insertIndex: Int

                if (legacyCharSequenceIndex > -2) {
                    textComponentRegister =
                        getInstruction<TwoRegisterInstruction>(legacyCharSequenceIndex).registerA
                    insertIndex = legacyCharSequenceIndex - 1
                } else if (charSequenceIndex > -1) {
                    textComponentRegister =
                        getInstruction<FiveRegisterInstruction>(charSequenceIndex).registerD
                    insertIndex = charSequenceIndex
                } else {
                    throw PatchException("Could not find insert index")
                }

                textComponentContextRegister = getInstruction<TwoRegisterInstruction>(
                    indexOfFirstInstructionOrThrow(insertIndex, Opcode.IGET_OBJECT)
                ).registerA

                addInstructions(
                    insertIndex, """
                        move-object/from16 v$textComponentContextRegister, p0
                        iget-object v$textComponentContextRegister, v$textComponentContextRegister, $conversionContextFieldReference
                        """
                )
                textComponentIndex = insertIndex + 2
            }
        }
    }

    private lateinit var spannedMethod: MutableMethod
    private var spannedIndex = 0
    private var spannedRegister = 0
    private var spannedContextRegister = 0

    private lateinit var textComponentMethod: MutableMethod
    private var textComponentIndex = 0
    private var textComponentRegister = 0
    private var textComponentContextRegister = 0

    fun hookSpannableString(
        classDescriptor: String,
        methodName: String
    ) = spannedMethod.addInstructions(
        spannedIndex, """
            invoke-static {v$spannedContextRegister, v$spannedRegister}, $classDescriptor->$methodName(Ljava/lang/Object;Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
            move-result-object v$spannedRegister
            """
    )

    fun hookTextComponent(
        classDescriptor: String,
        methodName: String = "onLithoTextLoaded"
    ) = textComponentMethod.apply {
        addInstructions(
            textComponentIndex, """
                invoke-static {v$textComponentContextRegister, v$textComponentRegister}, $classDescriptor->$methodName(Ljava/lang/Object;Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
                move-result-object v$textComponentRegister
                """
        )
        textComponentIndex += 2
    }
}

