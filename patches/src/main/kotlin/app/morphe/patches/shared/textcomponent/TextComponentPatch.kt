/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.shared.textcomponent

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.util.findFreeRegister
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.music.utils.playservice.is_9_00_or_greater
import app.morphe.patches.music.utils.playservice.versionCheckPatch
import app.morphe.patches.shared.indexOfSpannableStringInstruction
import app.morphe.patches.shared.spannableStringBuilderFingerprint
import app.morphe.patches.youtube.utils.playservice.is_21_07_or_greater
import app.morphe.util.cloneMutableAndPreserveParameters
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch as youtubeVersionCheckPatch

private var spannedContextRegister = 0

private lateinit var textComponentMethod: MutableMethod
private var textComponentIndex = 0
private var textComponentRegister = 0
private var textComponentContextRegister = 0

val textComponentPatch = bytecodePatch(
    description = "textComponentPatch"
) {
    dependsOn(versionCheckPatch, youtubeVersionCheckPatch)
    execute {
        spannableStringBuilderFingerprint.method.apply {
            val spannedIndex = indexOfSpannableStringInstruction(this)
            val spannedRegister = getInstruction<FiveRegisterInstruction>(spannedIndex).registerC
            spannedContextRegister = findFreeRegister(spannedIndex, spannedRegister)

            addInstructionsAtControlFlowLabel(
                spannedIndex,
                "move-object/from16 v$spannedContextRegister, p0"
            )
        }

        val textComponentContextMethod = TextComponentContextFingerprint.let {
            if (is_21_07_or_greater) {
                it.method.cloneMutableAndPreserveParameters(it.classDef)
            } else {
                it.method
            }
        }

        textComponentContextMethod.apply {
            textComponentMethod = this
            if (is_9_00_or_greater) {
                val charSequenceInvokeIndex = indexOfFirstInstruction {
                    val reference = getReference<MethodReference>()
                    (opcode == Opcode.INVOKE_STATIC || opcode == Opcode.INVOKE_STATIC_RANGE) &&
                            reference?.returnType == "Ljava/lang/CharSequence;"
                }

                if (charSequenceInvokeIndex == -1) {
                    throw PatchException("Could not find text component CharSequence invocation index")
                }

                val invokeInstruction = getInstruction(charSequenceInvokeIndex)
                textComponentContextRegister = when (invokeInstruction) {
                    is RegisterRangeInstruction -> invokeInstruction.startRegister
                    is FiveRegisterInstruction -> invokeInstruction.registerC
                    else -> throw PatchException("Unsupported instruction type for text component context register")
                }

                textComponentRegister =
                    getInstruction<OneRegisterInstruction>(charSequenceInvokeIndex + 1).registerA

                textComponentIndex = charSequenceInvokeIndex + 2
            } else {
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
}

context(_: BytecodePatchContext)
internal fun hookSpannableString(
    classDescriptor: String,
    methodName: String
) = spannableStringBuilderFingerprint.let {
    // Hooks of the same method, such as hookSpannableStringResult, can shift the instructions.
    it.clearMatch()

    it.method.apply {
        val spannedIndex = indexOfSpannableStringInstruction(this)
        val spannedRegister = getInstruction<FiveRegisterInstruction>(spannedIndex).registerC

        addInstructions(
            spannedIndex, """
                invoke-static {v$spannedContextRegister, v$spannedRegister}, $classDescriptor->$methodName(Ljava/lang/Object;Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
                move-result-object v$spannedRegister
                """
        )
    }
}

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

/**
 * Hooks the same texts as [hookSpannableString], where they are returned once their style runs
 * are applied as spans. Unlike [hookSpannableString], this also includes the texts without any
 * style runs. The extension method returns the text to use, and text it inserts with a
 * SpannableStringBuilder keeps the spans in place.
 */
context(_: BytecodePatchContext)
internal fun hookSpannableStringResult(
    classDescriptor: String,
    methodName: String = "onLithoTextLoaded"
) = spannableStringBuilderFingerprint.method.apply {
    findInstructionIndicesReversedOrThrow(Opcode.RETURN_OBJECT).forEach { returnIndex ->
        val textRegister = getInstruction<OneRegisterInstruction>(returnIndex).registerA
        val contextRegister = findFreeRegister(returnIndex, textRegister)

        addInstructionsAtControlFlowLabel(
            returnIndex,
            """
                move-object/from16 v$contextRegister, p0
                invoke-static { v$contextRegister, v$textRegister }, $classDescriptor->$methodName(Ljava/lang/Object;Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
                move-result-object v$textRegister
            """
        )
    }
}
