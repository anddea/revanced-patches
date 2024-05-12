package app.revanced.patches.shared.clientspoof

import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.transformation.BaseTransformInstructionsPatch
import app.revanced.patches.shared.transformation.IMethodCall
import app.revanced.patches.shared.transformation.Instruction35cInfo
import app.revanced.patches.shared.transformation.filterMapInstruction35c
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

abstract class BaseClientSpoofPatch(
    private val packageName: String
) : BaseTransformInstructionsPatch<Instruction35cInfo>() {
    override fun filterMap(
        classDef: ClassDef,
        method: Method,
        instruction: Instruction,
        instructionIndex: Int,
    ) = filterMapInstruction35c<MethodCall>(
        "Lapp/revanced/integrations",
        classDef,
        instruction,
        instructionIndex,
    )

    override fun transform(mutableMethod: MutableMethod, entry: Instruction35cInfo) {
        val (_, _, instructionIndex) = entry

        // Replace the result of context.getPackageName(), if it is used in a user agent string.
        mutableMethod.apply {
            var isTargetMethod = true

            for ((index, instruction) in implementation!!.instructions.withIndex()) {
                if (instruction.opcode != Opcode.CONST_STRING)
                    continue

                val constString = getInstruction<BuilderInstruction21c>(index).reference.toString()

                if (constString != "android.resource://" && constString != "gcore_")
                    continue

                isTargetMethod = false
                break
            }

            if (isTargetMethod) {
                // After context.getPackageName() the result is moved to a register.
                val targetRegister = (
                        getInstruction(instructionIndex + 1)
                                as? OneRegisterInstruction ?: return
                        ).registerA

                // IndexOutOfBoundsException is not possible here,
                // but no such occurrences are present in the app.
                val referee = getInstruction(instructionIndex + 2).getReference<MethodReference>()?.toString()

                // This can technically also match non-user agent string builder append methods,
                // but no such occurrences are present in the app.
                if (referee != "Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;") {
                    return
                }

                // Overwrite the result of context.getPackageName() with the original package name.
                replaceInstruction(
                    instructionIndex + 1,
                    "const-string v$targetRegister, \"$packageName\"",
                )
            }
        }
    }

    @Suppress("unused")
    private enum class MethodCall(
        override val definedClassName: String,
        override val methodName: String,
        override val methodParams: Array<String>,
        override val returnType: String,
    ) : IMethodCall {
        GetPackageName(
            "Landroid/content/Context;",
            "getPackageName",
            emptyArray(),
            "Ljava/lang/String;",
        ),
    }
}