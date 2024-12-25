package app.revanced.patches.shared.spans

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.shared.extension.Constants.SPANS_PATH
import app.revanced.patches.shared.indexOfSpannableStringInstruction
import app.revanced.patches.shared.spannableStringBuilderFingerprint
import app.revanced.patches.shared.textcomponent.hookSpannableString
import app.revanced.patches.shared.textcomponent.textComponentPatch
import app.revanced.util.findMethodOrThrow
import app.revanced.util.findMethodsOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.mutableClassOrThrow
import app.revanced.util.getFiveRegisters
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

private const val EXTENSION_SPANS_CLASS_DESCRIPTOR =
    "$SPANS_PATH/InclusiveSpanPatch;"

private const val EXTENSION_FILER_ARRAY_DESCRIPTOR =
    "[$SPANS_PATH/Filter;"

private lateinit var filterArrayMethod: MutableMethod
private var filterCount = 0

internal lateinit var addSpanFilter: (String) -> Unit
    private set

val inclusiveSpanPatch = bytecodePatch(
    description = "inclusiveSpanPatch"
) {
    dependsOn(textComponentPatch)

    execute {
        hookSpannableString(
            EXTENSION_SPANS_CLASS_DESCRIPTOR,
            "setConversionContext"
        )

        spannableStringBuilderFingerprint.methodOrThrow().apply {
            val spannedIndex = indexOfSpannableStringInstruction(this)
            val setInclusiveSpanIndex = indexOfFirstInstructionOrThrow(spannedIndex) {
                val reference = getReference<MethodReference>()
                opcode == Opcode.INVOKE_STATIC &&
                        reference?.returnType == "V" &&
                        reference.parameterTypes.size > 3 &&
                        reference.parameterTypes.firstOrNull() == "Landroid/text/SpannableString;"
            }
            val setInclusiveSpanMethod = getWalkerMethod(setInclusiveSpanIndex)

            setInclusiveSpanMethod.apply {
                val insertIndex = indexOfFirstInstructionReversedOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>().toString() == "Landroid/text/SpannableString;->setSpan(Ljava/lang/Object;III)V"
                }
                replaceInstruction(
                    insertIndex,
                    "invoke-static { ${getFiveRegisters(insertIndex)} }, " +
                            EXTENSION_SPANS_CLASS_DESCRIPTOR +
                            "->" +
                            "setSpan(Landroid/text/SpannableString;Ljava/lang/Object;III)V"
                )
            }

            val customCharacterStyle =
                customCharacterStyleFingerprint.mutableClassOrThrow().type

            findMethodOrThrow(EXTENSION_SPANS_CLASS_DESCRIPTOR) {
                name == "getSpanType" &&
                        returnType != "Ljava/lang/String;"
            }.apply {
                val index = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INSTANCE_OF &&
                            (this as? ReferenceInstruction)?.reference?.toString() == "Landroid/text/style/CharacterStyle;"
                }
                val instruction = getInstruction<TwoRegisterInstruction>(index)
                replaceInstruction(
                    index,
                    "instance-of v${instruction.registerA}, v${instruction.registerB}, $customCharacterStyle"
                )
            }


            // Create a new method to get the filter array to avoid register conflicts.
            // This fixes an issue with extension compiled with Android Gradle Plugin 8.3.0+.
            // https://github.com/ReVanced/revanced-patches/issues/2818
            val spansFilterMethods = findMethodsOrThrow(EXTENSION_SPANS_CLASS_DESCRIPTOR)

            spansFilterMethods
                .first { it.name == "<clinit>" }
                .apply {
                    val setArrayIndex = indexOfFirstInstructionOrThrow {
                        opcode == Opcode.SPUT_OBJECT &&
                                getReference<FieldReference>()?.type == EXTENSION_FILER_ARRAY_DESCRIPTOR
                    }
                    val setArrayRegister =
                        getInstruction<OneRegisterInstruction>(setArrayIndex).registerA
                    val addedMethodName = "getFilterArray"

                    addInstructions(
                        setArrayIndex, """
                        invoke-static {}, $EXTENSION_SPANS_CLASS_DESCRIPTOR->$addedMethodName()$EXTENSION_FILER_ARRAY_DESCRIPTOR
                        move-result-object v$setArrayRegister
                        """
                    )

                    filterArrayMethod = ImmutableMethod(
                        definingClass,
                        addedMethodName,
                        emptyList(),
                        EXTENSION_FILER_ARRAY_DESCRIPTOR,
                        AccessFlags.PRIVATE or AccessFlags.STATIC,
                        null,
                        null,
                        MutableMethodImplementation(3),
                    ).toMutable().apply {
                        addInstruction(
                            0,
                            "return-object v2"
                        )
                    }

                    spansFilterMethods.add(filterArrayMethod)
                }

            addSpanFilter = { classDescriptor ->
                filterArrayMethod.addInstructions(
                    0, """
                        new-instance v0, $classDescriptor
                        invoke-direct {v0}, $classDescriptor-><init>()V
                        const/16 v1, ${filterCount++}
                        aput-object v0, v2, v1
                        """
                )
            }
        }

    }

    finalize {
        filterArrayMethod.addInstructions(
            0, """
                const/16 v0, $filterCount
                new-array v2, v0, $EXTENSION_FILER_ARRAY_DESCRIPTOR
                """
        )
    }
}


