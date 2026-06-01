package app.morphe.patches.youtube.utils.componentlist

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.conversionContextFingerprintToString2
import app.morphe.patches.shared.litho.componentContextSubParserFingerprint2
import app.morphe.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.util.*
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/LazilyConvertedElementPatch;"

private lateinit var lazilyConvertedElementMethod: MutableMethod
private lateinit var componentElementMethod: MutableMethod

val lazilyConvertedElementHookPatch = bytecodePatch(
    description = "lazilyConvertedElementHookPatch"
) {
    dependsOn(
        sharedExtensionPatch,
        versionCheckPatch,
    )

    execute {
        val componentContextClass = componentContextSubParserFingerprint2.matchOrThrow().classDef

        ComponentListFingerprint.match(componentContextClass).method.apply {
            val conversionContextMethod = conversionContextFingerprintToString2.methodOrThrow()
            val identifierReference = with(conversionContextMethod) {
                val identifierStringIndex =
                    indexOfFirstStringInstructionOrThrow(", identifierProperty=")
                val identifierStringAppendIndex =
                    indexOfFirstInstructionOrThrow(identifierStringIndex, Opcode.INVOKE_VIRTUAL)
                val identifierAppendIndex =
                    indexOfFirstInstructionOrThrow(
                        identifierStringAppendIndex + 1,
                        Opcode.INVOKE_VIRTUAL
                    )
                val identifierRegister =
                    getInstruction<FiveRegisterInstruction>(identifierAppendIndex).registerD
                val identifierIndex =
                    indexOfFirstInstructionReversedOrThrow(identifierAppendIndex) {
                        opcode == Opcode.IGET_OBJECT &&
                                getReference<FieldReference>()?.type == "Ljava/lang/String;" &&
                                (this as? TwoRegisterInstruction)?.registerA == identifierRegister
                }
                getInstruction<ReferenceInstruction>(identifierIndex).reference
            }

            val listIndex = implementation!!.instructions.lastIndex
            val listRegister = getInstruction<OneRegisterInstruction>(listIndex).registerA

            mutableClassDefBy { it.type == EXTENSION_CLASS_DESCRIPTOR }.methods.let { methods ->
                lazilyConvertedElementMethod = methods.single { it.name == "hookElementList" }
                componentElementMethod = methods.single { it.name == "hookComponentList" }
            }

            val pathBuilderReference = classDefBy(conversionContextMethod.definingClass)
                .fields
                .single { field -> field.type == "Ljava/lang/StringBuilder;" }
            val registerProvider = getFreeRegisterProvider(listIndex, 2, listRegister)
            val identifierRegister = registerProvider.getFreeRegister()
            val pathBuilderRegister = registerProvider.getFreeRegister()

            addInstructionsAtControlFlowLabel(
                listIndex, """
                    move-object/from16 v$identifierRegister, p2
                    iget-object v$identifierRegister, v$identifierRegister, $identifierReference
                    move-object/from16 v$pathBuilderRegister, p2
                    iget-object v$pathBuilderRegister, v$pathBuilderRegister, $pathBuilderReference
                    invoke-static {v$listRegister, v$identifierRegister, v$pathBuilderRegister}, $EXTENSION_CLASS_DESCRIPTOR->hookElements(Ljava/util/List;Ljava/lang/String;Ljava/lang/StringBuilder;)V
                    """
            )
        }
    }
}

internal fun hookComponentList(descriptor: String) =
    componentElementMethod.addInstruction(
        0,
        "invoke-static {p0, p1}, $descriptor(Ljava/lang/String;Ljava/util/List;)V"
    )

internal fun hookElementList(descriptor: String) =
    lazilyConvertedElementMethod.addInstruction(
        0,
        "invoke-static {p0, p1}, $descriptor(Ljava/util/List;Ljava/lang/String;)V"
    )
