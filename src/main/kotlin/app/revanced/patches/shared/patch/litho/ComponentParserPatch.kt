package app.revanced.patches.shared.patch.litho

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.or
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.RVXCompatibility
import app.revanced.patches.shared.fingerprints.litho.ByteBufferHookFingerprint
import app.revanced.patches.shared.fingerprints.litho.EmptyComponentBuilderFingerprint
import app.revanced.patches.shared.fingerprints.litho.IdentifierFingerprint
import app.revanced.util.bytecode.getStringIndex
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.immutable.ImmutableField
import kotlin.properties.Delegates

@Name("component-parser-patch")
@RVXCompatibility
@Version("0.0.1")
class ComponentParserPatch : BytecodePatch(
    listOf(
        ByteBufferHookFingerprint,
        EmptyComponentBuilderFingerprint,
        IdentifierFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        EmptyComponentBuilderFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getStringIndex("Failed to convert Element to Flatbuffers: %s") + 2
                val builderMethodDescriptor =
                    getInstruction<ReferenceInstruction>(targetIndex).reference
                val emptyComponentFieldDescriptor =
                    getInstruction<ReferenceInstruction>(targetIndex + 2).reference

                emptyComponentLabel = """
                    move-object/from16 v0, p1
                    invoke-static {v0}, $builderMethodDescriptor
                    move-result-object v0
                    iget-object v0, v0, $emptyComponentFieldDescriptor
                    return-object v0
                """

                it.mutableClass.staticFields.add(
                    ImmutableField(
                        definingClass,
                        "buffer",
                        "Ljava/nio/ByteBuffer;",
                        AccessFlags.PUBLIC or AccessFlags.STATIC,
                        null,
                        null,
                        null
                    ).toMutable()
                )
            }
        } ?: return EmptyComponentBuilderFingerprint.toErrorResult()

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
                freeRegister = getInstruction<OneRegisterInstruction>(freeIndex).registerA

                insertIndex = stringBuilderIndex + 1
            }
        } ?: return IdentifierFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    internal companion object {
        lateinit var emptyComponentLabel: String
        lateinit var insertMethod: MutableMethod

        var insertIndex by Delegates.notNull<Int>()

        var freeRegister by Delegates.notNull<Int>()
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

        fun objectHook(
            descriptor: String
        ) {
            insertMethod.apply {
                addInstructionsWithLabels(
                    0, """
                        move-object/from16 v0, p2
                        sget-object v1, $definingClass->buffer:Ljava/nio/ByteBuffer;
                        invoke-static {v0, v1}, $descriptor(Ljava/lang/Object;Ljava/nio/ByteBuffer;)Z
                        move-result v0
                        if-eqz v0, :unfiltered
                        """ + emptyComponentLabel, ExternalLabel("unfiltered", getInstruction(0))
                )
            }
        }
    }
}