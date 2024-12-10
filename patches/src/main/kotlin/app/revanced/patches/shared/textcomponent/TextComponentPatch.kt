package app.revanced.patches.shared.textcomponent

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.SPANNABLE_STRING_REFERENCE
import app.revanced.patches.shared.indexOfSpannableStringInstruction
import app.revanced.patches.shared.spannableStringBuilderFingerprint
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private lateinit var spannedMethod: MutableMethod
private var spannedIndex = 0
private var spannedRegister = 0
private var spannedContextRegister = 0

private lateinit var textComponentMethod: MutableMethod
private var textComponentIndex = 0
private var textComponentRegister = 0
private var textComponentContextRegister = 0

val textComponentPatch = bytecodePatch(
    description = "textComponentPatch"
) {
    execute {
        spannableStringBuilderFingerprint.methodOrThrow().apply {
            spannedMethod = this
            spannedIndex = indexOfSpannableStringInstruction(this)
            spannedRegister = getInstruction<FiveRegisterInstruction>(spannedIndex).registerC
            spannedContextRegister =
                getInstruction<TwoRegisterInstruction>(0).registerA

            replaceInstruction(
                spannedIndex,
                "nop"
            )
            addInstruction(
                ++spannedIndex,
                "invoke-static {v$spannedRegister}, $SPANNABLE_STRING_REFERENCE"
            )
        }

        textComponentContextFingerprint.methodOrThrow(textComponentConstructorFingerprint).apply {
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

internal fun hookSpannableString(
    classDescriptor: String,
    methodName: String
) = spannedMethod.addInstructions(
    spannedIndex, """
        invoke-static {v$spannedContextRegister, v$spannedRegister}, $classDescriptor->$methodName(Ljava/lang/Object;Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
        move-result-object v$spannedRegister
        """
)

internal fun hookTextComponent(
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

