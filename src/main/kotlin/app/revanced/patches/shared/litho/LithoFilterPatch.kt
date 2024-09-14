package app.revanced.patches.shared.litho

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.or
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.shared.litho.fingerprints.ByteBufferFingerprint
import app.revanced.patches.shared.litho.fingerprints.EmptyComponentsFingerprint
import app.revanced.patches.shared.litho.fingerprints.PathBuilderFingerprint
import app.revanced.util.getReference
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.resultOrThrow
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
import java.io.Closeable

@Suppress("SpellCheckingInspection", "unused")
object LithoFilterPatch : BytecodePatch(
    setOf(
        ByteBufferFingerprint,
        EmptyComponentsFingerprint,
    )
), Closeable {
    private const val INTEGRATIONS_LITHO_FILER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/LithoFilterPatch;"

    private const val INTEGRATIONS_FILER_ARRAY_DESCRIPTOR =
        "[$COMPONENTS_PATH/Filter;"

    private lateinit var filterArrayMethod: MutableMethod
    private var filterCount = 0

    internal lateinit var addFilter: (String) -> Unit
        private set

    override fun execute(context: BytecodeContext) {

        // region Pass the buffer into Integrations.

        ByteBufferFingerprint.resultOrThrow().mutableMethod.addInstruction(
            0,
            "invoke-static { p2 }, $INTEGRATIONS_LITHO_FILER_CLASS_DESCRIPTOR->setProtoBuffer(Ljava/nio/ByteBuffer;)V"
        )

        // endregion

        var (emptyComponentMethod, emptyComponentLabel) =
            EmptyComponentsFingerprint.resultOrThrow().let {
                PathBuilderFingerprint.resolve(context, it.classDef)
                with(it.mutableMethod) {
                    val emptyComponentMethodIndex = it.scanResult.patternScanResult!!.startIndex + 1
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
                    .forEach {
                        val insertRegister =
                            getInstruction<OneRegisterInstruction>(it + 1).registerA
                        val insertIndex = it + 2

                        addInstructionsWithLabels(
                            insertIndex, """
                                    if-nez v$insertRegister, :ignore
                                    """ + emptyComponentLabel,
                            ExternalLabel("ignore", getInstruction(insertIndex))
                        )
                    }

                emptyComponentLabel = """
                    const/4 v0, 0x0
                    return-object v0
                    """
            }
        }

        PathBuilderFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                checkMethodSignatureMatch(this)

                val stringBuilderIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.IPUT_OBJECT &&
                            getReference<FieldReference>()?.type == "Ljava/lang/StringBuilder;"
                }
                val stringBuilderRegister =
                    getInstruction<TwoRegisterInstruction>(stringBuilderIndex).registerA

                val emptyStringIndex = getStringInstructionIndex("")
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
                        invoke-static {v$stringBuilderRegister, v$identifierRegister, v$objectRegister}, $INTEGRATIONS_LITHO_FILER_CLASS_DESCRIPTOR->filter(Ljava/lang/StringBuilder;Ljava/lang/String;Ljava/lang/Object;)Z
                        move-result v$stringBuilderRegister
                        if-eqz v$stringBuilderRegister, :filter
                        """ + emptyComponentLabel,
                    ExternalLabel("filter", getInstruction(insertIndex))
                )
            }
        }

        // Create a new method to get the filter array to avoid register conflicts.
        // This fixes an issue with Integrations compiled with Android Gradle Plugin 8.3.0+.
        // https://github.com/ReVanced/revanced-patches/issues/2818
        val lithoFilterMethods = context.findClass(INTEGRATIONS_LITHO_FILER_CLASS_DESCRIPTOR)
            ?.mutableClass
            ?.methods
            ?: throw PatchException("LithoFilterPatch class not found.")

        lithoFilterMethods
            .first { it.name == "<clinit>" }
            .apply {
                val setArrayIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.SPUT_OBJECT &&
                            getReference<FieldReference>()?.type == INTEGRATIONS_FILER_ARRAY_DESCRIPTOR
                }
                val setArrayRegister =
                    getInstruction<OneRegisterInstruction>(setArrayIndex).registerA
                val addedMethodName = "getFilterArray"

                addInstructions(
                    setArrayIndex, """
                        invoke-static {}, $INTEGRATIONS_LITHO_FILER_CLASS_DESCRIPTOR->$addedMethodName()$INTEGRATIONS_FILER_ARRAY_DESCRIPTOR
                        move-result-object v$setArrayRegister
                        """
                )

                filterArrayMethod = ImmutableMethod(
                    definingClass,
                    addedMethodName,
                    emptyList(),
                    INTEGRATIONS_FILER_ARRAY_DESCRIPTOR,
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

        addFilter = { classDescriptor ->
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

    override fun close() = filterArrayMethod.addInstructions(
        0,
        """
            const/16 v0, $filterCount
            new-array v2, v0, $INTEGRATIONS_FILER_ARRAY_DESCRIPTOR
            """
    )
}