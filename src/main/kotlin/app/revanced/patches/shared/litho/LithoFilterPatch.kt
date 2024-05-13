package app.revanced.patches.shared.litho

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.shared.litho.fingerprints.LithoFilterPatchConstructorFingerprint
import app.revanced.patches.shared.litho.fingerprints.PathBuilderFingerprint
import app.revanced.patches.shared.litho.fingerprints.SetByteBufferFingerprint
import app.revanced.util.getEmptyStringInstructionIndex
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexReversed
import app.revanced.util.getTargetIndexWithFieldReferenceType
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import java.io.Closeable

@Suppress("SpellCheckingInspection", "unused")
object LithoFilterPatch : BytecodePatch(
    setOf(
        LithoFilterPatchConstructorFingerprint,
        PathBuilderFingerprint,
        SetByteBufferFingerprint
    )
), Closeable {
    private const val INTEGRATIONS_LITHO_FILER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/LithoFilterPatch;"

    private const val INTEGRATIONS_FILER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/Filter;"

    internal lateinit var addFilter: (String) -> Unit
        private set

    private var filterCount = 0

    override fun execute(context: BytecodeContext) {

        SetByteBufferFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = getTargetIndex(Opcode.IF_EQZ) + 1

                addInstruction(
                    insertIndex,
                    "invoke-static { p2 }, $INTEGRATIONS_LITHO_FILER_CLASS_DESCRIPTOR->setProtoBuffer(Ljava/nio/ByteBuffer;)V"
                )
            }
        }

        PathBuilderFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val emptyComponentMethodIndex = it.scanResult.patternScanResult!!.startIndex + 1
                val emptyComponentMethodReference =
                    getInstruction<ReferenceInstruction>(emptyComponentMethodIndex).reference
                val emptyComponentFieldReference =
                    getInstruction<ReferenceInstruction>(emptyComponentMethodIndex + 2).reference

                val stringBuilderIndex = getTargetIndexWithFieldReferenceType("Ljava/lang/StringBuilder;")
                val stringBuilderRegister = getInstruction<TwoRegisterInstruction>(stringBuilderIndex).registerA

                val emptyStringIndex = getEmptyStringInstructionIndex()

                val identifierIndex = getTargetIndexReversed(emptyStringIndex, Opcode.IPUT_OBJECT)
                val identifierRegister = getInstruction<TwoRegisterInstruction>(identifierIndex).registerA

                val objectIndex = getTargetIndex(emptyStringIndex, Opcode.INVOKE_VIRTUAL)
                val objectRegister = getInstruction<BuilderInstruction35c>(objectIndex).registerC

                val insertIndex = stringBuilderIndex + 1

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {v$stringBuilderRegister, v$identifierRegister, v$objectRegister}, $INTEGRATIONS_LITHO_FILER_CLASS_DESCRIPTOR->filter(Ljava/lang/StringBuilder;Ljava/lang/String;Ljava/lang/Object;)Z
                        move-result v$stringBuilderRegister
                        if-eqz v$stringBuilderRegister, :filter
                        move-object/from16 v0, p1
                        invoke-static {v0}, $emptyComponentMethodReference
                        move-result-object v0
                        iget-object v0, v0, $emptyComponentFieldReference
                        return-object v0
                        """, ExternalLabel("filter", getInstruction(insertIndex))
                )
            }
        }

        LithoFilterPatchConstructorFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                removeInstructions(0, 6)

                addFilter = { classDescriptor ->
                    addInstructions(
                        0, """
                            new-instance v1, $classDescriptor
                            invoke-direct {v1}, $classDescriptor-><init>()V
                            const/16 v2, ${filterCount++}
                            aput-object v1, v0, v2
                            """
                    )
                }
            }
        }
    }

    override fun close() = LithoFilterPatchConstructorFingerprint.result!!
        .mutableMethod.addInstructions(
            0, """
                const/16 v0, $filterCount
                new-array v0, v0, [$INTEGRATIONS_FILER_CLASS_DESCRIPTOR
                """
        )
}