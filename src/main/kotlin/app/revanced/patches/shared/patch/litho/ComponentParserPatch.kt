package app.revanced.patches.shared.patch.litho

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.fingerprints.litho.EmptyComponentBuilderFingerprint
import app.revanced.patches.shared.fingerprints.litho.IdentifierFingerprint
import app.revanced.util.bytecode.getStringIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import kotlin.properties.Delegates

class ComponentParserPatch : BytecodePatch(
    listOf(
        EmptyComponentBuilderFingerprint,
        IdentifierFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        EmptyComponentBuilderFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetStringIndex = getStringIndex("Error while converting %s")

                for (index in targetStringIndex until implementation!!.instructions.size - 1) {
                    if (getInstruction(index).opcode != Opcode.INVOKE_STATIC_RANGE) continue

                    val builderMethodDescriptor =
                        getInstruction<ReferenceInstruction>(index).reference
                    val emptyComponentFieldDescriptor =
                        getInstruction<ReferenceInstruction>(index + 2).reference

                    emptyComponentLabel = """
                        move-object/from16 v0, p1
                        invoke-static {v0}, $builderMethodDescriptor
                        move-result-object v0
                        iget-object v0, v0, $emptyComponentFieldDescriptor
                        return-object v0
                        """
                    break
                }

                if (emptyComponentLabel.isEmpty())
                    throw PatchException("could not find Empty Component Label in method")
            }
        } ?: throw EmptyComponentBuilderFingerprint.exception

        IdentifierFingerprint.result?.let {
            it.mutableMethod.apply {
                insertMethod = this

                val stringBuilderIndex =
                    implementation!!.instructions.indexOfFirst { instruction ->
                        val fieldReference =
                            (instruction as? ReferenceInstruction)?.reference as? FieldReference
                        fieldReference?.let { reference -> reference.type == "Ljava/lang/StringBuilder;" } == true
                    }

                val identifierIndex = it.scanResult.patternScanResult!!.endIndex
                val objectIndex = getStringIndex("") + 1
                val freeIndex = implementation!!.instructions.indexOfFirst { instruction ->
                    instruction.opcode == Opcode.CONST
                }

                stringBuilderRegister =
                    getInstruction<TwoRegisterInstruction>(stringBuilderIndex).registerA
                identifierRegister =
                    getInstruction<OneRegisterInstruction>(identifierIndex).registerA
                objectRegister = getInstruction<BuilderInstruction35c>(objectIndex).registerC

                val register = getInstruction<OneRegisterInstruction>(freeIndex).registerA

                freeRegister =
                    if (register == stringBuilderRegister || register == identifierRegister || register == objectRegister)
                        15
                    else
                        register

                insertIndex = stringBuilderIndex + 1
            }
        } ?: throw IdentifierFingerprint.exception

    }

    internal companion object {
        lateinit var emptyComponentLabel: String
        lateinit var insertMethod: MutableMethod

        var insertIndex by Delegates.notNull<Int>()

        var freeRegister = 15

        var identifierRegister by Delegates.notNull<Int>()
        var objectRegister by Delegates.notNull<Int>()
        var stringBuilderRegister by Delegates.notNull<Int>()

        fun generalHook(
            descriptor: String
        ) {
            insertMethod.apply {
                addInstructionsWithLabels(
                    insertIndex,
                    """
                        invoke-static {v$stringBuilderRegister, v$identifierRegister, v$objectRegister}, $descriptor(Ljava/lang/StringBuilder;Ljava/lang/String;Ljava/lang/Object;)Z
                        move-result v$freeRegister
                        if-eqz v$freeRegister, :unfiltered
                        """ + emptyComponentLabel,
                    ExternalLabel("unfiltered", getInstruction(insertIndex))
                )
            }
        }

        fun identifierHook(
            descriptor: String
        ) {
            insertMethod.apply {
                addInstructionsWithLabels(
                    insertIndex,
                    """
                        invoke-static {v$stringBuilderRegister, v$identifierRegister}, $descriptor(Ljava/lang/StringBuilder;Ljava/lang/String;)Z
                        move-result v$freeRegister
                        if-eqz v$freeRegister, :unfiltered
                        """ + emptyComponentLabel,
                    ExternalLabel("unfiltered", getInstruction(insertIndex))
                )
            }
        }
    }
}