package app.revanced.patches.shared.patch.litho

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.or
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.fingerprints.litho.ByteBufferHookFingerprint
import app.revanced.patches.shared.fingerprints.litho.EmptyComponentBuilderFingerprint
import app.revanced.patches.shared.fingerprints.litho.IdentifierFingerprint
import app.revanced.patches.shared.fingerprints.litho.PbToFbFingerprint
import app.revanced.patches.shared.fingerprints.litho.PbToFbLegacyFingerprint
import app.revanced.util.bytecode.getStringIndex
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import kotlin.properties.Delegates

class ComponentParserPatch : BytecodePatch(
    listOf(
        ByteBufferHookFingerprint,
        EmptyComponentBuilderFingerprint,
        IdentifierFingerprint,
        PbToFbFingerprint,
        PbToFbLegacyFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

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
                    throw PatchResultError("could not find Empty Component Label in method")
            }
        } ?: return EmptyComponentBuilderFingerprint.toErrorResult()

        val pbToFbResult = PbToFbFingerprint.result
            ?: PbToFbLegacyFingerprint.result
            ?: throw PbToFbLegacyFingerprint.toErrorResult()

        pbToFbResult.let {
            it.mutableMethod.apply {
                val byteBufferClassIndex = it.scanResult.patternScanResult!!.startIndex

                byteBufferClassLabel =
                    getInstruction<ReferenceInstruction>(byteBufferClassIndex).reference.toString()
            }
        }

        ByteBufferHookFingerprint.result?.let {
            (context
                .toMethodWalker(it.method)
                .nextMethod(it.scanResult.patternScanResult!!.endIndex, true)
                .getMethod() as MutableMethod
                    ).apply {
                    val methodName =
                        EmptyComponentBuilderFingerprint.result!!.mutableMethod.definingClass

                    addInstruction(
                        0,
                        "sput-object p2, $methodName->buffer:Ljava/nio/ByteBuffer;"
                    )
                }
        } ?: return ByteBufferHookFingerprint.toErrorResult()

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

                it.mutableClass.staticFields.add(
                    ImmutableField(
                        definingClass,
                        "buffer",
                        "Ljava/nio/ByteBuffer;",
                        AccessFlags.PUBLIC or AccessFlags.STATIC,
                        null,
                        annotations,
                        null
                    ).toMutable()
                )
            }
        } ?: return IdentifierFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    internal companion object {
        lateinit var byteBufferClassLabel: String
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
                        sget-object v$freeRegister, $definingClass->buffer:Ljava/nio/ByteBuffer;
                        invoke-static {v$stringBuilderRegister, v$identifierRegister, v$objectRegister, v$freeRegister}, $descriptor(Ljava/lang/StringBuilder;Ljava/lang/String;Ljava/lang/Object;Ljava/nio/ByteBuffer;)Z
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

        // only for YouTube v18.20.39
        fun legacyHook(
            descriptor: String
        ) {
            insertMethod.apply {
                addInstructionsWithLabels(
                    insertIndex,
                        """
                        move-object/from16 v$freeRegister, p3
                        iget-object v$freeRegister, v$freeRegister, ${parameters[2]}->b:Ljava/lang/Object;
                        if-eqz v$freeRegister, :unfiltered
                        check-cast v$freeRegister, $byteBufferClassLabel
                        iget-object v$freeRegister, v$freeRegister, $byteBufferClassLabel->b:Ljava/nio/ByteBuffer;
                        invoke-static {v$stringBuilderRegister, v$identifierRegister, v$objectRegister, v$freeRegister}, $descriptor(Ljava/lang/StringBuilder;Ljava/lang/String;Ljava/lang/Object;Ljava/nio/ByteBuffer;)Z
                        move-result v$freeRegister
                        if-eqz v$freeRegister, :unfiltered
                        """ + emptyComponentLabel,
                    ExternalLabel("unfiltered", getInstruction(insertIndex))
                )
            }
        }
    }
}