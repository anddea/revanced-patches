/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Portions of this file are modified by anddea:
 * Copyright (C) 2026 anddea
 * https://github.com/anddea/revanced-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.shared.textcomponent.copytext

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.litho.context.EXTENSION_CONTEXT_INTERFACE
import app.morphe.patches.shared.litho.context.conversionContextClassDef
import app.morphe.patches.shared.litho.context.conversionContextPatch
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.cloneMutableAndPreserveParameters
import app.morphe.util.getReference
import app.morphe.util.findFreeRegister
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.insertLiteralOverride
import app.morphe.util.numberOfParameterRegistersLogical
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import java.lang.ref.WeakReference

private lateinit var textComponentLookupMethodRef : WeakReference<MutableMethod>
private var textComponentLookupInsertIndex = -1
private var textComponentLookupConversionContext = -1
private var textComponentLookupConversionContextField : Field? = null
private var textComponentLookupCharSequenceRegister = -1


private var lithoSpannableStringCreationMethodRef: WeakReference<MutableMethod>? = null
private var lithoSpannableStringCreationInsertIndex = -1
private var lithoSpannableStringCreationConversionContextRegister = -1
private var lithoSpannableStringCreationConversionContextField : String = ""
private var lithoSpannableStringCreationCharSequenceRegister = -1

private const val EXTENSION_CLASS = "Lapp/morphe/extension/shared/patches/TextComponentPatch;"

/**
 * Ported with local parameter-cloning and instruction-search utilities so cached and newly
 * created Litho text both reach Copy text without changing the existing text-component hooks.
 */

/**
 * Hooks the creation and cached lookup of YouTube Litho text Spans.
 */
internal val lithoSpannableStringPatch = bytecodePatch(
    description = "Provides hooks into Litho text Spans for extension filtering."
) {
    dependsOn(
        conversionContextPatch
    )

    execute {
        // region Hook code for creation and cached lookup of text Spans.

        // Alternatively the hook can be made in the creation of Spans in TextComponentSpec.
        // And it works in all situations except if the likes do not such as disliking.
        // This hook handles all situations, as it's where the created Spans are stored and later reused.

        // Find the field name of the conversion context.
        val textComponentConversionContextField = TextComponentConstructorFingerprint
            .originalClassDef.fields.find {
                it.type == conversionContextClassDef.type
            } ?: throw PatchException("Could not find conversion context field")

        // Old pre 20.40 and lower hook.
        TextComponentLookupFingerprint.let {
            // 21.05 clobbers p0 (this) register.
            // Add additional registers so all parameters including p0 are free to use anywhere in the method.
            it.method.cloneMutableAndPreserveParameters(it.classDef).apply {
                val setterIndex = indexOfFirstInstruction {
                    val reference = getReference<MethodReference>()
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            reference?.returnType == "V" &&
                            reference.parameterTypes == listOf("Ljava/lang/CharSequence;")
                }
                val insertIndex: Int
                val charSequenceRegister: Int
                if (setterIndex >= 0) {
                    insertIndex = setterIndex
                    charSequenceRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerD
                } else {
                    insertIndex = indexOfFirstInstructionOrThrow {
                        opcode == Opcode.NEW_INSTANCE &&
                                getReference<TypeReference>()?.type == TextComponentDataFingerprint.originalClassDef.type
                    }
                    val charSequenceIndex = indexOfFirstInstructionOrThrow(insertIndex) {
                        opcode == Opcode.IPUT_OBJECT &&
                                getReference<FieldReference>()?.type == "Ljava/lang/CharSequence;"
                    }
                    charSequenceRegister = getInstruction<TwoRegisterInstruction>(charSequenceIndex).registerA
                }
                val conversionContext = findFreeRegister(insertIndex, charSequenceRegister)

                textComponentLookupMethodRef = WeakReference(this)
                textComponentLookupInsertIndex = insertIndex
                textComponentLookupConversionContext = conversionContext
                textComponentLookupConversionContextField = textComponentConversionContextField
                textComponentLookupCharSequenceRegister = charSequenceRegister

                addInstructionsAtControlFlowLabel(
                    textComponentLookupInsertIndex,
                    """
                        # Copy conversion context.
                        move-object/from16 v$textComponentLookupConversionContext, p0
                        iget-object v$textComponentLookupConversionContext, v$textComponentLookupConversionContext, $textComponentLookupConversionContextField
                    """
                )

                textComponentLookupInsertIndex += 2
            }
        }

        // Older versions only use the lookup path and have no separate Span creation hook.
        lithoSpannableStringCreationMethodRef = null
        LithoSpannableStringCreationFingerprint.matchOrNull()?.let {
            val conversionContextField = it.classDef.type +
                    "->" + textComponentConversionContextField.name +
                    ":" + textComponentConversionContextField.type

            // 21.05+ clobbers p0 and must clone to preserve it.
            it.method.cloneMutableAndPreserveParameters(it.classDef).apply {
                // Must offset match indexes since cloning adds additional move instructions.
                val insertIndex = it.instructionMatches[1].index + numberOfParameterRegistersLogical
                val charSequenceRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerD
                val conversionContextRegister = findFreeRegister(insertIndex, charSequenceRegister)

                lithoSpannableStringCreationMethodRef = WeakReference(this)
                lithoSpannableStringCreationInsertIndex = insertIndex
                lithoSpannableStringCreationConversionContextRegister = conversionContextRegister
                lithoSpannableStringCreationConversionContextField = conversionContextField
                lithoSpannableStringCreationCharSequenceRegister = charSequenceRegister

                addInstructions(
                    lithoSpannableStringCreationInsertIndex,
                    """
                        move-object/from16 v$lithoSpannableStringCreationConversionContextRegister, p0
                        iget-object v$lithoSpannableStringCreationConversionContextRegister, v$lithoSpannableStringCreationConversionContextRegister, $lithoSpannableStringCreationConversionContextField
                    """
                )

                lithoSpannableStringCreationInsertIndex += 2
            }
        }

        // Hook new Litho text creation when its feature flag exists.
        TextComponentFeatureFlagFingerprint.matchAllOrNull().orEmpty().forEach {
            it.method.insertLiteralOverride(
                it.instructionMatches.first().index,
                "$EXTENSION_CLASS->useNewLithoTextCreation(Z)Z"
            )
        }

        // endregion
    }
}

internal fun hookLithoSpannableString(
    classDescriptor: String,
    methodName: String = "onLithoTextLoaded",
) {
    textComponentLookupMethodRef.get()!!.addInstructionsAtControlFlowLabel(
        textComponentLookupInsertIndex,
        """
            invoke-static { v$textComponentLookupConversionContext, v$textComponentLookupCharSequenceRegister }, $classDescriptor->$methodName(${EXTENSION_CONTEXT_INTERFACE}Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
            move-result-object v$textComponentLookupCharSequenceRegister

            :ignore
            nop
        """
    )

    textComponentLookupInsertIndex += 3

    lithoSpannableStringCreationMethodRef?.get()?.apply {
        addInstructions(
            lithoSpannableStringCreationInsertIndex,
            """
                invoke-static { v$lithoSpannableStringCreationConversionContextRegister, v$lithoSpannableStringCreationCharSequenceRegister }, $classDescriptor->$methodName(${EXTENSION_CONTEXT_INTERFACE}Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
                move-result-object v$lithoSpannableStringCreationCharSequenceRegister
            """
        )

        lithoSpannableStringCreationInsertIndex += 2
    }
}
