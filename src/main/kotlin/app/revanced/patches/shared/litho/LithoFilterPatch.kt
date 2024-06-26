package app.revanced.patches.shared.litho

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.shared.litho.fingerprints.EmptyComponentsFingerprint
import app.revanced.patches.shared.litho.fingerprints.LithoFilterPatchConstructorFingerprint
import app.revanced.patches.shared.litho.fingerprints.PathBuilderFingerprint
import app.revanced.patches.shared.litho.fingerprints.SetByteBufferFingerprint
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.getTargetIndexOrThrow
import app.revanced.util.getTargetIndexReversedOrThrow
import app.revanced.util.getTargetIndexWithFieldReferenceTypeOrThrow
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import java.io.Closeable

@Suppress("SpellCheckingInspection", "unused")
object LithoFilterPatch : BytecodePatch(
    setOf(
        EmptyComponentsFingerprint,
        LithoFilterPatchConstructorFingerprint,
        SetByteBufferFingerprint
    )
), Closeable {
    private const val INTEGRATIONS_LITHO_FILER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/LithoFilterPatch;"

    private const val INTEGRATIONS_FILER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/Filter;"

    internal lateinit var addFilter: (String) -> Unit
        private set

    private lateinit var emptyComponentMethod: MutableMethod

    private lateinit var emptyComponentLabel: String
    private lateinit var emptyComponentMethodName: String

    private lateinit var pathBuilderMethodCall: String

    private var filterCount = 0

    override fun execute(context: BytecodeContext) {

        SetByteBufferFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = getTargetIndexOrThrow(Opcode.IF_EQZ) + 1

                addInstruction(
                    insertIndex,
                    "invoke-static { p2 }, $INTEGRATIONS_LITHO_FILER_CLASS_DESCRIPTOR->setProtoBuffer(Ljava/nio/ByteBuffer;)V"
                )
            }
        }

        EmptyComponentsFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                // resolves fingerprint.
                PathBuilderFingerprint.resolve(context, it.classDef)
                emptyComponentMethod = this
                emptyComponentMethodName = name

                val emptyComponentMethodIndex = it.scanResult.patternScanResult!!.startIndex + 1
                val emptyComponentMethodReference =
                    getInstruction<ReferenceInstruction>(emptyComponentMethodIndex).reference
                val emptyComponentFieldReference =
                    getInstruction<ReferenceInstruction>(emptyComponentMethodIndex + 2).reference

                emptyComponentLabel = """
                    move-object/from16 v0, p1
                    invoke-static {v0}, $emptyComponentMethodReference
                    move-result-object v0
                    iget-object v0, v0, $emptyComponentFieldReference
                    return-object v0
                    """
            }
        }

        PathBuilderFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                // If the EmptyComponents Method and the PathBuilder Method are different,
                // new inject way is required.
                // TODO: Refactor LithoFilter patch when support for YouTube 18.29.38 ~ 19.17.41 and YT Music 6.29.58 ~ 6.51.53 is dropped.
                if (emptyComponentMethodName != name) {
                    // In this case, the access modifier of the method that handles PathBuilder is 'AccessFlags.PRIVATE or AccessFlags.FINAL.
                    // Methods that handle PathBuilder are invoked by methods that handle EmptyComponents.
                    // 'pathBuilderMethodCall' is a reference that invokes the PathBuilder Method.
                    pathBuilderMethodCall = "$definingClass->$name("
                    for (i in 0 until parameters.size) {
                        pathBuilderMethodCall += parameterTypes[i]
                    }
                    pathBuilderMethodCall += ")$returnType"

                    emptyComponentMethod.apply {
                        // If the return value of the PathBuilder Method is null,
                        // it means that pathBuilder has been filtered by the LithoFilterPatch.
                        // (Refer comments below.)
                        // Returns emptyComponents.
                        for (index in implementation!!.instructions.size - 1 downTo 0) {
                            val instruction = getInstruction(index)
                            if ((instruction as? ReferenceInstruction)?.reference.toString() != pathBuilderMethodCall)
                                continue

                            val insertRegister =
                                getInstruction<OneRegisterInstruction>(index + 1).registerA
                            val insertIndex = index + 2

                            addInstructionsWithLabels(
                                insertIndex, """
                                    if-nez v$insertRegister, :ignore
                                    """ + emptyComponentLabel,
                                ExternalLabel("ignore", getInstruction(insertIndex))
                            )
                        }
                    }

                    // If the EmptyComponents Method and the PathBuilder Method are different,
                    // PathBuilder Method's returnType cannot cast emptyComponents.
                    // So just returns null value.
                    emptyComponentLabel = """
                    const/4 v0, 0x0
                    return-object v0
                    """
                }

                val stringBuilderIndex =
                    getTargetIndexWithFieldReferenceTypeOrThrow("Ljava/lang/StringBuilder;")
                val stringBuilderRegister =
                    getInstruction<TwoRegisterInstruction>(stringBuilderIndex).registerA

                val emptyStringIndex = getStringInstructionIndex("")

                val identifierIndex =
                    getTargetIndexReversedOrThrow(emptyStringIndex, Opcode.IPUT_OBJECT)
                val identifierRegister =
                    getInstruction<TwoRegisterInstruction>(identifierIndex).registerA

                val objectIndex = getTargetIndexOrThrow(emptyStringIndex, Opcode.INVOKE_VIRTUAL)
                val objectRegister = getInstruction<BuilderInstruction35c>(objectIndex).registerC

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