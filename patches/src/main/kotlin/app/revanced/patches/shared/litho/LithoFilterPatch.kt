package app.revanced.patches.shared.litho

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.extension.Constants.COMPONENTS_PATH
import app.revanced.util.findMethodsOrThrow
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.indexOfFirstStringInstructionOrThrow
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val EXTENSION_LITHO_FILER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/LithoFilterPatch;"

private const val EXTENSION_FILER_ARRAY_DESCRIPTOR =
    "[$COMPONENTS_PATH/Filter;"

private lateinit var filterArrayMethod: MutableMethod
private var filterCount = 0

internal lateinit var addLithoFilter: (String) -> Unit
    private set

val lithoFilterPatch = bytecodePatch(
    description = "lithoFilterPatch",
) {
    execute {

        // region Pass the buffer into extension.

        byteBufferFingerprint.methodOrThrow().addInstruction(
            0,
            "invoke-static { p2 }, $EXTENSION_LITHO_FILER_CLASS_DESCRIPTOR->setProtoBuffer(Ljava/nio/ByteBuffer;)V"
        )

        // endregion

        var (emptyComponentMethod, emptyComponentLabel) =
            emptyComponentsFingerprint.matchOrThrow().let {
                with(it.method) {
                    val emptyComponentMethodIndex = it.patternMatch!!.startIndex + 1
                    val emptyComponentMethodReference =
                        getInstruction<ReferenceInstruction>(emptyComponentMethodIndex).reference
                    val emptyComponentFieldReference =
                        getInstruction<ReferenceInstruction>(emptyComponentMethodIndex + 2).reference

                    val label = """
                        move-object/from16 v0, p1
                        invoke-static {v0}, $emptyComponentMethodReference
                        move-result-object v0
                        iget-object v0, v0, $emptyComponentFieldReference
                        return-object v0
                        """

                    Pair(this, label)
                }
            }

        fun checkMethodSignatureMatch(pathBuilder: MutableMethod) = emptyComponentMethod.apply {
            if (!MethodUtil.methodSignaturesMatch(pathBuilder, this)) {
                implementation!!.instructions
                    .withIndex()
                    .filter { (_, instruction) ->
                        val reference = (instruction as? ReferenceInstruction)?.reference
                        reference is MethodReference &&
                                MethodUtil.methodSignaturesMatch(pathBuilder, reference)
                    }
                    .map { (index, _) -> index }
                    .reversed()
                    .forEach { index ->
                        val insertInstruction = getInstruction(index + 1)
                        if (insertInstruction is OneRegisterInstruction) {
                            val insertRegister =
                                insertInstruction.registerA
                            val insertIndex = index + 2

                            addInstructionsWithLabels(
                                insertIndex, """
                                    if-nez v$insertRegister, :ignore
                                    """ + emptyComponentLabel,
                                ExternalLabel("ignore", getInstruction(insertIndex))
                            )
                        }
                    }

                emptyComponentLabel = """
                    const/4 v0, 0x0
                    return-object v0
                    """
            }
        }

        pathBuilderFingerprint.methodOrThrow().apply {
            checkMethodSignatureMatch(this)

            val stringBuilderIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.IPUT_OBJECT &&
                        getReference<FieldReference>()?.type == "Ljava/lang/StringBuilder;"
            }
            val stringBuilderRegister =
                getInstruction<TwoRegisterInstruction>(stringBuilderIndex).registerA

            val emptyStringIndex = indexOfFirstStringInstructionOrThrow("")
            val identifierRegister = getInstruction<TwoRegisterInstruction>(
                indexOfFirstInstructionReversedOrThrow(emptyStringIndex) {
                    opcode == Opcode.IPUT_OBJECT
                            && getReference<FieldReference>()?.type == "Ljava/lang/String;"
                }
            ).registerA
            val objectRegister = getInstruction<FiveRegisterInstruction>(
                indexOfFirstInstructionOrThrow(emptyStringIndex) {
                    opcode == Opcode.INVOKE_VIRTUAL
                }
            ).registerC

            val insertIndex = stringBuilderIndex + 1

            addInstructionsWithLabels(
                insertIndex, """
                    invoke-static {v$stringBuilderRegister, v$identifierRegister, v$objectRegister}, $EXTENSION_LITHO_FILER_CLASS_DESCRIPTOR->filter(Ljava/lang/StringBuilder;Ljava/lang/String;Ljava/lang/Object;)Z
                    move-result v$stringBuilderRegister
                    if-eqz v$stringBuilderRegister, :filter
                    """ + emptyComponentLabel,
                ExternalLabel("filter", getInstruction(insertIndex))
            )
        }

        // region A/B test of new Litho native code.

        // Turn off native code that handles litho component names.  If this feature is on then nearly
        // all litho components have a null name and identifier/path filtering is completely broken.

        if (bufferUpbFeatureFlagFingerprint.second.methodOrNull != null &&
            pathUpbFeatureFlagFingerprint.second.methodOrNull != null
        ) {
            mapOf(
                bufferUpbFeatureFlagFingerprint to 45419603L,
                pathUpbFeatureFlagFingerprint to 45631264L,
            ).forEach { (fingerprint, literalValue) ->
                fingerprint.injectLiteralInstructionBooleanCall(
                    literalValue,
                    "0x0"
                )
            }
        }

        // endregion

        // Create a new method to get the filter array to avoid register conflicts.
        // This fixes an issue with extension compiled with Android Gradle Plugin 8.3.0+.
        // https://github.com/ReVanced/revanced-patches/issues/2818
        val lithoFilterMethods = findMethodsOrThrow(EXTENSION_LITHO_FILER_CLASS_DESCRIPTOR)

        lithoFilterMethods
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
                        invoke-static {}, $EXTENSION_LITHO_FILER_CLASS_DESCRIPTOR->$addedMethodName()$EXTENSION_FILER_ARRAY_DESCRIPTOR
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

                lithoFilterMethods.add(filterArrayMethod)
            }

        addLithoFilter = { classDescriptor ->
            filterArrayMethod.addInstructions(
                0,
                """
                    new-instance v0, $classDescriptor
                    invoke-direct {v0}, $classDescriptor-><init>()V
                    const/16 v1, ${filterCount++}
                    aput-object v0, v2, v1
                    """
            )
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


