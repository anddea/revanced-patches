package app.revanced.patches.music.utils.fix.accessibility

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patches.music.utils.fix.accessibility.fingerprints.TouchExplorationHoverEventFingerprint
import app.revanced.util.containsMethodReferenceNameInstructionIndex
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

object AccessibilityNodeInfoPatch : BytecodePatch(
    setOf(TouchExplorationHoverEventFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        /**
         * The "getTouchDelegateInfo" method has been implemented in YT Music v6.44.52.
         * For some reason this method sometimes returns null, which throws [IllegalArgumentException].
         * This is considered unimplemented code, so remove all methods associated with it.
         */
        TouchExplorationHoverEventFingerprint.result?.let {
            it.mutableMethod.apply {
                // Target instruction is invoke-static, but can also be invoke-virtual.
                // Therefore, the opcode is not checked.
                val touchExplorationHoverEventMethodIndex = implementation!!.instructions.indexOfFirst { instruction ->
                    val reference = ((instruction as? ReferenceInstruction)?.reference as? MethodReference)
                    ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.definingClass == definingClass
                            && reference?.returnType == "Z"
                }

                // Doesn't raise an exception, even if the target instruction is not found in this method
                val touchExplorationHoverEventMethodName = if (touchExplorationHoverEventMethodIndex > -1)
                    (getInstruction<ReferenceInstruction>(touchExplorationHoverEventMethodIndex).reference as MethodReference).name
                else
                    "UNDEFINED"

                val methods = it.mutableClass.methods

                methods.find { method ->
                    method.name == "getTouchDelegateInfo"
                }?.apply {
                    if (!containsMethodReferenceNameInstructionIndex("isEmpty")) {
                        arrayOf(
                            "getTouchDelegateInfo",
                            name,
                            touchExplorationHoverEventMethodName
                        ).forEach { methodName ->
                            methods.removeIf { method ->
                                method.name == methodName
                            }
                        }
                    }
                }
            }
        } // If this method has not been added, there is no need to remove it, so it will not raise any exceptions.
    }
}
