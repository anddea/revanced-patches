package app.revanced.patches.youtube.shorts.components

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.shorts.components.fingerprints.ReelEnumConstructorFingerprint
import app.revanced.patches.youtube.shorts.components.fingerprints.ReelEnumStaticFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.SHORTS_CLASS_DESCRIPTOR
import app.revanced.util.findMutableMethodOf
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstStringInstructionOrThrow
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

object ShortsRepeatPatch : BytecodePatch(
    setOf(ReelEnumConstructorFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        ReelEnumConstructorFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                ReelEnumStaticFingerprint.resolve(context, it.mutableClass)

                arrayOf(
                    "REEL_LOOP_BEHAVIOR_END_SCREEN" to "endScreen",
                    "REEL_LOOP_BEHAVIOR_REPEAT" to "repeat",
                    "REEL_LOOP_BEHAVIOR_SINGLE_PLAY" to "singlePlay"
                ).map { (enumName, fieldName) ->
                    injectEnum(enumName, fieldName)
                }

                val endScreenStringIndex =
                    indexOfFirstStringInstructionOrThrow("REEL_LOOP_BEHAVIOR_END_SCREEN")
                val endScreenReferenceIndex =
                    indexOfFirstInstructionOrThrow(endScreenStringIndex, Opcode.SPUT_OBJECT)
                val endScreenReference =
                    getInstruction<ReferenceInstruction>(endScreenReferenceIndex).reference.toString()

                val enumMethod = ReelEnumStaticFingerprint.resultOrThrow().mutableMethod

                context.injectHook(endScreenReference, enumMethod)
            }
        }
    }

    private fun MutableMethod.injectEnum(
        enumName: String,
        fieldName: String
    ) {
        val stringIndex = indexOfFirstStringInstructionOrThrow(enumName)
        val insertIndex = indexOfFirstInstructionOrThrow(stringIndex, Opcode.SPUT_OBJECT)
        val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

        addInstruction(
            insertIndex + 1,
            "sput-object v$insertRegister, $SHORTS_CLASS_DESCRIPTOR->$fieldName:Ljava/lang/Enum;"
        )
    }

    private fun BytecodeContext.injectHook(
        endScreenReference: String,
        enumMethod: MutableMethod
    ) {
        classes.forEach { classDef ->
            classDef.methods.filter { method ->
                method.parameters.size == 1
                        && method.parameters[0].startsWith("L")
                        && method.returnType == "V"
                        && method.indexOfFirstInstruction {
                    getReference<FieldReference>()?.toString() == endScreenReference
                } >= 0
            }.forEach { targetMethod ->
                proxy(classDef)
                    .mutableClass
                    .findMutableMethodOf(targetMethod)
                    .apply {
                        implementation!!.instructions
                            .withIndex()
                            .filter { (_, instruction) ->
                                val reference = (instruction as? ReferenceInstruction)?.reference
                                reference is MethodReference &&
                                        MethodUtil.methodSignaturesMatch(enumMethod, reference)
                            }
                            .map { (index, _) -> index }
                            .reversed()
                            .forEach { index ->
                                val register =
                                    getInstruction<OneRegisterInstruction>(index + 1).registerA

                                addInstructions(
                                    index + 2, """
                                        invoke-static {v$register}, $SHORTS_CLASS_DESCRIPTOR->changeShortsRepeatState(Ljava/lang/Enum;)Ljava/lang/Enum;
                                        move-result-object v$register
                                        """
                                )
                            }
                    }
            }
        }
    }
}
